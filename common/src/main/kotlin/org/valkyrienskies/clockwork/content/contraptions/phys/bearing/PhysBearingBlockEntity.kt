package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import com.simibubi.create.AllSoundEvents
import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.utility.ServerSpeedProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.forces.contraption.BearingController.Companion.getAngle
import org.valkyrienskies.clockwork.content.forces.contraption.BearingData
import org.valkyrienskies.clockwork.content.forces.contraption.BearingController
import org.valkyrienskies.clockwork.platform.api.ContraptionController
import org.valkyrienskies.clockwork.platform.api.ContraptionController.LockedMode
import org.valkyrienskies.clockwork.util.GlueAssembler.collectGlued
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.updateJoint
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.internal.joints.VSFixedJoint
import org.valkyrienskies.core.internal.joints.VSJoint
import org.valkyrienskies.core.internal.joints.VSJointId
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener
import org.valkyrienskies.mod.api.getShipManagingBlock
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.common.assembly.ShipAssembler.assembleToShip
import org.valkyrienskies.mod.common.assembly.VSAssemblyEvents
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.util.SplittingDisablerAttachment
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.world.clipIncludeShips
import org.valkyrienskies.mod.util.getVector3d
import org.valkyrienskies.mod.util.putVector3d
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin

class PhysBearingBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) :
    GeneratingKineticBlockEntity(type, pos, state), IBearingBlockEntity, IDisplayAssemblyExceptions,
    ContraptionController, BlockEntityPhysicsListener {

    var movementMode: ScrollOptionBehaviour<LockedMode>? = null

    /**
     * We use this instead of [facing] because
     * [facing] might change while we're assembled
     * if [ClockworkConfig.Server.allowWrenchingActivatedPhysBearing]
     * is enabled.
     */
    var originalFacing: Direction = facing

    @Volatile
    var jointId: VSJointId = -1
        private set

    var partnerPos: BlockPos? = null
        private set

    @Volatile
    private var queuedJointToAdd: VSJoint? = null
    private var lastException: AssemblyException? = null

    // jointId is updated on phys tick, but for some dumb reasaon the block entity
    // needs sendData to be run from the server tick. So we compare with this
    // in tick() to sync on game tick
    private var lastSyncedJointId: VSJointId = jointId

    @Volatile
    var targetAngle = 0f
        private set

    @Volatile override lateinit var dimension: DimensionId

    @Volatile
    var aligning: Boolean = false
        private set

    private var sequencedAngleLimit = -1.0f
    private var sequencedAngleProgress = 0f

    private val facing: Direction
        get() = blockState.getValue(BearingBlock.FACING)

    private val partnerShipId: ShipId
        get() = level.getShipManagingBlock(partnerPos)?.id ?: -1

    private val joint: VSJoint?
        get() = (level as? ServerLevel)?.gtpa?.getJointById(jointId)

    val isRunning: Boolean
        get() = (jointId != -1)

    // For the visuals
    private var open = false
    private var opening = false
    private var openProgress = 0f
    private var inOutCorner = 0f
    private var cornerShrinking = false
    private var clientAngleDiff = 0f
    private var prevAngle = 0f
    private var coreAngle = 0f
    private var previousCoreAngle = 0f
    val angularSpeed: Float
        get() {
            var speed = convertToAngular(getSpeed())
            if (getSpeed() == 0f) speed = 0f
            if (level!!.isClientSide) {
                speed *= ServerSpeedProvider.get()
                speed += clientAngleDiff / 3f
            }
            return speed
        }

    private fun tickAnimationLogic() {
        if (inOutCorner < 1 && !cornerShrinking) {
            inOutCorner += 0.0075f
        } else if (inOutCorner >= 1) {
            cornerShrinking = true
        }
        if (inOutCorner > 0 && cornerShrinking) {
            inOutCorner -= 0.0075f
        } else if (inOutCorner <= 0) {
            cornerShrinking = false
        }


        if (isRunning && !open && !opening) {
            opening = true
        }
        if (opening && isRunning && openProgress < 1.0f) {
            openProgress += 0.05f
        } else if (openProgress >= 1.0f) {
            opening = false
            open = true
            openProgress = 1f
        }

        if (!isRunning && openProgress > 0.0f) {
            opening = false
            open = true
            openProgress -= 0.05f
        } else if (!isRunning && openProgress <= 0.0f) {
            opening = false
            open = false
            openProgress = 0.0f
        }
    }

    fun getActualAngle(): Double? {
        val level = level as ServerLevel
        val subShip = level.getShipManagingPos(partnerPos ?: return null) ?: return null
        val mainShip = level.getShipManagingPos(worldPosition)
        return getAngle(originalFacing.normal.toJOMLD(), subShip.transform, mainShip?.transform)
    }

    override fun getInterpolatedAngle(partialTicks: Float): Float {
        var partialTicks = partialTicks
        if (isVirtual) return Mth.lerp(partialTicks + .5f, prevAngle, targetAngle)
        if (!isRunning) partialTicks = 0f
        return Mth.lerp(partialTicks, targetAngle, targetAngle + angularSpeed)
    }

    fun getInterpolatedCoreAngle(partialTicks: Float): Float {
        previousCoreAngle = coreAngle
        coreAngle++
        if (coreAngle == 360f) {
            coreAngle = 0f
        }
        return if (isVirtual) Mth.lerp(partialTicks + .5f, previousCoreAngle, coreAngle) else Mth.lerp(
            partialTicks,
            coreAngle,
            coreAngle + 4f
        )
    }

    fun getWingRotOffset(): Float {
        val openProgressMax = 70f
        return when {
            isRunning && open -> openProgressMax.toDouble().toFloat()
            isRunning -> Mth.lerp(openProgress.toDouble(), 0.0, openProgressMax.toDouble()).toFloat()
            !isRunning && open -> Mth.lerp(openProgress.toDouble(), 1.0, openProgressMax.toDouble()).toFloat()
            else -> 0.0f
        }
    }

    private fun movementModeChanged(value: Int) {
        if (level == null || level!!.isClientSide) { return }

        val enteringFollowAngle = movementMode?.get() == LockedMode.FOLLOW_ANGLE && partnerPos != null

        // Prevent abrupt jumps when switching into follow mode
        if (enteringFollowAngle) {
            val subShip = level.getShipManagingPos(partnerPos!!)
            val mainShip = level.getShipManagingPos(worldPosition)
            if (subShip != null) {
                targetAngle = -Math.toDegrees(
                    BearingController.getAngle(
                        originalFacing.normal.toJOMLD(),
                        subShip.transform,
                        mainShip?.transform
                    )
                ).toFloat()
            }

            // So that the fixed joint doesn't do funky interpolation
            lockedCurrentAngle = targetAngle
            lockedInterpolationStartAngle = targetAngle
            lockedInterpolationGoalAngle = targetAngle
            lockedInterpolationTick = 0
        }


        if (!(enteringFollowAngle && jointId != -1)) {
            updateJoint()
        }
        sendData()
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        movementMode = ScrollOptionBehaviour(
            LockedMode::class.java, Component.translatable("$MOD_ID.phys_bearing.rotation_mode"),
            this, movementModeSlot
        )
        movementMode!!.withCallback { movementModeChanged(it) }
        movementMode!!.requiresWrench()
        behaviours.add(movementMode!!)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        tag.putInt("jointId", jointId)
        tag.putString("originalFacing", originalFacing.name)

        // Save rotational state
        tag.putFloat("targetAngle", targetAngle)
        tag.putFloat("sequencedAngleLimit", sequencedAngleLimit)
        tag.putFloat("sequencedAngleProgress", sequencedAngleProgress)
        tag.putBoolean("aligning", aligning)

        partnerPos?.let { pos -> tag.putVector3d("partnerPos", pos.center.toJOML()) }
        tag.putLong("partnerShipId", partnerShipId)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (tag.isEmpty) return
        if (tag.contains("jointId")) {
            jointId = tag.getInt("jointId")
        }

        if (tag.contains("originalFacing")) {
            originalFacing = Direction.valueOf(tag.getString("originalFacing"))
        } else {
            originalFacing = facing
        }

        // Load rotational state
        targetAngle = tag.getFloat("targetAngle")
        sequencedAngleLimit = tag.getFloat("sequencedAngleLimit")
        sequencedAngleProgress = tag.getFloat("sequencedAngleProgress")
        aligning = tag.getBoolean("aligning")

        lockedCurrentAngle = targetAngle
        lockedInterpolationStartAngle = targetAngle
        lockedInterpolationGoalAngle = targetAngle
        lockedInterpolationTick = 0

        if (tag.contains("partnerPosx")) {
            val vec = tag.getVector3d("partnerPos")!!
            partnerPos = BlockPos.containing(Vec3(vec.x, vec.y, vec.z))
        }

        if (clientPacket) { return }
        val level = level as? ServerLevel ?: return
        updateJoint(level)
    }

    fun assemble() {
        // Prevent making joints twice
        if (queuedJointToAdd != null) return
        if (jointId != -1) return
        val level = level as? ServerLevel ?: return
        if (level.getBlockState(worldPosition).block !is BearingBlock) return

        originalFacing = facing
        val targetPosition = worldPosition.relative(originalFacing)

        val worldPos: Vector3dc = worldPosition.center.toJOML()
        val axis: Vector3d = originalFacing.normal.toJOMLD()
        val shipOn = level.getLoadedShipManagingPos(worldPosition)

        val startPos = Vector3d(worldPos).fma(0.5, axis)
        val endPos = Vector3d(worldPos).fma(1.5, axis)

        fun Vector3d.toVec3() = Vec3(this.x, this.y, this.z)

        val otherHit = level.clipIncludeShips(
            ClipContext(
                (shipOn?.transform?.shipToWorld?.transformPosition(startPos) ?: startPos).toVec3(),
                (shipOn?.transform?.shipToWorld?.transformPosition(endPos) ?: endPos).toVec3(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
            ), false, shipOn?.id
        )

        val otherShip = level.getLoadedShipManagingPos(otherHit.blockPos)

        val newPartnerPos: BlockPos
        if (otherShip == null) {
            val selection: DenseBlockPosSet
            try {
                selection = collectGlued(level, targetPosition) ?: return
                selection.remove(worldPosition.x, worldPosition.y, worldPosition.z)
                lastException = null
            } catch (e: AssemblyException) {
                lastException = e
                sendData()
                return
            }

            var centerPositions: Pair<Vector3d, Vector3d> = Pair(Vector3d(), Vector3d())
            val event = VSAssemblyEvents.onPasteBeforeBlocksAreLoaded.on {
                centerPositions = it.centerPosition.first.get(Vector3d()) to it.centerPosition.second.get(Vector3d())
            }
            assembleToShip(
                level,
                selection.toSet().map { it.toMinecraft() }.toSet(),
                1.0
            )
            event.unregister()

            val migratedPos = Vector3d(targetPosition.center.toJOML()).sub(centerPositions.first).add(centerPositions.second)
            newPartnerPos = BlockPos.containing(migratedPos.x, migratedPos.y, migratedPos.z)
        } else {
            newPartnerPos = otherHit.blockPos
        }

        partnerPos = newPartnerPos

        ClockworkSounds.PHYSICS_INFUSER_LIGHTNING.playOnServer(level, worldPosition)

        updateJoint(level)
        sendData()
    }

    override fun remove() {
        if (!(level?.isClientSide ?: true)) {
            removeJoint(level as ServerLevel)
        }
        super.remove()
    }

    fun disassemble() {
        val level = level as? ServerLevel ?: return
        val partnerPos = partnerPos ?: return
        val subShip = level.getLoadedShipManagingPos(partnerPos) ?: run {
            removeJoint(level)
            return
        }
        val mainShip = level.getLoadedShipManagingPos(worldPosition)

        val axis = originalFacing.normal.toJOMLD()
        val angleDegrees = Math.toDegrees(BearingController.getAngle(axis, subShip.transform, mainShip?.transform))

        if (abs(angleDegrees) > DISASSEMBLE_ANGLE_TOLERANCE_DEGREES) {
            if (!aligning) {
                aligning = true
                sendData()
            }
            return
        }
        aligning = false

        val inMain = worldPosition.relative(originalFacing, 1)
        val inSubship = partnerPos

        val aabb = subShip.shipAABB ?: run {
            removeJoint(level)
            return
        }
        val blocks = DenseBlockPosSet()
        for (x in aabb.minX() - 1 until aabb.maxX() + 1) {
            for (z in aabb.minZ() - 1 until aabb.maxZ() + 1) {
                for (y in aabb.minY() - 1 until aabb.maxY() + 1) {
                    blocks.add(x, y, z)
                } } }

        val subCouldSplit = subShip.getAttachment<SplittingDisablerAttachment>()?.let {
            if (it.canSplit()) { it.disableSplitting(); true } else { false }
        } ?: false
        val mainCouldSplit = mainShip?.getAttachment<SplittingDisablerAttachment>()?.let {
            if (it.canSplit()) { it.disableSplitting(); true } else { false }
        } ?: false

        val hasMoved = PhysBearingAssembler.moveBlocksFromTo(level, blocks, true, inSubship, inMain, subShip, mainShip)

        if (subCouldSplit) { subShip.getAttachment<SplittingDisablerAttachment>()?.enableSplitting() }
        if (mainCouldSplit) { mainShip?.getAttachment<SplittingDisablerAttachment>()?.enableSplitting() }

        if (!hasMoved) {
            aligning = false
            return
        }

        open = false
        opening = false
        openProgress = 0f
        inOutCorner = 0f
        cornerShrinking = false

        removeJoint(level)
        AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(level, worldPosition)
        sendData()
    }

    private fun removeJoint(level: ServerLevel) {
        if (jointId != -1) {
            level.gtpa.removeJoint(jointId)
        }
        jointId = -1
        queuedJointToAdd = null
        partnerPos = null
    }

    fun updateJoint(level: ServerLevel? = this.level as? ServerLevel) {
        level ?: return
        val updatedJoint = buildJoint()

        if (jointId != -1 && updatedJoint != null) {
            level.gtpa.updateJoint(jointId, updatedJoint)
        } else {
            queuedJointToAdd = updatedJoint
        }
        sendData()
    }

    override fun onSpeedChanged(previousSpeed: Float) {
        sequencedAngleLimit = -1.0f
        sequencedAngleProgress = 0.0f

        if (sequenceContext != null && sequenceContext.instruction == SequencerInstructions.TURN_ANGLE) {
            sequencedAngleLimit = sequenceContext.getEffectiveValue(theoreticalSpeed.toDouble()).toFloat()
        }

        super.onSpeedChanged(previousSpeed)
    }

    override fun tick() {
        super.tick()
        if (level!!.isClientSide) {
            tickAnimationLogic()
            return
        }

        if (jointId != lastSyncedJointId) {
            lastSyncedJointId = jointId
            sendData()
        }

        if (joint != null) {
            val angularSpeed = convertToAngular(getSpeed()) * if (originalFacing == Direction.WEST || originalFacing == Direction.NORTH || originalFacing == Direction.DOWN) 1 else -1
            var diff = 0.0f
            if (sequencedAngleLimit >= 0.0f) {
                val sequencedAngleLimit = sequencedAngleLimit * angularSpeed.sign

                sequencedAngleProgress += angularSpeed

                if (angularSpeed > 0 && sequencedAngleProgress > sequencedAngleLimit
                    || angularSpeed < 0 && sequencedAngleProgress < sequencedAngleLimit) {
                    diff = sequencedAngleProgress - sequencedAngleLimit
                    sequencedAngleProgress = sequencedAngleLimit
                }
            }

            val newAngle = targetAngle + angularSpeed - diff
            if (movementMode?.get() == LockedMode.FOLLOW_ANGLE || aligning) {
                if (aligning) {
                    targetAngle = 0.0f
                    if (lockedInterpolationGoalAngle != 0.0f) {
                        setLockedAngleGoal(0.0f)
                    }
                    disassemble()
                } else if (newAngle != targetAngle) {
                    targetAngle = newAngle
                    setLockedAngleGoal(targetAngle)
                }
            } else {
                // Preserve legacy wrapping behavior for non-locked modes.
                targetAngle = when {
                    newAngle >= 360f * 2 -> newAngle - 360f * 2
                    newAngle < 0f -> newAngle + 360f * 2
                    else -> newAngle
                }
            }

            updateControllerData()
        }
    }

    private fun updateControllerData() {
        val joint = joint ?: return
        val thisShip = level!!.getShipManagingPos(worldPosition)?.id ?: -1L

        val data = if (movementMode!!.get() == LockedMode.FOLLOW_ANGLE) {
            BearingData(
                originalFacing.normal.toJOMLD(),
                Math.toRadians(targetAngle.toDouble()),
                0f,
                false,
                true,
                aligning,
                thisShip,
                joint.pose0.pos.get(Vector3d()),
                joint.pose1.pos.get(Vector3d())
            )
        } else {
            BearingData(
                originalFacing.normal.toJOMLD(),
                -Math.toRadians(targetAngle.toDouble()), // why negative here? idk but it breaks without it
                if (aligning) 0.0f else getRealisticAngularSpeed(),
                movementMode?.get() == LockedMode.LOCKED,
                false,
                aligning,
                thisShip,
                joint.pose0.pos.get(Vector3d()),
                joint.pose1.pos.get(Vector3d())
            )
        }

        val serverLevel = level as? ServerLevel ?: return
        val controller = BearingController.getOrCreate(serverLevel.getLoadedShipManagingPos(partnerPos ?: return) ?: return)

        controller.setData(partnerPos!!, data)
    }

    @PhysTickOnly
    override fun physTick(physShip: PhysShip?, physLevel: PhysLevel) {
        if (isRemoved) return

        checkToAddJoint(physLevel)

        if (movementMode?.get() == LockedMode.FOLLOW_ANGLE) {
            updateFixedJoint(physLevel)
        }
    }

    @PhysTickOnly
    private fun checkToAddJoint(physLevel: PhysLevel) {
        queuedJointToAdd ?: return

        physLevel as VsiPhysLevel
        // If either ship isn't loaded, skip
        if (
            queuedJointToAdd!!.shipId0 != null && physLevel.getShipById(queuedJointToAdd!!.shipId0!!) == null ||
            queuedJointToAdd!!.shipId1 != null && physLevel.getShipById(queuedJointToAdd!!.shipId1!!) == null
        ) return

        val id = physLevel.addJoint(queuedJointToAdd!!)
        if (id == -1) {
            return
        }
        jointId = id
        queuedJointToAdd = null
    }

    // Used for follow angle mode only
    @Volatile private var lockedInterpolationTick = 0
    @Volatile private var lockedInterpolationStartAngle = targetAngle
    @Volatile private var lockedInterpolationGoalAngle = targetAngle
    @Volatile private var lockedCurrentAngle = targetAngle

    @PhysTickOnly
    private fun updateFixedJoint(physLevel: PhysLevel) {
        joint ?: return

        val interpProgress = (lockedInterpolationTick + 1).toDouble() / 3.0
        val shortestDelta = shortestAngleDeltaDegrees(lockedInterpolationStartAngle, lockedInterpolationGoalAngle)
        val interpolatedAngle = (lockedInterpolationStartAngle + shortestDelta * interpProgress).toFloat()
        val angle = Math.toRadians(interpolatedAngle.toDouble())

        physLevel as VsiPhysLevel

        //AxisAngle4d clamps angle, so when going from 359 to 0 degrees quat jumps from -0.999 w to 0.999 w or smth like that
        // which causes krunch to incorrectly interpolate, so i just extend angle range to [0, 720) and manually do this shit
        val s = sin(angle * 0.5)
        val fRot2 = Quaterniond(
            originalFacing.normal.x * s,
            originalFacing.normal.y * s,
            originalFacing.normal.z * s,
            org.joml.Math.cosFromSin(s, angle * 0.5)
        ).mul(getHingeRotation(originalFacing))
        val fRot1 = getHingeRotation(originalFacing)

        val updatedJoint = VSFixedJoint(
            joint!!.shipId0, VSJointPose(joint!!.pose0.pos, fRot1),
            joint!!.shipId1, VSJointPose(joint!!.pose1.pos, fRot2),
            compliance = 1e-100
        )
        updatedJoint.serialized()

        physLevel.updateJoint(jointId, updatedJoint)

        lockedCurrentAngle = interpolatedAngle
        lockedInterpolationTick = min(lockedInterpolationTick + 1, 2)
    }

    private fun setLockedAngleGoal(goalAngle: Float) {
        if (!goalAngle.isFinite()) {
            return
        }

        if (lockedInterpolationGoalAngle == goalAngle && lockedCurrentAngle == goalAngle) {
            return
        }

        lockedInterpolationStartAngle = lockedCurrentAngle
        lockedInterpolationGoalAngle = goalAngle
        lockedInterpolationTick = 0
    }

    private fun shortestAngleDeltaDegrees(from: Float, to: Float): Float {
        var delta = to - from
        while (delta > 180f) {
            delta -= 360f
        }
        while (delta < -180f) {
            delta += 360f
        }
        return delta
    }

    fun getRealisticAngularSpeed(): Float {
        val dir = originalFacing
        return getSpeed() * 2f * PI.toFloat() / 60f * if (dir == Direction.WEST || dir == Direction.NORTH || dir == Direction.DOWN) 1 else -1
    }

    private fun buildJoint(): VSJoint? {
        val partnerPos = partnerPos ?: return null

        val thisShipId = level.getShipManagingPos(worldPosition)?.id

        val pose0 = VSJointPose(worldPosition.center.toJOML(), getHingeRotation(originalFacing))
        val pose1 = VSJointPose(partnerPos.relative(originalFacing.opposite).center.toJOML(), getHingeRotation(originalFacing))

        // If we're in FOLLOW_ANGLE, this joint will be replaced next physics tick
        val joint = VSRevoluteJoint(thisShipId, pose0, partnerShipId, pose1, compliance = 1e-100, driveFreeSpin = true)

        joint.serialized()
        return joint
    }

    private fun getHingeRotation(localDirection: Direction): Quaterniond {
        val rotationQuaternion: Quaterniond = when (localDirection) {
            Direction.UP -> {
                Quaterniond()
            }
            Direction.DOWN -> {
                Quaterniond(AxisAngle4d(Math.PI, Vector3d(1.0, 0.0, 0.0)))
            }
            Direction.NORTH -> {
                Quaterniond(AxisAngle4d(Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                    Quaterniond(
                        AxisAngle4d(
                            Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)
                        )
                    )
                ).normalize()
            }
            Direction.EAST -> {
                Quaterniond(AxisAngle4d(0.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                    Quaterniond(
                        AxisAngle4d(
                            Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)
                        )
                    )
                ).normalize()
            }
            Direction.SOUTH -> {
                Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0))).normalize()
            }
            Direction.WEST -> {
                Quaterniond(AxisAngle4d(1.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                    Quaterniond(
                        AxisAngle4d(
                            Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)
                        )
                    )
                ).normalize()
            }
        }

        return rotationQuaternion.mul(
            Quaterniond(AxisAngle4d(Math.toRadians(90.0), 0.0, 0.0, 1.0)),
            Quaterniond()
        ).normalize()
    }

    override fun setAngle(forcedAngle: Float) {
        if (!forcedAngle.isFinite()) return
        targetAngle = forcedAngle
    }
    override fun getLastAssemblyException(): AssemblyException? = lastException
    override fun getBlockPosition(): BlockPos = worldPosition
    override fun isWoodenTop(): Boolean = false
    override fun attach(contraption: ControlledContraptionEntity) {}
    override fun onStall() { if (!level!!.isClientSide) sendData() }
    override fun isValid(): Boolean = !isRemoved
    override fun isAttachedTo(contraption: AbstractContraptionEntity): Boolean = false

    companion object {
        private const val DISASSEMBLE_ANGLE_TOLERANCE_DEGREES = 5
    }
}
