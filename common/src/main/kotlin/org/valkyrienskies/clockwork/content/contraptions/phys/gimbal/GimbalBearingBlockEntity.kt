package org.valkyrienskies.clockwork.content.contraptions.phys.gimbal

import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity
import com.simibubi.create.content.contraptions.DirectionalExtenderScrollOptionSlot
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingAssembler
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.ClockworkUtils.getVector3d
import org.valkyrienskies.clockwork.util.GlueAssembler.collectGlued
import org.valkyrienskies.clockwork.util.findMatchingJoint
import org.valkyrienskies.clockwork.util.findMatchingJointIds
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.hasFinitePoseData
import org.valkyrienskies.clockwork.util.removeMatchingJointsExcept
import org.valkyrienskies.clockwork.util.updateJoint
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.impl.bodies.properties.BodyTransformFactory
import org.valkyrienskies.core.internal.joints.VSD6Joint
import org.valkyrienskies.core.internal.joints.VSJoint
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.core.internal.joints.VSSphericalJoint
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.common.*
import org.valkyrienskies.mod.common.assembly.ShipAssembler.assembleToShip
import org.valkyrienskies.mod.common.assembly.VSAssemblyEvents
import org.valkyrienskies.mod.common.util.SplittingDisablerAttachment
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.world.clipIncludeShips
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class GimbalBearingBlockEntity(type: net.minecraft.world.level.block.entity.BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) :
    GeneratingKineticBlockEntity(type, pos, state),
    IBearingBlockEntity,
    IDisplayAssemblyExceptions,
    BlockEntityPhysicsListener {

    @Volatile override lateinit var dimension: DimensionId

    var assembleNextTick = false
    var isRunning = false
        private set

    @Volatile var shiptraptionID: Long = NO_SHIPTRAPTION_ID
        private set

    @Volatile var joint: VSJoint? = null
        private set
    @Volatile var jointID: Int = -1
        private set

    var modeBehaviour: ScrollOptionBehaviour<GimbalMode>? = null
    var maxAngleBehaviour: MaxAngleScrollValueBehaviour? = null

    private var lastException: AssemblyException? = null

    private var originalDirection: Direction? = null

    @Volatile private var bearingAxisLocal: Vector3d = Vector3d(0.0, 1.0, 0.0)
    @Volatile private var bearingPosInSub: Vector3d = Vector3d()
    @Volatile private var assemblyHostRotation: Quaterniond = Quaterniond()
    @Volatile private var lastJointMaxAngleDeg: Int = DEFAULT_MAX_ANGLE_DEG

    @Volatile private var redstoneVecLocal: Vector3d = Vector3d()

    private var ticks = 0
    private var lastStateChanged = 0
    private val cooldown = 20

    private var pendingJointCreationToken: Long = 0L

    private val facing: Direction
        get() = blockState.getValue(BlockStateProperties.FACING)

    fun getMaxAngleDeg(): Int = (maxAngleBehaviour?.value ?: DEFAULT_MAX_ANGLE_DEG).coerceIn(0, MAX_ANGLE_LIMIT_DEG)

    fun getMode(): GimbalMode = modeBehaviour?.get() ?: GimbalMode.LOCKED

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)

        modeBehaviour = ScrollOptionBehaviour(
            GimbalMode::class.java,
            Component.translatable("$MOD_ID.gimbal_bearing.mode"),
            this,
            ModeSlot()
        )
        modeBehaviour!!.requiresWrench()
        behaviours.add(modeBehaviour!!)

        maxAngleBehaviour = MaxAngleScrollValueBehaviour(
            Component.translatable("$MOD_ID.gimbal_bearing.max_angle"),
            this,
            MaxAngleSlot()
        )
        maxAngleBehaviour!!.between(0, MAX_ANGLE_LIMIT_DEG)
        maxAngleBehaviour!!.withCallback { onMaxAngleChanged() }
        maxAngleBehaviour!!.value = DEFAULT_MAX_ANGLE_DEG
        behaviours.add(maxAngleBehaviour!!)
    }

    private fun onMaxAngleChanged() {
        if (level == null || level!!.isClientSide) return
        rebuildJointSwingLimitIfNeeded()
        sendData()
    }

    fun onNeighborChanged() {
        updateRedstoneVector()
    }

    private fun updateRedstoneVector() {
        val level = level ?: return
        val pos = blockPos
        val facing = this.facing
        val v = Vector3d()
        for (dir in Direction.entries) {
            if (dir.axis == facing.axis) continue
            val signal = level.getSignal(pos.relative(dir), dir)
            if (signal <= 0) continue
            val n = dir.normal
            v.add(n.x.toDouble() * signal, n.y.toDouble() * signal, n.z.toDouble() * signal)
        }
        v.div(15.0)
        val len = v.length()
        if (len > 1.0) v.div(len)
        redstoneVecLocal = v
    }

    private fun rebuildJointSwingLimitIfNeeded() {
        if (!isRunning) return
        val current = joint as? VSSphericalJoint ?: return
        val newMax = getMaxAngleDeg()
        if (newMax == lastJointMaxAngleDeg) return
        val serverLevel = level as? ServerLevel ?: return
        val limitRad = Math.toRadians(newMax.toDouble()).toFloat().coerceAtLeast(MIN_LIMIT_RAD)
        val newJoint = current.copy(limitCone = VSD6Joint.LimitCone(limitRad, limitRad))
        if (jointID != -1) {
            serverLevel.gtpa.updateJoint(jointID, newJoint)
        }
        joint = newJoint
        lastJointMaxAngleDeg = newMax
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        tag.putBoolean(ClockworkConstants.Nbt.RUNNING, isRunning)
        if (shiptraptionID != NO_SHIPTRAPTION_ID) {
            tag.putLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID, shiptraptionID)
        }
        originalDirection?.let { tag.putInt(ClockworkConstants.Nbt.ORIGINAL_DIRECTION, it.ordinal) }
        AssemblyException.write(tag, lastException)
        tag.putInt("MaxAngleDeg", getMaxAngleDeg())
        tag.putInt("Mode", getMode().ordinal)

        tag.putDouble("BearingAxisLX", bearingAxisLocal.x)
        tag.putDouble("BearingAxisLY", bearingAxisLocal.y)
        tag.putDouble("BearingAxisLZ", bearingAxisLocal.z)
        tag.putDouble("BearingPosSX", bearingPosInSub.x)
        tag.putDouble("BearingPosSY", bearingPosInSub.y)
        tag.putDouble("BearingPosSZ", bearingPosInSub.z)
        tag.putDouble("AssyHostRotX", assemblyHostRotation.x)
        tag.putDouble("AssyHostRotY", assemblyHostRotation.y)
        tag.putDouble("AssyHostRotZ", assemblyHostRotation.z)
        tag.putDouble("AssyHostRotW", assemblyHostRotation.w)

        if (jointID != -1) tag.putInt("JointID", jointID)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        isRunning = tag.getBoolean(ClockworkConstants.Nbt.RUNNING)
        if (tag.contains(ClockworkConstants.Nbt.SHIPTRAPTION_ID)) {
            shiptraptionID = tag.getLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID)
        }
        if (tag.contains(ClockworkConstants.Nbt.ORIGINAL_DIRECTION)) {
            originalDirection = Direction.entries[tag.getInt(ClockworkConstants.Nbt.ORIGINAL_DIRECTION)]
        }
        lastException = AssemblyException.read(tag)
        if (tag.contains("MaxAngleDeg")) {
            maxAngleBehaviour?.value = tag.getInt("MaxAngleDeg").coerceIn(0, MAX_ANGLE_LIMIT_DEG)
        }
        if (tag.contains("Mode")) {
            modeBehaviour?.setValue(tag.getInt("Mode").coerceIn(0, GimbalMode.entries.size - 1))
        }

        if (tag.contains("BearingAxisLX")) {
            bearingAxisLocal = Vector3d(tag.getDouble("BearingAxisLX"), tag.getDouble("BearingAxisLY"), tag.getDouble("BearingAxisLZ"))
        }
        if (tag.contains("BearingPosSX")) {
            bearingPosInSub = Vector3d(tag.getDouble("BearingPosSX"), tag.getDouble("BearingPosSY"), tag.getDouble("BearingPosSZ"))
        }
        if (tag.contains("AssyHostRotX")) {
            assemblyHostRotation = Quaterniond(
                tag.getDouble("AssyHostRotX"),
                tag.getDouble("AssyHostRotY"),
                tag.getDouble("AssyHostRotZ"),
                tag.getDouble("AssyHostRotW")
            )
        }
        if (tag.contains("JointID")) jointID = tag.getInt("JointID")
        lastJointMaxAngleDeg = getMaxAngleDeg()
    }

    override fun tick() {
        super.tick()
        ticks++
        if (level == null || level!!.isClientSide) return

        tryAssembleNextTick()
    }

    private fun tryAssembleNextTick() {
        if (!assembleNextTick) return
        if (ticks - lastStateChanged <= cooldown) return
        assembleNextTick = false
        if (!isRunning) assemble()
    }

    private fun assemble() {
        if (level!!.getBlockState(worldPosition).block !is BearingBlock) return
        val level = level as ServerLevel
        originalDirection = blockState.getValue(BearingBlock.FACING)
        val direction = originalDirection!!
        val attachPoint = worldPosition.relative(direction)

        val worldPos: Vector3dc = worldPosition.center.toJOML()
        val axis = direction.normal.toJOMLD()
        val shipOn = level.getShipObjectManagingPos(worldPosition)

        val startPos = Vector3d(worldPos).fma(0.5, axis)
        val endPos = Vector3d(worldPos).fma(1.5, axis)

        fun Vector3d.toVec3() = net.minecraft.world.phys.Vec3(this.x, this.y, this.z)

        val otherPos = level.clipIncludeShips(
            ClipContext(
                (shipOn?.transform?.shipToWorld?.transformPosition(startPos) ?: startPos).toVec3(),
                (shipOn?.transform?.shipToWorld?.transformPosition(endPos) ?: endPos).toVec3(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
            ), false, shipOn?.id
        )

        val otherShip = level.getShipObjectManagingPos(otherPos.blockPos)
        val posInOwnerShip = Vector3d(worldPos)

        val (bearingPos, shiptraption) = if (otherShip == null) {
            val selection: DenseBlockPosSet?
            try {
                selection = collectGlued(level, attachPoint)
                selection?.remove(this.blockPos.x, this.blockPos.y, this.blockPos.z)
                lastException = null
            } catch (e: AssemblyException) {
                lastException = e
                sendData()
                return
            }
            if (selection == null) return

            var centerPositions: Pair<Vector3d, Vector3d> = Pair(Vector3d(), Vector3d())
            val event = VSAssemblyEvents.onPasteBeforeBlocksAreLoaded.on {
                centerPositions = it.centerPosition.first.get(Vector3d()) to it.centerPosition.second.get(Vector3d())
            }
            val ship = assembleToShip(
                level,
                selection.toSet().map { it.toMinecraft() }.toSet(),
                1.0
            )
            event.unregister()

            val newPos = Vector3d(worldPos).sub(centerPositions.first).add(centerPositions.second)
            shiptraptionID = ship.id
            Pair(newPos, ship)
        } else {
            shiptraptionID = otherShip.id
            val bp = Vector3d(otherPos.blockPos.x.toDouble() + 0.5, otherPos.blockPos.y.toDouble() + 0.5, otherPos.blockPos.z.toDouble() + 0.5).sub(direction.normal.toJOMLD())
            Pair(bp, otherShip)
        }

        val shipOnID = shipOn?.id

        val posInWorld = shipOn?.transform?.shipToWorld?.transformPosition(
            Vector3d(posInOwnerShip).sub(bearingPos).add(shiptraption.inertiaData.centerOfMass), Vector3d()
        ) ?: Vector3d(worldPos).sub(bearingPos).add(shiptraption.inertiaData.centerOfMass)
        val rotInWorld = shipOn?.transform?.shipToWorldRotation?.let { Quaterniond(it) } ?: Quaterniond()
        val scaling = shipOn?.transform?.shipToWorldScaling ?: Vector3d(1.0, 1.0, 1.0)

        shiptraption.unsafeSetTransform(
            BodyTransformFactory.create(
                posInWorld, rotInWorld, scaling, shiptraption.transform.positionInModel
            )
        )

        val ship1rot = directionHingeRotation(direction)
        val ship2rot = directionHingeRotation(direction)
        val extraDist = 1.0
        val pose0 = VSJointPose(Vector3d(bearingPos).fma(-extraDist, axis), ship1rot)
        val pose1 = VSJointPose(Vector3d(posInOwnerShip).fma(-extraDist, axis), ship2rot)

        val maxAngle = getMaxAngleDeg()
        val limitRad = Math.toRadians(maxAngle.toDouble()).toFloat().coerceAtLeast(MIN_LIMIT_RAD)

        joint = VSSphericalJoint(
            shipId0 = shiptraptionID,
            pose0 = pose0,
            shipId1 = shipOnID,
            pose1 = pose1,
            compliance = 1e-100,
            limitCone = VSD6Joint.LimitCone(limitRad, limitRad)
        )
        bearingAxisLocal = axis
        bearingPosInSub = bearingPos
        assemblyHostRotation = rotInWorld
        lastJointMaxAngleDeg = maxAngle

        tryMakeJoint()
        sendData()
        updateGeneratedRotation()
    }

    private fun tryMakeJoint() {
        val joint = joint ?: return
        val creationToken = ++pendingJointCreationToken
        if (!joint.hasFinitePoseData()) {
            ClockworkMod.LOGGER.warn("Rejecting corrupted gimbal bearing joint at {}.", worldPosition)
            this.joint = null
            return
        }

        ClockworkMod.physTickOnce(level.dimensionId!!) { physLevel, _, tryNextTick ->
            physLevel as VsiPhysLevel
            if (creationToken != pendingJointCreationToken) return@physTickOnce

            val match = physLevel.findMatchingJoint(joint)
            if (match != null) {
                this.joint = match.joint
                this.jointID = match.jointId
                physLevel.removeMatchingJointsExcept(match.joint, match.jointId)
                isRunning = true
                lastStateChanged = ticks
                return@physTickOnce
            }

            if (
                joint.shipId0 != null && physLevel.getShipById(joint.shipId0!!) == null ||
                joint.shipId1 != null && physLevel.getShipById(joint.shipId1!!) == null
            ) {
                tryNextTick()
                return@physTickOnce
            }

            val id = physLevel.addJoint(joint)
            if (id == -1) {
                if (creationToken == pendingJointCreationToken) tryNextTick()
                return@physTickOnce
            }
            if (creationToken != pendingJointCreationToken) {
                physLevel.removeJoint(id)
                return@physTickOnce
            }
            this.jointID = id
            physLevel.removeMatchingJointsExcept(joint, id)
            isRunning = true
            lastStateChanged = ticks
        }
    }

    private fun removeTrackedJoint(serverLevel: ServerLevel) {
        pendingJointCreationToken++
        val ids = linkedSetOf<Int>()
        if (jointID != -1) ids.add(jointID)
        joint?.let { ids.addAll(serverLevel.gtpa.findMatchingJointIds(it)) }
        ids.forEach { serverLevel.gtpa.removeJoint(it) }
        joint = null
        jointID = -1
    }

    fun disassemble() {
        if (!isRunning && shiptraptionID == NO_SHIPTRAPTION_ID) return
        if (ticks - lastStateChanged <= cooldown) return
        if (shiptraptionID == NO_SHIPTRAPTION_ID || level!!.isClientSide) {
            resetState()
            return
        }
        val level = level as ServerLevel
        val subShip = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: run {
            removeTrackedJoint(level)
            resetState()
            return
        }
        val mainShip = level.getShipObjectManagingPos(worldPosition)

        val targetRot = mainShip?.transform?.shipToWorldRotation?.let { Quaterniond(it) } ?: Quaterniond()
        val scaling = mainShip?.transform?.shipToWorldScaling ?: Vector3d(1.0, 1.0, 1.0)
        val posInWorld = mainShip?.transform?.shipToWorld?.transformPosition(
            Vector3d(worldPosition.center.toJOML()).sub(bearingPosInSub).add(subShip.inertiaData.centerOfMass), Vector3d()
        ) ?: Vector3d(worldPosition.center.toJOML()).sub(bearingPosInSub).add(subShip.inertiaData.centerOfMass)
        subShip.unsafeSetTransform(
            BodyTransformFactory.create(posInWorld, targetRot, scaling, subShip.transform.positionInModel)
        )

        val direction = originalDirection ?: blockState.getValue(BearingBlock.FACING)
        val inMain = worldPosition.relative(direction, 1)
        val inSubship = Vector3d(bearingPosInSub).add(bearingAxisLocal).let { BlockPos.containing(it.x, it.y, it.z) }

        val aabb = subShip.shipAABB!!
        val blocks = DenseBlockPosSet()
        for (x in aabb.minX() - 1 until aabb.maxX() + 1)
            for (z in aabb.minZ() - 1 until aabb.maxZ() + 1)
                for (y in aabb.minY() - 1 until aabb.maxY() + 1)
                    blocks.add(x, y, z)

        val subCouldSplit = subShip.getAttachment<SplittingDisablerAttachment>()?.let {
            if (it.canSplit()) { it.disableSplitting(); true } else false
        } ?: false
        val mainCouldSplit = mainShip?.getAttachment<SplittingDisablerAttachment>()?.let {
            if (it.canSplit()) { it.disableSplitting(); true } else false
        } ?: false

        val moved = PhysBearingAssembler.moveBlocksFromTo(level, blocks, true, inSubship, inMain, subShip, mainShip)

        if (subCouldSplit) subShip.getAttachment<SplittingDisablerAttachment>()?.enableSplitting()
        if (mainCouldSplit) mainShip?.getAttachment<SplittingDisablerAttachment>()?.enableSplitting()

        if (!moved) return
        removeTrackedJoint(level)
        lastStateChanged = ticks
        resetState()
    }

    private fun resetState() {
        pendingJointCreationToken++
        shiptraptionID = NO_SHIPTRAPTION_ID
        isRunning = false
        assembleNextTick = false
        joint = null
        jointID = -1
        sendData()
        updateGeneratedRotation()
    }

    override fun destroy() {
        super.destroy()
        val level = level ?: return
        if (level.isClientSide || level !is ServerLevel) return
        removeTrackedJoint(level)
    }

    override fun physTick(physShip: PhysShip?, physLevel: PhysLevel) {
        if (isRemoved || !isRunning) return
        val subShip = physLevel.getShipById(shiptraptionID) ?: return
        if (subShip.isStatic) return

        val mass = subShip.mass
        if (mass <= 0.0) return

        val hostRot = physShip?.transform?.shipToWorldRotation?.let { Quaterniond(it) } ?: Quaterniond()

        // Always-on twist correction: VSSphericalJoint allows free roll, so we substitute
        // the hard TWIST=LOCKED constraint with a stiff PID along the bearing axis.
        applyTwistCorrection(subShip, physShip, hostRot, mass)

        val rsLocal = redstoneVecLocal
        val rsLen = min(rsLocal.length(), 1.0)
        val tiltAngleRad = rsLen * Math.toRadians(getMaxAngleDeg().toDouble())
        val mode = getMode()

        val rpmAbs = abs(getSpeed())
        if (rpmAbs < 1e-3) return

        val maxTorque = ClockworkConfig.SERVER.gimbal.gimbalMaxTorquePerRpmPerKg * rpmAbs * mass

        val refRot = if (mode == GimbalMode.GYROSCOPIC) Quaterniond(assemblyHostRotation) else hostRot

        val currentAxisWorld = Vector3d(bearingAxisLocal).rotate(subShip.transform.shipToWorldRotation, Vector3d())

        if (mode == GimbalMode.UNLOCKED) {
            if (rsLen < 1e-6) return
            val rsWorld = Vector3d(rsLocal).rotate(refRot)
            val torqueAxis = Vector3d(currentAxisWorld).cross(rsWorld, Vector3d())
            val tLen = torqueAxis.length()
            if (tLen < 1e-9) return
            torqueAxis.div(tLen)
            val torqueMag = rsLen * ClockworkConfig.SERVER.gimbal.gimbalUnlockedForcePerRpmPerKg * rpmAbs * mass
            val torque = torqueAxis.mul(torqueMag, Vector3d())
            applyTorquePair(subShip, physShip, torque)
            return
        }

        // LOCKED / GYROSCOPIC: PID toward target axis
        val targetAxisLocal = Vector3d(bearingAxisLocal).mul(cos(tiltAngleRad))
        if (rsLen > 1e-6 && tiltAngleRad > 1e-9) {
            val tiltDirLocal = Vector3d(rsLocal).normalize()
            targetAxisLocal.fma(sin(tiltAngleRad), tiltDirLocal)
        }
        val targetAxisWorld = targetAxisLocal.rotate(refRot, Vector3d())

        val errCross = Vector3d(currentAxisWorld).cross(targetAxisWorld, Vector3d())
        val sinErr = errCross.length()
        val cosErr = currentAxisWorld.dot(targetAxisWorld)
        if (sinErr < 1e-9 && cosErr > 0.0) return
        val errAngle = atan2(sinErr, cosErr)
        val errAxisUnit = if (sinErr > 1e-9) Vector3d(errCross).div(sinErr) else Vector3d(0.0, 1.0, 0.0)

        val subOmega = Vector3d(subShip.angularVelocity)
        val hostOmega = if (physShip != null) Vector3d(physShip.angularVelocity) else Vector3d()
        val relOmega = subOmega.sub(hostOmega, Vector3d())
        val omegaAlongErr = relOmega.dot(errAxisUnit)

        val kp = ClockworkConfig.SERVER.gimbal.gimbalAngleErrorMultiplier
        val kd = ClockworkConfig.SERVER.gimbal.gimbalOmegaErrorMultiplier
        var torqueMag = (kp * errAngle - kd * omegaAlongErr) * mass
        if (!java.lang.Double.isFinite(torqueMag)) return
        if (maxTorque > 0.0) torqueMag = torqueMag.coerceIn(-maxTorque, maxTorque)
        val torque = errAxisUnit.mul(torqueMag, Vector3d())
        applyTorquePair(subShip, physShip, torque)
    }

    private fun applyTwistCorrection(subShip: PhysShip, hostShip: PhysShip?, hostRot: Quaterniond, mass: Double) {
        val kp = ClockworkConfig.SERVER.gimbal.gimbalTwistKp
        val kd = ClockworkConfig.SERVER.gimbal.gimbalTwistKd
        if (kp <= 0.0 && kd <= 0.0) return

        // Relative rotation expressed in host's local frame: q_rel = q_host^-1 * q_sub
        val qHostInv = Quaterniond(hostRot).invert()
        val qSub = Quaterniond(subShip.transform.shipToWorldRotation)
        val qRel = qHostInv.mul(qSub, Quaterniond())

        // Twist angle around bearingAxisLocal: 2 * atan2(qImag · axis, qW)
        val a = bearingAxisLocal
        val sinHalf = qRel.x * a.x + qRel.y * a.y + qRel.z * a.z
        val cosHalf = qRel.w
        var twistAngle = 2.0 * atan2(sinHalf, cosHalf)
        while (twistAngle > Math.PI) twistAngle -= 2.0 * Math.PI
        while (twistAngle < -Math.PI) twistAngle += 2.0 * Math.PI

        // Twist-axis angular velocity in world: project (subOmega - hostOmega) onto bearingAxisWorld
        val bearingAxisWorld = Vector3d(a).rotate(hostRot, Vector3d())
        val subOmega = Vector3d(subShip.angularVelocity)
        val hostOmega = if (hostShip != null) Vector3d(hostShip.angularVelocity) else Vector3d()
        val omegaRel = subOmega.sub(hostOmega, Vector3d())
        val omegaTwist = omegaRel.dot(bearingAxisWorld)

        var torqueMag = -(kp * twistAngle + kd * omegaTwist) * mass
        if (!java.lang.Double.isFinite(torqueMag)) return
        val maxTorque = ClockworkConfig.SERVER.gimbal.gimbalTwistMaxTorquePerKg * mass
        if (maxTorque > 0.0) torqueMag = torqueMag.coerceIn(-maxTorque, maxTorque)
        val torque = bearingAxisWorld.mul(torqueMag, Vector3d())
        applyTorquePair(subShip, hostShip, torque)
    }

    private fun applyTorquePair(subShip: PhysShip, hostShip: PhysShip?, torque: Vector3dc) {
        if (!torque.isFiniteVec()) return
        subShip.applyWorldTorque(torque)
        hostShip?.applyWorldTorque(Vector3d(torque).mul(-1.0))
    }

    private fun Vector3dc.isFiniteVec(): Boolean =
        java.lang.Double.isFinite(x()) && java.lang.Double.isFinite(y()) && java.lang.Double.isFinite(z())

    override fun getInterpolatedAngle(partialTicks: Float): Float = 0f
    override fun isWoodenTop(): Boolean = false
    override fun setAngle(forcedAngle: Float) { /* no-op: gimbal does not have a single-axis angle */ }

    override fun attach(contraption: ControlledContraptionEntity) {}
    override fun onStall() { if (!level!!.isClientSide) sendData() }
    override fun isValid(): Boolean = !isRemoved
    override fun isAttachedTo(contraption: AbstractContraptionEntity): Boolean = false
    override fun getLastAssemblyException(): AssemblyException? = lastException
    override fun getBlockPosition(): BlockPos = worldPosition

    /**
     * Mode slot: shown on lateral sides whose axis is NOT the max-angle axis.
     * Restricted (vs. the standard movementModeSlot) so it doesn't share faces with the
     * max-angle widget — overlapping hit boxes would let mode swallow all clicks.
     */
    private class ModeSlot : DirectionalExtenderScrollOptionSlot({ state, dir ->
        val facingAxis = state.getValue(BearingBlock.FACING).axis
        dir.axis != facingAxis && dir.axis != maxAngleAxisFor(facingAxis)
    })

    /** Max-angle slot: shown on the two lateral sides on the [maxAngleAxisFor] axis. */
    private class MaxAngleSlot : DirectionalExtenderScrollOptionSlot({ state, dir ->
        dir.axis == maxAngleAxisFor(state.getValue(BearingBlock.FACING).axis)
    })

    /**
     * Distinct [BehaviourType] so this coexists with the mode's [ScrollValueBehaviour] in the
     * BE's behaviour map, and a unique [netId] so [ValueSettingsPacket] routes value-edit packets
     * to this behaviour instead of the first matching one (mode).
     */
    class MaxAngleScrollValueBehaviour(
        label: Component,
        be: com.simibubi.create.foundation.blockEntity.SmartBlockEntity,
        slot: ValueBoxTransform
    ) : ScrollValueBehaviour(label, be, slot) {
        override fun getType(): BehaviourType<*> = TYPE
        override fun netId(): Int = NET_ID

        override fun write(nbt: net.minecraft.nbt.CompoundTag, clientPacket: Boolean) {
            // ScrollValueBehaviour writes to the hardcoded "ScrollValue" key, which collides
            // with the mode behaviour. Use our own key and skip the parent's NBT write.
            nbt.putInt(NBT_KEY, value)
        }

        override fun read(nbt: net.minecraft.nbt.CompoundTag, clientPacket: Boolean) {
            if (nbt.contains(NBT_KEY)) value = nbt.getInt(NBT_KEY)
        }

        companion object {
            @JvmField val TYPE = BehaviourType<MaxAngleScrollValueBehaviour>()
            private const val NET_ID = 1
            private const val NBT_KEY = "GimbalMaxAngleScrollValue"
        }
    }

    private fun directionHingeRotation(localDirection: Direction): Quaterniond {
        val base: Quaterniond = when (localDirection) {
            Direction.UP -> Quaterniond()
            Direction.DOWN -> Quaterniond(AxisAngle4d(Math.PI, Vector3d(1.0, 0.0, 0.0)))
            Direction.NORTH -> Quaterniond(AxisAngle4d(Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)))
            ).normalize()
            Direction.EAST -> Quaterniond(AxisAngle4d(0.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)))
            ).normalize()
            Direction.SOUTH -> Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0))).normalize()
            Direction.WEST -> Quaterniond(AxisAngle4d(1.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)))
            ).normalize()
        }
        return base.mul(
            Quaterniond(AxisAngle4d(Math.toRadians(90.0), 0.0, 0.0, 1.0)),
            Quaterniond()
        ).normalize()
    }

    companion object {
        const val NO_SHIPTRAPTION_ID: Long = -1
        const val DEFAULT_MAX_ANGLE_DEG = 45
        const val MAX_ANGLE_LIMIT_DEG = 180
        private const val MIN_LIMIT_RAD = 1e-3f

        /**
         * For a given bearing facing axis, returns which of the two lateral axes the max-angle
         * widget uses. The mode widget gets the other lateral axis.
         */
        fun maxAngleAxisFor(facingAxis: Direction.Axis): Direction.Axis = when (facingAxis) {
            Direction.Axis.X -> Direction.Axis.Z
            Direction.Axis.Y -> Direction.Axis.Z
            Direction.Axis.Z -> Direction.Axis.Y
        }
    }
}
