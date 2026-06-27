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
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
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
import org.valkyrienskies.core.internal.joints.VSD6Joint.D6Axis
import org.valkyrienskies.core.internal.joints.VSD6Joint.D6Motion
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
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import java.util.EnumMap
import kotlin.math.abs
import kotlin.math.min

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

    @Volatile var debugHasForceData: Boolean = false
        private set
    @Volatile var debugModeOrdinal: Int = GimbalMode.LOCKED.ordinal
        private set
    @Volatile var debugTargetAngleDeg: Double = 0.0
        private set
    @Volatile var debugMass: Double = 0.0
        private set
    @Volatile var debugRpmAbs: Double = 0.0
        private set
    @Volatile var debugMaxForce: Double = 0.0
        private set
    @Volatile var debugSubAnchorWorld: Vector3d = Vector3d()
        private set
    @Volatile var debugHostAnchorWorld: Vector3d = Vector3d()
        private set
    @Volatile var debugCurrentPointWorld: Vector3d = Vector3d()
        private set
    @Volatile var debugTargetPointWorld: Vector3d = Vector3d()
        private set
    @Volatile var debugPositionErrorWorld: Vector3d = Vector3d()
        private set
    @Volatile var debugForceWorld: Vector3d = Vector3d()
        private set
    @Volatile var debugOppositeForceWorld: Vector3d = Vector3d()
        private set
    @Volatile var debugCurrentAxisWorld: Vector3d = Vector3d()
        private set
    @Volatile var debugTargetAxisWorld: Vector3d = Vector3d()
        private set
    @Volatile var debugRedstoneWorld: Vector3d = Vector3d()
        private set
    @Volatile var debugControlledPointVelocityWorld: Vector3d = Vector3d()
        private set
    @Volatile var debugTargetPointVelocityWorld: Vector3d = Vector3d()
        private set
    @Volatile var debugRelativeVelocityWorld: Vector3d = Vector3d()
        private set
    @Volatile var debugSubAnchorVelocityWorld: Vector3d = Vector3d()
        private set
    @Volatile var debugHostAnchorVelocityWorld: Vector3d = Vector3d()
        private set
    @Volatile var debugAnchorRelativeVelocityWorld: Vector3d = Vector3d()
        private set

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
        writeGimbalDebug(tag)
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
        readGimbalDebug(tag)
        lastJointMaxAngleDeg = getMaxAngleDeg()
    }

    private fun writeGimbalDebug(tag: CompoundTag) {
        tag.putBoolean("GimbalDebugHasForceData", debugHasForceData)
        tag.putInt("GimbalDebugMode", debugModeOrdinal)
        tag.putDouble("GimbalDebugTargetAngleDeg", debugTargetAngleDeg)
        tag.putDouble("GimbalDebugMass", debugMass)
        tag.putDouble("GimbalDebugRpmAbs", debugRpmAbs)
        tag.putDouble("GimbalDebugMaxForce", debugMaxForce)
        tag.putVector3d("GimbalDebugSubAnchorWorld", debugSubAnchorWorld)
        tag.putVector3d("GimbalDebugHostAnchorWorld", debugHostAnchorWorld)
        tag.putVector3d("GimbalDebugCurrentPointWorld", debugCurrentPointWorld)
        tag.putVector3d("GimbalDebugTargetPointWorld", debugTargetPointWorld)
        tag.putVector3d("GimbalDebugPositionErrorWorld", debugPositionErrorWorld)
        tag.putVector3d("GimbalDebugForceWorld", debugForceWorld)
        tag.putVector3d("GimbalDebugOppositeForceWorld", debugOppositeForceWorld)
        tag.putVector3d("GimbalDebugCurrentAxisWorld", debugCurrentAxisWorld)
        tag.putVector3d("GimbalDebugTargetAxisWorld", debugTargetAxisWorld)
        tag.putVector3d("GimbalDebugRedstoneWorld", debugRedstoneWorld)
        tag.putVector3d("GimbalDebugControlledPointVelocityWorld", debugControlledPointVelocityWorld)
        tag.putVector3d("GimbalDebugTargetPointVelocityWorld", debugTargetPointVelocityWorld)
        tag.putVector3d("GimbalDebugRelativeVelocityWorld", debugRelativeVelocityWorld)
        tag.putVector3d("GimbalDebugSubAnchorVelocityWorld", debugSubAnchorVelocityWorld)
        tag.putVector3d("GimbalDebugHostAnchorVelocityWorld", debugHostAnchorVelocityWorld)
        tag.putVector3d("GimbalDebugAnchorRelativeVelocityWorld", debugAnchorRelativeVelocityWorld)
    }

    private fun readGimbalDebug(tag: CompoundTag) {
        debugHasForceData = tag.getBoolean("GimbalDebugHasForceData")
        if (tag.contains("GimbalDebugMode")) debugModeOrdinal = tag.getInt("GimbalDebugMode")
        if (tag.contains("GimbalDebugTargetAngleDeg")) debugTargetAngleDeg = tag.getDouble("GimbalDebugTargetAngleDeg")
        if (tag.contains("GimbalDebugMass")) debugMass = tag.getDouble("GimbalDebugMass")
        if (tag.contains("GimbalDebugRpmAbs")) debugRpmAbs = tag.getDouble("GimbalDebugRpmAbs")
        if (tag.contains("GimbalDebugMaxForce")) debugMaxForce = tag.getDouble("GimbalDebugMaxForce")
        debugSubAnchorWorld = tag.getVector3d("GimbalDebugSubAnchorWorld") ?: debugSubAnchorWorld
        debugHostAnchorWorld = tag.getVector3d("GimbalDebugHostAnchorWorld") ?: debugHostAnchorWorld
        debugCurrentPointWorld = tag.getVector3d("GimbalDebugCurrentPointWorld") ?: debugCurrentPointWorld
        debugTargetPointWorld = tag.getVector3d("GimbalDebugTargetPointWorld") ?: debugTargetPointWorld
        debugPositionErrorWorld = tag.getVector3d("GimbalDebugPositionErrorWorld") ?: debugPositionErrorWorld
        debugForceWorld = tag.getVector3d("GimbalDebugForceWorld") ?: debugForceWorld
        debugOppositeForceWorld = tag.getVector3d("GimbalDebugOppositeForceWorld") ?: debugOppositeForceWorld
        debugCurrentAxisWorld = tag.getVector3d("GimbalDebugCurrentAxisWorld") ?: debugCurrentAxisWorld
        debugTargetAxisWorld = tag.getVector3d("GimbalDebugTargetAxisWorld") ?: debugTargetAxisWorld
        debugRedstoneWorld = tag.getVector3d("GimbalDebugRedstoneWorld") ?: debugRedstoneWorld
        debugControlledPointVelocityWorld = tag.getVector3d("GimbalDebugControlledPointVelocityWorld") ?: debugControlledPointVelocityWorld
        debugTargetPointVelocityWorld = tag.getVector3d("GimbalDebugTargetPointVelocityWorld") ?: debugTargetPointVelocityWorld
        debugRelativeVelocityWorld = tag.getVector3d("GimbalDebugRelativeVelocityWorld") ?: debugRelativeVelocityWorld
        debugSubAnchorVelocityWorld = tag.getVector3d("GimbalDebugSubAnchorVelocityWorld") ?: debugSubAnchorVelocityWorld
        debugHostAnchorVelocityWorld = tag.getVector3d("GimbalDebugHostAnchorVelocityWorld") ?: debugHostAnchorVelocityWorld
        debugAnchorRelativeVelocityWorld = tag.getVector3d("GimbalDebugAnchorRelativeVelocityWorld") ?: debugAnchorRelativeVelocityWorld
    }

    private fun CompoundTag.putVector3d(key: String, value: Vector3dc) {
        putDouble("${key}x", value.x())
        putDouble("${key}y", value.y())
        putDouble("${key}z", value.z())
    }

    override fun tick() {
        super.tick()
        ticks++
        if (level == null || level!!.isClientSide) return

        tryAssembleNextTick()
        if (isRunning) sendData()
    }

    private fun tryAssembleNextTick() {
        if (!assembleNextTick) return
        if (ticks - lastStateChanged <= cooldown) return
        assembleNextTick = false
        if (!isRunning) assemble()
    }

    @OptIn(PhysTickOnly::class)
    private fun assemble() {
        if (level!!.getBlockState(worldPosition).block !is BearingBlock) return
        val level = level as ServerLevel
        originalDirection = blockState.getValue(BearingBlock.FACING)
        val direction = originalDirection!!
        val attachPoint = worldPosition.relative(direction)

        val worldPos: Vector3dc = worldPosition.center.toJOML()
        val axis = direction.normal.toJOMLD()
        val shipOn = level.getShipObjectManagingPos(worldPosition)
        val ownerFaceAnchor = Vector3d(worldPos).fma(0.5, axis)
        val ownerJointAnchor = Vector3d(ownerFaceAnchor).fma(JOINT_ATTACHMENT_OFFSET_BLOCKS, axis)

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

            val newPos = Vector3d(ownerFaceAnchor).sub(centerPositions.first).add(centerPositions.second)
            shiptraptionID = ship.id
            Pair(newPos, ship)
        } else {
            shiptraptionID = otherShip.id
            val bp = Vector3d(
                otherPos.blockPos.x.toDouble() + 0.5,
                otherPos.blockPos.y.toDouble() + 0.5,
                otherPos.blockPos.z.toDouble() + 0.5
            ).fma(-0.5, axis)
            Pair(bp, otherShip)
        }

        val shipOnID = shipOn?.id

        val posInWorld = shipOn?.transform?.shipToWorld?.transformPosition(
            Vector3d(ownerFaceAnchor).sub(bearingPos).add(shiptraption.inertiaData.centerOfMass), Vector3d()
        ) ?: Vector3d(ownerFaceAnchor).sub(bearingPos).add(shiptraption.inertiaData.centerOfMass)
        val rotInWorld = shipOn?.transform?.shipToWorldRotation?.let { Quaterniond(it) } ?: Quaterniond()
        val scaling = shipOn?.transform?.shipToWorldScaling ?: Vector3d(1.0, 1.0, 1.0)

        shiptraption.unsafeSetTransform(
            BodyTransformFactory.create(
                posInWorld, rotInWorld, scaling, shiptraption.transform.positionInModel
            )
        )

        val ship1rot = directionHingeRotation(direction)
        val ship2rot = directionHingeRotation(direction)
        val pose0 = VSJointPose(Vector3d(bearingPos).fma(JOINT_ATTACHMENT_OFFSET_BLOCKS, axis), ship1rot)
        val pose1 = VSJointPose(ownerJointAnchor, ship2rot)

        val maxAngle = getMaxAngleDeg()
        val limitRad = Math.toRadians(maxAngle.toDouble()).toFloat().coerceAtLeast(MIN_LIMIT_RAD)
        val motions = EnumMap<VSD6Joint.D6Axis, D6Motion>(D6Axis::class.java)
        motions[D6Axis.X] = D6Motion.LOCKED
        motions[D6Axis.Y] = D6Motion.LOCKED
        motions[D6Axis.Z] = D6Motion.LOCKED
        motions[D6Axis.TWIST] = D6Motion.LOCKED
        motions[D6Axis.SWING1] = D6Motion.LIMITED
        motions[D6Axis.SWING2] = D6Motion.LIMITED

        joint = VSD6Joint(
            shipId0 = shiptraptionID,
            pose0 = pose0,
            shipId1 = shipOnID,
            pose1 = pose1,
            compliance = 1e-100,
            motions = motions,
            swingLimit = VSD6Joint.LimitCone(limitRad,limitRad)
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
        val direction = originalDirection ?: blockState.getValue(BearingBlock.FACING)
        val ownerAnchor = Vector3d(worldPosition.center.toJOML()).fma(0.5, bearingAxisLocal)

        val targetRot = mainShip?.transform?.shipToWorldRotation?.let { Quaterniond(it) } ?: Quaterniond()
        val scaling = mainShip?.transform?.shipToWorldScaling ?: Vector3d(1.0, 1.0, 1.0)
        val posInWorld = mainShip?.transform?.shipToWorld?.transformPosition(
            Vector3d(ownerAnchor).sub(bearingPosInSub).add(subShip.inertiaData.centerOfMass), Vector3d()
        ) ?: Vector3d(ownerAnchor).sub(bearingPosInSub).add(subShip.inertiaData.centerOfMass)
        subShip.unsafeSetTransform(
            BodyTransformFactory.create(posInWorld, targetRot, scaling, subShip.transform.positionInModel)
        )

        val inMain = worldPosition.relative(direction, 1)
        val inSubship = Vector3d(bearingPosInSub).fma(0.5, bearingAxisLocal).let { BlockPos.containing(it.x, it.y, it.z) }

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
        clearGimbalDebugSnapshot()
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
        if (isRemoved || !isRunning || joint == null) {
            clearGimbalDebugSnapshot()
            return
        }
        val subShip = physLevel.getShipById(shiptraptionID) ?: run {
            clearGimbalDebugSnapshot()
            return
        }
        clearGimbalDebugSnapshot()
        if (subShip.isStatic) return

        val rsLocal = redstoneVecLocal
        val rsLen = min(rsLocal.length(), 1.0)
        val tiltAngleRad = rsLen * Math.toRadians(getMaxAngleDeg().toDouble())
        val mode = getMode()

        val mass = subShip.mass
        if (mass <= 0.0) return
        val rpmAbs = abs(getSpeed()).toDouble()
        if (rpmAbs < 1e-3) return

        val hostRot = physShip?.transform?.shipToWorldRotation?.let { Quaterniond(it) } ?: Quaterniond()
        val refRot = if (mode == GimbalMode.GYROSCOPIC) Quaterniond(assemblyHostRotation) else hostRot
        val hostAnchorLocal = getOwnerJointAnchorLocal()
        val hostAnchorWorld = physShip?.transform?.shipToWorld?.transformPosition(hostAnchorLocal, Vector3d())
            ?: Vector3d(hostAnchorLocal)
        val subAnchorLocal = getSubJointAnchorLocal()
        val forceApplicationLocal = Vector3d(subShip.centerOfMass)
        val subAnchorWorld = subShip.transform.shipToWorld.transformPosition(subAnchorLocal, Vector3d())
        val forceApplicationWorld = subShip.transform.shipToWorld.transformPosition(forceApplicationLocal, Vector3d())
        val currentAxisWorld = Vector3d(bearingAxisLocal).rotate(subShip.transform.shipToWorldRotation, Vector3d())

        if (mode == GimbalMode.UNLOCKED) {
            if (rsLen < 1e-6) return
            val forceDir = Vector3d(rsLocal).rotate(hostRot)
            val forceLen = forceDir.length()
            if (forceLen < 1e-9) return
            val forceDirUnit = forceDir.div(forceLen, Vector3d())
            val forceMag = rsLen * ClockworkConfig.SERVER.gimbal.gimbalUnlockedForcePerRpmPerKg * rpmAbs * mass
            val force = forceDirUnit.mul(forceMag, Vector3d())
            val subAnchorVelocity = getPointVelocityWorld(subShip, subAnchorWorld)
            val hostAnchorVelocity = physShip?.let { getPointVelocityWorld(it, hostAnchorWorld) } ?: Vector3d()
            setGimbalDebugSnapshot(
                mode = mode,
                targetAngleDeg = Math.toDegrees(tiltAngleRad),
                mass = mass,
                rpmAbs = rpmAbs,
                maxForce = forceMag,
                subAnchorWorld = subAnchorWorld,
                hostAnchorWorld = hostAnchorWorld,
                currentPointWorld = forceApplicationWorld,
                targetPointWorld = forceApplicationWorld,
                positionErrorWorld = Vector3d(),
                forceWorld = force,
                oppositeForceWorld = Vector3d(force).mul(-1.0),
                currentAxisWorld = currentAxisWorld,
                targetAxisWorld = Vector3d(forceDirUnit),
                redstoneWorld = Vector3d(forceDirUnit).mul(rsLen),
                controlledPointVelocityWorld = getPointVelocityWorld(subShip, forceApplicationWorld),
                targetPointVelocityWorld = Vector3d(),
                relativeVelocityWorld = Vector3d(),
                subAnchorVelocityWorld = subAnchorVelocity,
                hostAnchorVelocityWorld = hostAnchorVelocity,
                anchorRelativeVelocityWorld = subAnchorVelocity.sub(hostAnchorVelocity, Vector3d())
            )
            applyForcePair(subShip, physShip, force, forceApplicationLocal, hostAnchorLocal)
            return
        }

        val targetRot = getTargetRotation(refRot, rsLocal, rsLen, tiltAngleRad)
        val targetAxisWorld = Vector3d(bearingAxisLocal).rotate(targetRot, Vector3d())
        val redstoneWorld = if (rsLen > 1e-6) Vector3d(rsLocal).rotate(refRot) else Vector3d()
        val controlledPointLocal = forceApplicationLocal
        val targetOffsetWorld = controlledPointLocal.sub(subAnchorLocal, Vector3d()).rotate(targetRot)
        val targetPointWorld = Vector3d(hostAnchorWorld).add(targetOffsetWorld)
        val currentPointWorld = subShip.transform.shipToWorld.transformPosition(controlledPointLocal, Vector3d())

        val positionError = targetPointWorld.sub(currentPointWorld, Vector3d())

        val subPointVelocity = getPointVelocityWorld(subShip, currentPointWorld)
        val targetPointVelocity = getTargetPointVelocityWorld(physShip, hostAnchorWorld, targetOffsetWorld, mode)
        val relativeVelocity = subPointVelocity.sub(targetPointVelocity, Vector3d())

        val kp = ClockworkConfig.SERVER.gimbal.gimbalPositionErrorMultiplier
        val kd = ClockworkConfig.SERVER.gimbal.gimbalVelocityErrorMultiplier
        val desiredControlledPointAcceleration = positionError.mul(kp, Vector3d())
            .sub(relativeVelocity.mul(kd, Vector3d()))
        val force = desiredControlledPointAcceleration.mul(mass)
        if (!force.isFiniteVec()) return

        val maxForce = ClockworkConfig.SERVER.gimbal.gimbalMaxForcePerRpmPerKg * rpmAbs * mass
        if (maxForce > 0.0 && force.lengthSquared() > maxForce * maxForce) {
            force.normalize(maxForce)
        }
        val subAnchorVelocity = getPointVelocityWorld(subShip, subAnchorWorld)
        val hostAnchorVelocity = physShip?.let { getPointVelocityWorld(it, hostAnchorWorld) } ?: Vector3d()
        if (positionError.lengthSquared() < 1e-12 && relativeVelocity.lengthSquared() < 1e-12) {
            force.set(0.0, 0.0, 0.0)
        }
        setGimbalDebugSnapshot(
            mode = mode,
            targetAngleDeg = Math.toDegrees(tiltAngleRad),
            mass = mass,
            rpmAbs = rpmAbs,
            maxForce = maxForce,
            subAnchorWorld = subAnchorWorld,
            hostAnchorWorld = hostAnchorWorld,
            currentPointWorld = currentPointWorld,
            targetPointWorld = targetPointWorld,
            positionErrorWorld = positionError,
            forceWorld = force,
            oppositeForceWorld = Vector3d(force).mul(-1.0),
            currentAxisWorld = currentAxisWorld,
            targetAxisWorld = targetAxisWorld,
            redstoneWorld = redstoneWorld,
            controlledPointVelocityWorld = subPointVelocity,
            targetPointVelocityWorld = targetPointVelocity,
            relativeVelocityWorld = relativeVelocity,
            subAnchorVelocityWorld = subAnchorVelocity,
            hostAnchorVelocityWorld = hostAnchorVelocity,
            anchorRelativeVelocityWorld = subAnchorVelocity.sub(hostAnchorVelocity, Vector3d())
        )
        applyForcePair(subShip, physShip, force, forceApplicationLocal, hostAnchorLocal)
    }

    private fun getOwnerJointAnchorLocal(): Vector3d =
        Vector3d(worldPosition.center.toJOML()).fma(0.5 + JOINT_ATTACHMENT_OFFSET_BLOCKS, bearingAxisLocal)

    private fun getSubJointAnchorLocal(): Vector3d =
        Vector3d(bearingPosInSub).fma(JOINT_ATTACHMENT_OFFSET_BLOCKS, bearingAxisLocal)

    private fun getTargetRotation(refRot: Quaterniond, rsLocal: Vector3dc, rsLen: Double, tiltAngleRad: Double): Quaterniond {
        val targetRot = Quaterniond(refRot)
        if (rsLen <= 1e-6 || tiltAngleRad <= 1e-9) return targetRot

        val tiltDirLocal = Vector3d(rsLocal).normalize()
        val tiltAxisLocal = Vector3d(bearingAxisLocal).cross(tiltDirLocal, Vector3d())
        val axisLen = tiltAxisLocal.length()
        if (axisLen < 1e-9) return targetRot

        val tiltRot = Quaterniond(AxisAngle4d(tiltAngleRad, tiltAxisLocal.div(axisLen)))
        return targetRot.mul(tiltRot).normalize()
    }

    private fun getPointVelocityWorld(ship: PhysShip, pointWorld: Vector3dc): Vector3d {
        val offset = pointWorld.sub(ship.transform.positionInWorld, Vector3d())
        return Vector3d(ship.angularVelocity).cross(offset, Vector3d()).add(ship.velocity)
    }

    private fun getTargetPointVelocityWorld(
        hostShip: PhysShip?,
        hostAnchorWorld: Vector3dc,
        targetOffsetWorld: Vector3dc,
        mode: GimbalMode
    ): Vector3d {
        if (hostShip == null) return Vector3d()
        val velocity = getPointVelocityWorld(hostShip, hostAnchorWorld)
        if (mode == GimbalMode.LOCKED) {
            velocity.add(Vector3d(hostShip.angularVelocity).cross(targetOffsetWorld, Vector3d()))
        }
        return velocity
    }

    private fun applyForcePair(
        subShip: PhysShip,
        hostShip: PhysShip?,
        force: Vector3dc,
        subForceApplicationLocal: Vector3dc,
        hostAnchorLocal: Vector3dc
    ) {
        if (!force.isFiniteVec()) return
        subShip.applyWorldForceToModelPos(force, Vector3d(subForceApplicationLocal))
        hostShip?.applyWorldForceToModelPos(Vector3d(force).mul(-1.0), Vector3d(hostAnchorLocal))
    }

    private fun setGimbalDebugSnapshot(
        mode: GimbalMode,
        targetAngleDeg: Double,
        mass: Double,
        rpmAbs: Double,
        maxForce: Double,
        subAnchorWorld: Vector3dc,
        hostAnchorWorld: Vector3dc,
        currentPointWorld: Vector3dc,
        targetPointWorld: Vector3dc,
        positionErrorWorld: Vector3dc,
        forceWorld: Vector3dc,
        oppositeForceWorld: Vector3dc,
        currentAxisWorld: Vector3dc,
        targetAxisWorld: Vector3dc,
        redstoneWorld: Vector3dc,
        controlledPointVelocityWorld: Vector3dc,
        targetPointVelocityWorld: Vector3dc,
        relativeVelocityWorld: Vector3dc,
        subAnchorVelocityWorld: Vector3dc,
        hostAnchorVelocityWorld: Vector3dc,
        anchorRelativeVelocityWorld: Vector3dc
    ) {
        debugHasForceData = true
        debugModeOrdinal = mode.ordinal
        debugTargetAngleDeg = targetAngleDeg
        debugMass = mass
        debugRpmAbs = rpmAbs
        debugMaxForce = maxForce
        debugSubAnchorWorld = Vector3d(subAnchorWorld)
        debugHostAnchorWorld = Vector3d(hostAnchorWorld)
        debugCurrentPointWorld = Vector3d(currentPointWorld)
        debugTargetPointWorld = Vector3d(targetPointWorld)
        debugPositionErrorWorld = Vector3d(positionErrorWorld)
        debugForceWorld = Vector3d(forceWorld)
        debugOppositeForceWorld = Vector3d(oppositeForceWorld)
        debugCurrentAxisWorld = Vector3d(currentAxisWorld)
        debugTargetAxisWorld = Vector3d(targetAxisWorld)
        debugRedstoneWorld = Vector3d(redstoneWorld)
        debugControlledPointVelocityWorld = Vector3d(controlledPointVelocityWorld)
        debugTargetPointVelocityWorld = Vector3d(targetPointVelocityWorld)
        debugRelativeVelocityWorld = Vector3d(relativeVelocityWorld)
        debugSubAnchorVelocityWorld = Vector3d(subAnchorVelocityWorld)
        debugHostAnchorVelocityWorld = Vector3d(hostAnchorVelocityWorld)
        debugAnchorRelativeVelocityWorld = Vector3d(anchorRelativeVelocityWorld)
    }

    private fun clearGimbalDebugSnapshot() {
        debugHasForceData = false
        debugMaxForce = 0.0
        debugForceWorld = Vector3d()
        debugOppositeForceWorld = Vector3d()
        debugPositionErrorWorld = Vector3d()
        debugControlledPointVelocityWorld = Vector3d()
        debugTargetPointVelocityWorld = Vector3d()
        debugRelativeVelocityWorld = Vector3d()
        debugSubAnchorVelocityWorld = Vector3d()
        debugHostAnchorVelocityWorld = Vector3d()
        debugAnchorRelativeVelocityWorld = Vector3d()
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
     * Mode slot: shown on lateral sides whose axis is not the max-angle axis.
     * Restricted so it does not share faces with the max-angle widget.
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
     * Distinct behaviour type and net id so value-edit packets route to the max-angle behaviour
     * instead of colliding with the mode behaviour.
     */
    class MaxAngleScrollValueBehaviour(
        label: Component,
        be: com.simibubi.create.foundation.blockEntity.SmartBlockEntity,
        slot: ValueBoxTransform
    ) : ScrollValueBehaviour(label, be, slot) {
        override fun getType(): BehaviourType<*> = TYPE
        override fun netId(): Int = NET_ID

        override fun write(nbt: CompoundTag, clientPacket: Boolean) {
            nbt.putInt(NBT_KEY, value)
        }

        override fun read(nbt: CompoundTag, clientPacket: Boolean) {
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
        private const val JOINT_ATTACHMENT_OFFSET_BLOCKS = 0.5

        fun maxAngleAxisFor(facingAxis: Direction.Axis): Direction.Axis = when (facingAxis) {
            Direction.Axis.X -> Direction.Axis.Z
            Direction.Axis.Y -> Direction.Axis.Z
            Direction.Axis.Z -> Direction.Axis.Y
        }
    }
}
