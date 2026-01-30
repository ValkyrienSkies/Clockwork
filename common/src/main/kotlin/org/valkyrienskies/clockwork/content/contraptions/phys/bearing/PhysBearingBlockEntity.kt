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
import com.simibubi.create.foundation.item.TooltipHelper
import com.simibubi.create.foundation.utility.ServerSpeedProvider
import net.createmod.catnip.math.AngleHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.*
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.forces.contraption.BearingController.Companion.getAngle
import org.valkyrienskies.clockwork.platform.api.ContraptionController
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.ClockworkConstants.Nbt.ORIGINAL_DIRECTION
import org.valkyrienskies.clockwork.util.ClockworkUtils.getVector3d
import org.valkyrienskies.clockwork.util.GlueAssembler.collectGlued
import org.valkyrienskies.clockwork.util.minus
import org.valkyrienskies.clockwork.util.plus
import org.valkyrienskies.clockwork.util.times
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.internal.joints.*
import org.valkyrienskies.core.impl.bodies.properties.BodyTransformFactory
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3d
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.common.*
import org.valkyrienskies.mod.common.assembly.ICopyableBlock
import org.valkyrienskies.mod.common.assembly.ShipAssembler.assembleToShip
import org.valkyrienskies.mod.common.assembly.VSAssemblyEvents
import org.valkyrienskies.mod.common.util.SplittingDisablerAttachment
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.world.clipIncludeShips
import org.valkyrienskies.mod.util.putVector3d
import java.lang.Math
import kotlin.math.*

class PhysBearingBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) :
    GeneratingKineticBlockEntity(type, pos, state), IBearingBlockEntity, IDisplayAssemblyExceptions,
    ContraptionController, BlockEntityPhysicsListener {

    var assembleNextTick = false
    var movementMode: ScrollOptionBehaviour<PhysBearingRotationMode>? = null
    var isRunning = false
        private set
    var shiptraptionID = NO_SHIPTRAPTION_ID
        private set
    var targetAngle = 0f
        get() = field
        private set(idk) {field = idk}
    private var targetAngleUnwrapped = 0.0
    private var isSpeedDrivenTargetAngle = false
    var disassembleWhenPossible = false
        private set
    @Volatile var joint : VSJoint? = null
        private set
    @Volatile var jointID : Int = -1
        private set
    @Volatile private var mainShipId: Long = UNKNOWN_MAIN_SHIP_ID
    private var unresolvedMainShipTicks: Int = 0
    @Volatile private var driveWarmupTicks: Int = 0
    @Volatile private var shouldVerifyConnection: Boolean = false
    @Volatile private var desiredModeOrdinal: Int = PhysBearingRotationMode.UNLOCKED.ordinal
    @Volatile private var desiredAngularVelocity: Float = 0f
    @Volatile private var physTargetAngle: Double = 0.0
    @Volatile private var physAligning: Boolean = false
    private var lastFixedJointMainRot: Quaterniond? = null
    private var followAngleSmoothInitialized: Boolean = false
    private var followAngleSmoothTimeSeconds: Double = 0.0
    private var followAngleSmoothFromAngleRad: Double = 0.0
    private var followAngleSmoothToAngleRad: Double = 0.0
    private var followAngleSmoothCurrentAngleRad: Double = 0.0
    private var followAngleHoldAngleRad: Double? = null
    private var followAngleWasRotationApplied: Boolean = false

    private var lastException: AssemblyException? = null
    private var open = false
    private var originalDirection: Direction? = null
    private var clientAngleDiff = 0f
    private var prevAngle = 0f
    private var coreAngle = 0f
    private var previousCoreAngle = 0f

    private var opening = false
    private var openProgress = 0f
    private var openProgressMax = 70f
    private var inOutCorner = 0f
    private var cornerShrinking = false

    private var ticks = 0
    private var lastStateChanged = 0
    private var cooldown = 20

    private var sequencedAngleLimit = -1.0f
    private var sequencedAngleProgress = 0f

    //pos of bearing in subship coordinates
    private var bearingPos: Vector3d = Vector3d()
    private var aligning = false
    private var bearingAxis: Vector3d = Vector3d()

    private var lastMode = PhysBearingRotationMode.UNLOCKED

    init {
        setLazyTickRate(3)
    }

    private fun movementModeChanged(value: Int) {
        if (level == null || level!!.isClientSide) {return}
        sendData()
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        movementMode = ScrollOptionBehaviour(
            PhysBearingRotationMode::class.java, Component.translatable("$MOD_ID.phys_bearing.rotation_mode"),
            this, movementModeSlot
        )
        movementMode!!.withCallback{movementModeChanged(it)}
        movementMode!!.requiresWrench()
        behaviours.add(movementMode!!)
    }

    @Volatile override lateinit var dimension: DimensionId
    override fun physTick(physShip: PhysShip?, physLevel: PhysLevel) {
        if (isRemoved) return
        val vsiPhysLevel = physLevel as? VsiPhysLevel ?: return

        if (!isRunning || shiptraptionID == NO_SHIPTRAPTION_ID) return
        val dtSeconds = if (this::dimension.isInitialized) {
            ClockworkMod.getLastPhysDeltaSeconds(dimension)
        } else {
            1.0 / 60.0
        }

        // If this bearing is mounted on a ship, prefer the physics-provided owning ship id. This prevents cases where
        // game ticks run before the owning ship is fully loaded and we accidentally treat it as world-attached.
        if (physShip != null && mainShipId != physShip.id) {
            mainShipId = physShip.id
        }

        // Prevent creating a world-attached joint if this bearing is mounted on an unloaded ship.
        if (mainShipId == UNKNOWN_MAIN_SHIP_ID) return
        val subPhysShip = physLevel.getShipById(shiptraptionID) ?: return
        val mainPhysShip = if (mainShipId == NO_SHIPTRAPTION_ID) null else physLevel.getShipById(mainShipId) ?: return

        val desiredMode = PhysBearingRotationMode.entries.getOrElse(desiredModeOrdinal) { PhysBearingRotationMode.UNLOCKED }
        val shouldFollowAngle = desiredMode == PhysBearingRotationMode.FOLLOW_ANGLE || physAligning
        val mainId: Long? = if (mainShipId == NO_SHIPTRAPTION_ID) null else mainShipId

        val direction = originalDirection ?: return
        val axis = bearingAxis
        val hingeRot = getHingeRotation(direction)

        // Place the hinge on the shared face between the bearing block and the first block of the attached ship.
        // This avoids large lever-arms that can cause instability when the sub-ship is heavy/off-center.
        val defaultSubPos = Vector3d(bearingPos).fma(0.5, axis, Vector3d())
        val defaultMainPos = Vector3d(worldPosition.center.toJOML()).fma(0.5, axis, Vector3d())
        val subPose = VSJointPose(defaultSubPos, hingeRot)
        val mainPose = VSJointPose(defaultMainPos, hingeRot)
        val existing = if (jointID != -1) vsiPhysLevel.getJointById(jointID) else null

        fun getSubMainPoseData(joint: VSJoint): Triple<Vector3dc, Vector3dc, Boolean>? = when (joint) {
            is VSRevoluteJoint -> when {
                joint.shipId0 == shiptraptionID && joint.shipId1 == mainId -> Triple(joint.pose0.pos, joint.pose1.pos, true)
                joint.shipId1 == shiptraptionID && joint.shipId0 == mainId -> Triple(joint.pose1.pos, joint.pose0.pos, false)
                else -> null
            }
            is VSFixedJoint -> when {
                joint.shipId0 == shiptraptionID && joint.shipId1 == mainId -> Triple(joint.pose0.pos, joint.pose1.pos, true)
                joint.shipId1 == shiptraptionID && joint.shipId0 == mainId -> Triple(joint.pose1.pos, joint.pose0.pos, false)
                else -> null
            }
            else -> null
        }

        val existingPoseData = existing?.let(::getSubMainPoseData)
        val (subPos, mainPos, subIsPose0) = existingPoseData
            ?: Triple(subPose.pos, mainPose.pos, mainId != null)

        val fixedJointMaxForceTorque = run {
            val axisGlobal = Vector3d(axis)
            mainPhysShip?.transform?.shipToWorldRotation?.transform(axisGlobal)

            val inertia = getEffectiveAngularInertia(subPhysShip, mainPhysShip, axisGlobal, subPos, mainPos)
            val omegaRelative = Vector3d(subPhysShip.angularVelocity).also { rel ->
                if (mainPhysShip != null && !mainPhysShip.isStatic) rel.sub(mainPhysShip.angularVelocity)
            }
            val omegaAlongAxis = abs(axisGlobal.dot(omegaRelative))
            val accelToStopNow = if (dtSeconds > 1e-6) omegaAlongAxis / dtSeconds else omegaAlongAxis
            val targetAccel = max(FIXED_JOINT_MAX_ANG_ACCEL_RAD_S2, accelToStopNow)

            val maxTorque = (inertia * targetAccel).coerceIn(FIXED_JOINT_MIN_TORQUE, FIXED_JOINT_MAX_TORQUE_CAP)
            VSJointMaxForceTorque(FIXED_JOINT_MAX_FORCE, maxTorque.toFloat())
        }

        fun fixedJointForAngle(targetAngleRad: Double): VSFixedJoint {
            val mainRot = quaternionAroundAxis(axis, targetAngleRad).mul(hingeRot, Quaterniond()).normalize()
            val last = lastFixedJointMainRot
            if (last != null && mainRot.dot(last) < 0.0) {
                mainRot.set(-mainRot.x, -mainRot.y, -mainRot.z, -mainRot.w)
            }
            lastFixedJointMainRot = Quaterniond(mainRot)
            return if (subIsPose0) {
                VSFixedJoint(
                    shipId0 = shiptraptionID,
                    pose0 = VSJointPose(Vector3d(subPos), hingeRot),
                    shipId1 = mainId,
                    pose1 = VSJointPose(Vector3d(mainPos), mainRot),
                    maxForceTorque = fixedJointMaxForceTorque,
                    compliance = FIXED_JOINT_COMPLIANCE,
                )
            } else {
                VSFixedJoint(
                    shipId0 = mainId,
                    pose0 = VSJointPose(Vector3d(mainPos), mainRot),
                    shipId1 = shiptraptionID,
                    pose1 = VSJointPose(Vector3d(subPos), hingeRot),
                    maxForceTorque = fixedJointMaxForceTorque,
                    compliance = FIXED_JOINT_COMPLIANCE,
                )
            }
        }

        val rotationApplied = !physAligning && abs(desiredAngularVelocity) > 1e-6f
        if (!shouldFollowAngle) {
            followAngleHoldAngleRad = null
            followAngleWasRotationApplied = false
        } else if (rotationApplied || physAligning) {
            followAngleHoldAngleRad = null
        } else if (followAngleWasRotationApplied) {
            val currentTargetRad = if (followAngleSmoothInitialized) {
                followAngleSmoothCurrentAngleRad
            } else {
                Math.toRadians(physTargetAngle)
            }
            val wrapped = getAngle(axis, subPhysShip.transform, mainPhysShip?.transform)
            val unwrapped = wrapped + (Math.PI * 2.0) * round((currentTargetRad - wrapped) / (Math.PI * 2.0))
            followAngleHoldAngleRad = unwrapped
        }

        if (shouldVerifyConnection) {
            if (existing != null) {
                joint = existing
                shouldVerifyConnection = false
            } else {
                jointID = -1
                joint = null
                val newJoint: VSJoint = if (shouldFollowAngle) {
                    lastFixedJointMainRot = null
                    followAngleSmoothInitialized = false
                    followAngleHoldAngleRad = null
                    followAngleWasRotationApplied = rotationApplied
                    val targetAngleRad = when {
                        physAligning -> 0.0
                        rotationApplied -> Math.toRadians(physTargetAngle)
                        else -> followAngleHoldAngleRad ?: Math.toRadians(physTargetAngle)
                    }
                    fixedJointForAngle(smoothFollowAngleTarget(targetAngleRad, dtSeconds, rotationApplied))
                } else {
                    lastFixedJointMainRot = null
                    followAngleSmoothInitialized = false
                    if (subIsPose0) {
                        VSRevoluteJoint(
                            shiptraptionID,
                            subPose,
                            mainId,
                            mainPose,
                            driveFreeSpin = true,
                        )
                    } else {
                        VSRevoluteJoint(
                            mainId,
                            mainPose,
                            shiptraptionID,
                            subPose,
                            driveFreeSpin = true,
                        )
                    }
                }
                val id = vsiPhysLevel.addJoint(newJoint)
                if (id != -1) {
                    jointID = id
                    joint = newJoint
                    shouldVerifyConnection = false
                    driveWarmupTicks = if (shouldFollowAngle) 0 else REVOLUTE_DRIVE_WARMUP_TICKS
                }
                return
            }
        } else if (existing == null) {
            return
        }

        val updated: VSJoint = if (shouldFollowAngle) {
            val targetAngleRad = when {
                physAligning -> 0.0
                rotationApplied -> Math.toRadians(physTargetAngle)
                else -> followAngleHoldAngleRad ?: Math.toRadians(physTargetAngle)
            }
            when (existing) {
                is VSFixedJoint -> {
                    fixedJointForAngle(smoothFollowAngleTarget(targetAngleRad, dtSeconds, rotationApplied))
                }
                is VSRevoluteJoint -> {
                    lastFixedJointMainRot = null
                    followAngleSmoothInitialized = false
                    followAngleHoldAngleRad = null
                    followAngleWasRotationApplied = rotationApplied
                    fixedJointForAngle(smoothFollowAngleTarget(targetAngleRad, dtSeconds, rotationApplied))
                }
                else -> return
            }
        } else {
            when (existing) {
                is VSRevoluteJoint -> existing.copy(
                    maxForceTorque = existing.maxForceTorque,
                    driveVelocity = null,
                    driveForceLimit = null,
                    driveGearRatio = null,
                    driveFreeSpin = true
                )
                is VSFixedJoint -> {
                    lastFixedJointMainRot = null
                    followAngleSmoothInitialized = false
                    driveWarmupTicks = REVOLUTE_DRIVE_WARMUP_TICKS
                    VSRevoluteJoint(
                        existing.shipId0,
                        existing.pose0,
                        existing.shipId1,
                        existing.pose1,
                        driveFreeSpin = true,
                    )
                }
                else -> return
            }
        }
        if (updated != existing) {
            vsiPhysLevel.updateJoint(jointID, updated)
            joint = updated
        }

        if (shouldFollowAngle) {
            followAngleWasRotationApplied = rotationApplied
            return
        }

        // Drive revolute rotation via torque instead of the built-in velocity drive.
        if (driveWarmupTicks > 0) {
            driveWarmupTicks--
            return
        }
        if (abs(desiredAngularVelocity) <= 1e-6f) return

        val revoluteJoint = updated as? VSRevoluteJoint ?: return
        val (subLocalPos, mainLocalPos) = if (revoluteJoint.shipId0 == shiptraptionID) {
            revoluteJoint.pose0.pos to revoluteJoint.pose1.pos
        } else {
            revoluteJoint.pose1.pos to revoluteJoint.pose0.pos
        }

        val torque = computeUnlockedTorque(
            subShip = subPhysShip,
            mainShip = mainPhysShip,
            bearingAxis = axis,
            desiredAngularVelocity = desiredAngularVelocity,
            subLocalPos = subLocalPos,
            mainLocalPos = mainLocalPos,
            dtSeconds = dtSeconds,
        )

        if (torque.lengthSquared() > 1e-18) {
            val torqueMag = torque.length()
            if (torqueMag > MAX_APPLIED_TORQUE_MAG) {
                torque.mul(MAX_APPLIED_TORQUE_MAG / torqueMag)
            }
            if (!subPhysShip.isStatic) subPhysShip.applyWorldTorque(torque)
            if (mainPhysShip != null && !mainPhysShip.isStatic) {
                mainPhysShip.applyWorldTorque(torque.mul(-1.0, Vector3d()))
            }
        }
    }

    public override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)

        tag.putBoolean(ClockworkConstants.Nbt.RUNNING, isRunning)
        tag.putFloat(ClockworkConstants.Nbt.ANGLE, targetAngle)
        tag.putDouble("targetAngleUnwrapped", targetAngleUnwrapped)
        if (mainShipId != UNKNOWN_MAIN_SHIP_ID) {
            tag.putLong("mainShipId", mainShipId)
        }
        if (shiptraptionID != NO_SHIPTRAPTION_ID) {
            tag.putLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID, shiptraptionID)
        }
        if (originalDirection != null) {
            tag.putInt(ORIGINAL_DIRECTION, originalDirection!!.ordinal)
        }
        AssemblyException.write(tag, lastException)
        tag.putBoolean(ClockworkConstants.Nbt.OPEN, open)
        tag.putFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_LIMIT, sequencedAngleLimit)
        tag.putFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_PROGRESS, sequencedAngleProgress)

        tag.putVector3d("bearingPos", bearingPos)
        tag.putVector3d("bearingAxis", bearingAxis)
        tag.putBoolean("aligning", aligning)
        if (isRunning && shiptraptionID != NO_SHIPTRAPTION_ID && jointID != -1) {
            tag.putInt("jointID", jointID)
        }
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        val angleBefore = targetAngle
        open = tag.getBoolean(ClockworkConstants.Nbt.OPEN)
        isRunning = tag.getBoolean(ClockworkConstants.Nbt.RUNNING)
        targetAngle = tag.getFloat(ClockworkConstants.Nbt.ANGLE)
        targetAngleUnwrapped = if (tag.contains("targetAngleUnwrapped")) tag.getDouble("targetAngleUnwrapped") else targetAngle.toDouble()
        isSpeedDrivenTargetAngle = false
        lastException = AssemblyException.read(tag)
        if (tag.contains(ClockworkConstants.Nbt.SHIPTRAPTION_ID)) {
            shiptraptionID = tag.getLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID)
        }
        if (tag.contains(ORIGINAL_DIRECTION)) {
            originalDirection = Direction.entries[tag.getInt(ORIGINAL_DIRECTION)]
        }
        if (isRunning) {
            if (shiptraptionID == NO_SHIPTRAPTION_ID) {
                clientAngleDiff = AngleHelper.getShortestAngleDiff(angleBefore.toDouble(), targetAngle.toDouble())
                targetAngle = angleBefore
                targetAngleUnwrapped = targetAngle.toDouble()
            }
        } else {
            shiptraptionID = NO_SHIPTRAPTION_ID
        }
        sequencedAngleLimit = tag.getFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_LIMIT)
        sequencedAngleProgress = tag.getFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_PROGRESS)

        bearingPos = tag.getVector3d("bearingPos") ?: Vector3d()
        bearingAxis = tag.getVector3d("bearingAxis") ?: (originalDirection?.normal?.toJOMLD() ?: Vector3d(0.0, 1.0, 0.0))
        aligning = tag.getBoolean("aligning")
        physTargetAngle = if (aligning) 0.0 else targetAngleUnwrapped
        physAligning = aligning
        mainShipId = if (tag.contains("mainShipId")) tag.getLong("mainShipId") else UNKNOWN_MAIN_SHIP_ID
        unresolvedMainShipTicks = 0
        driveWarmupTicks = if (isRunning) REVOLUTE_DRIVE_WARMUP_TICKS else 0
        lastFixedJointMainRot = null
        followAngleSmoothInitialized = false
        joint = null
        jointID = if (isRunning && tag.contains("jointID")) tag.getInt("jointID") else -1

        super.read(tag, clientPacket)
        movementMode?.let {
            it.value = it.value.coerceIn(0, PhysBearingRotationMode.entries.size - 1)
        }
        if (clientPacket) {return}
        shouldVerifyConnection = isRunning
    }

    override fun getInterpolatedAngle(partialTicks: Float): Float {
        var partialTicks = partialTicks
        if (isVirtual) return Mth.lerp(partialTicks + .5f, prevAngle, targetAngle)
        if (shiptraptionID == NO_SHIPTRAPTION_ID || !isRunning) partialTicks = 0f
        return Mth.lerp(partialTicks, targetAngle, targetAngle + angularSpeed)
    }

    fun getWingRotOffset(): Float = when {
         isRunning && open -> openProgressMax.toDouble().toFloat()
         isRunning         -> Mth.lerp(openProgress.toDouble(), 0.0, openProgressMax.toDouble()).toFloat()
        !isRunning && open -> Mth.lerp(openProgress.toDouble(), 1.0, openProgressMax.toDouble()).toFloat()
        else -> 0.0f
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

        val hingeOrientation: Quaterniond = rotationQuaternion.mul(
            Quaterniond(AxisAngle4d(Math.toRadians(90.0), 0.0, 0.0, 1.0)),
            Quaterniond()
        ).normalize()

        return hingeOrientation
    }

    private fun quaternionAroundAxis(axis: Vector3dc, angleRad: Double): Quaterniond {
        val half = angleRad * 0.5
        val s = sin(half)
        return Quaterniond(
            axis.x() * s,
            axis.y() * s,
            axis.z() * s,
            org.joml.Math.cosFromSin(s, half),
        )
    }

    private fun smoothFollowAngleTarget(targetAngleRad: Double, dtSeconds: Double, rotationApplied: Boolean): Double {
        if (!followAngleSmoothInitialized) {
            followAngleSmoothInitialized = true
            followAngleSmoothFromAngleRad = targetAngleRad
            followAngleSmoothToAngleRad = targetAngleRad
            followAngleSmoothCurrentAngleRad = targetAngleRad
            followAngleSmoothTimeSeconds = GAME_TICK_SECONDS
            return targetAngleRad
        }

        if (!rotationApplied) {
            followAngleSmoothFromAngleRad = targetAngleRad
            followAngleSmoothToAngleRad = targetAngleRad
            followAngleSmoothCurrentAngleRad = targetAngleRad
            followAngleSmoothTimeSeconds = GAME_TICK_SECONDS
            return targetAngleRad
        }

        if (targetAngleRad != followAngleSmoothToAngleRad) {
            followAngleSmoothFromAngleRad = followAngleSmoothCurrentAngleRad
            followAngleSmoothToAngleRad = targetAngleRad
            followAngleSmoothTimeSeconds = 0.0
        }

        followAngleSmoothTimeSeconds = min(GAME_TICK_SECONDS, followAngleSmoothTimeSeconds + dtSeconds)
        val alpha = if (GAME_TICK_SECONDS <= 1e-9) 1.0 else followAngleSmoothTimeSeconds / GAME_TICK_SECONDS
        followAngleSmoothCurrentAngleRad =
            followAngleSmoothFromAngleRad + (followAngleSmoothToAngleRad - followAngleSmoothFromAngleRad) * alpha
        return followAngleSmoothCurrentAngleRad
    }

    private data class JointRemovalSnapshot(
        val subShipId: Long,
        val mainShipId: Long?,
        val mainShipIdKnown: Boolean,
        val rotationsKnown: Boolean,
        val subPos: Vector3d,
        val subRot: Quaterniond,
        val mainPos: Vector3d,
        val mainRot: Quaterniond,
    )

    private fun scheduleJointRemoval(level: ServerLevel) {
        val id = jointID
        if (id == -1) return

        val snapshot = computeExpectedJointSnapshot()

        // Clear immediately so this BE can never delete an unrelated joint later via a stale ID.
        jointID = -1
        joint = null
        shouldVerifyConnection = false

        if (snapshot == null) return

        ClockworkMod.physTickOnce(level.dimensionId) { physLevel, _, _ ->
            val vsiPhysLevel = physLevel as? VsiPhysLevel ?: return@physTickOnce
            val existing = vsiPhysLevel.getJointById(id) ?: return@physTickOnce
            if (jointMatchesSnapshot(existing, snapshot)) {
                vsiPhysLevel.removeJoint(id)
            }
        }
    }

    private fun computeExpectedJointSnapshot(): JointRemovalSnapshot? {
        val subShipId = shiptraptionID.takeIf { it != NO_SHIPTRAPTION_ID } ?: return null
        val (expectedMainShipId, expectedMainShipIdKnown) = when (mainShipId) {
            UNKNOWN_MAIN_SHIP_ID -> null to false
            NO_SHIPTRAPTION_ID -> null to true
            else -> mainShipId to true
        }

        val currentJoint = joint
        if (currentJoint is VSRevoluteJoint || currentJoint is VSFixedJoint) {
            val pose0: VSJointPose
            val pose1: VSJointPose
            when (currentJoint) {
                is VSRevoluteJoint -> {
                    pose0 = currentJoint.pose0
                    pose1 = currentJoint.pose1
                }
                is VSFixedJoint -> {
                    pose0 = currentJoint.pose0
                    pose1 = currentJoint.pose1
                }
                else -> throw AssertionError()
            }
            return JointRemovalSnapshot(
                subShipId = subShipId,
                mainShipId = expectedMainShipId,
                mainShipIdKnown = expectedMainShipIdKnown,
                rotationsKnown = true,
                subPos = Vector3d(pose0.pos),
                subRot = Quaterniond(pose0.rot),
                mainPos = Vector3d(pose1.pos),
                mainRot = Quaterniond(pose1.rot),
            )
        }

        val direction = originalDirection
            ?: (blockState.block as? BearingBlock)?.let { blockState.getValue(BearingBlock.FACING) }
            ?: return null
        val axis = bearingAxis
        val hingeRot = getHingeRotation(direction)

        return JointRemovalSnapshot(
            subShipId = subShipId,
            mainShipId = expectedMainShipId,
            mainShipIdKnown = expectedMainShipIdKnown,
            rotationsKnown = false,
            subPos = Vector3d(bearingPos).fma(0.5, axis, Vector3d()),
            subRot = Quaterniond(hingeRot),
            mainPos = Vector3d(worldPosition.center.toJOML()).fma(0.5, axis, Vector3d()),
            mainRot = Quaterniond(hingeRot),
        )
    }

    private data class JointInfo(
        val shipId0: Long?,
        val shipId1: Long?,
        val pos0: Vector3d,
        val rot0: Quaterniond,
        val pos1: Vector3d,
        val rot1: Quaterniond,
    )

    private fun jointMatchesSnapshot(joint: VSJoint, expected: JointRemovalSnapshot): Boolean {
        val actual = when (joint) {
            is VSRevoluteJoint -> JointInfo(
                shipId0 = joint.shipId0,
                shipId1 = joint.shipId1,
                pos0 = Vector3d(joint.pose0.pos),
                rot0 = Quaterniond(joint.pose0.rot),
                pos1 = Vector3d(joint.pose1.pos),
                rot1 = Quaterniond(joint.pose1.rot),
            )
            is VSFixedJoint -> JointInfo(
                shipId0 = joint.shipId0,
                shipId1 = joint.shipId1,
                pos0 = Vector3d(joint.pose0.pos),
                rot0 = Quaterniond(joint.pose0.rot),
                pos1 = Vector3d(joint.pose1.pos),
                rot1 = Quaterniond(joint.pose1.rot),
            )
            else -> return false
        }

        return jointInfoMatchesSnapshot(
            subShipIdCandidate = actual.shipId0,
            mainShipIdCandidate = actual.shipId1,
            subPosCandidate = actual.pos0,
            subRotCandidate = actual.rot0,
            mainPosCandidate = actual.pos1,
            mainRotCandidate = actual.rot1,
            expected = expected,
        ) || jointInfoMatchesSnapshot(
            subShipIdCandidate = actual.shipId1,
            mainShipIdCandidate = actual.shipId0,
            subPosCandidate = actual.pos1,
            subRotCandidate = actual.rot1,
            mainPosCandidate = actual.pos0,
            mainRotCandidate = actual.rot0,
            expected = expected,
        )
    }

    private fun jointInfoMatchesSnapshot(
        subShipIdCandidate: Long?,
        mainShipIdCandidate: Long?,
        subPosCandidate: Vector3d,
        subRotCandidate: Quaterniond,
        mainPosCandidate: Vector3d,
        mainRotCandidate: Quaterniond,
        expected: JointRemovalSnapshot,
    ): Boolean {
        if (subShipIdCandidate != expected.subShipId) return false
        if (expected.mainShipIdKnown && mainShipIdCandidate != expected.mainShipId) return false

        if (!positionsMatch(subPosCandidate, expected.subPos)) return false
        if (!positionsMatch(mainPosCandidate, expected.mainPos)) return false
        if (expected.rotationsKnown) {
            if (!rotationsMatch(subRotCandidate, expected.subRot)) return false
            if (!rotationsMatch(mainRotCandidate, expected.mainRot)) return false
        }
        return true
    }

    private fun positionsMatch(a: Vector3d, b: Vector3d): Boolean =
        a.distanceSquared(b) <= JOINT_MATCH_POS_TOLERANCE * JOINT_MATCH_POS_TOLERANCE

    private fun rotationsMatch(a: Quaterniond, b: Quaterniond): Boolean =
        abs(a.dot(b)) >= (1.0 - JOINT_MATCH_ROT_DOT_TOLERANCE)

    private fun assemble() {
        if (level!!.getBlockState(worldPosition).block !is BearingBlock) return
        val level = level as ServerLevel

        originalDirection = blockState.getValue(BearingBlock.FACING)
        val direction = originalDirection!!
        val attachPoint = worldPosition.relative(direction)

        // bearing data
        val worldPos: Vector3dc = worldPosition.center.toJOML()
        val axis = direction.normal.toJOMLD()
        val shipOn = level.getShipObjectManagingPos(worldPosition)

        val startPos = worldPos + axis * 0.5
        val endPos = worldPos + axis * 1.5

        fun Vector3d.toVec3() = this.let { net.minecraft.world.phys.Vec3(it.x, it.y, it.z) }

        val otherPos = level.clipIncludeShips(
            ClipContext(
                (shipOn?.transform?.shipToWorld?.transformPosition(startPos) ?: startPos).toVec3(),
                (shipOn?.transform?.shipToWorld?.transformPosition(endPos) ?: endPos).toVec3(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
                ), false, shipOn?.id)

        val otherShip = level.getShipObjectManagingPos(otherPos.blockPos)
        val posInOwnerShip = Vector3d(worldPos)

        val (bearingPos, shiptraption, otherDirection) = if (otherShip == null) {
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

            //TODO this is dumb, but i forgot to make assembly return center positions, oh well
            val event = VSAssemblyEvents.onPasteBeforeBlocksAreLoaded.on {
                centerPositions = it.centerPosition.first.get(Vector3d()) to it.centerPosition.second.get(Vector3d())
            }
            val shiptraption = assembleToShip(
                level,
                selection.toSet().map { it.toMinecraft() }.toSet(), //accursed, unholy, abominable
                1.0
            )
            event.unregister()

            val newPos = Vector3d(worldPos).sub(centerPositions.first).add(centerPositions.second)

            shiptraptionID = shiptraption.id
            Triple(newPos, shiptraption, direction)
        } else {
            shiptraptionID = otherShip.id
            Triple(otherPos.blockPos.toVector3d() + 0.5 - direction.normal.toJOMLD(), otherShip, otherPos.direction)
        }


        // AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);
        ClockworkSounds.PHYSICS_INFUSER_LIGHTNING.playOnServer(level, worldPosition)

        val posInWorld = shipOn?.transform?.shipToWorld?.transformPosition(
            posInOwnerShip - bearingPos + shiptraption.inertiaData.centerOfMass , Vector3d()
        ) ?: (worldPos - bearingPos + shiptraption.inertiaData.centerOfMass)
        val rotInWorld = shipOn?.transform?.shipToWorldRotation ?: Quaterniond()
        val scaling    = shipOn?.transform?.shipToWorldScaling ?: Vector3d(1.0, 1.0, 1.0)

        shiptraption.unsafeSetTransform(BodyTransformFactory.create(
            posInWorld, rotInWorld, scaling, shiptraption.transform.positionInModel
        ))

        this.bearingAxis = axis
        this.bearingPos = bearingPos
        this.mainShipId = shipOn?.id ?: NO_SHIPTRAPTION_ID
        this.unresolvedMainShipTicks = 0
        this.driveWarmupTicks = 0
        this.joint = null
        this.jointID = -1
        this.shouldVerifyConnection = true
        this.isRunning = true
        this.lastStateChanged = ticks

        sendData()
        updateGeneratedRotation()
    }

    override fun destroy() {
        val level = level ?: return
        if (level.isClientSide || level !is ServerLevel) return
        scheduleJointRemoval(level)
    }

    fun disassemble() {
        if (!isRunning && shiptraptionID == NO_SHIPTRAPTION_ID) return
        if (ticks - lastStateChanged <= cooldown) return
        targetAngle = 0f
        targetAngleUnwrapped = 0.0
        if (shiptraptionID == NO_SHIPTRAPTION_ID) return
        val level = level as ServerLevel
        val ship = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return resetState()

        if (!canDisassemble(bearingAxis, ship, level.getShipObjectManagingPos(worldPosition))) {
            disassembleWhenPossible = !disassembleWhenPossible
            aligning = !aligning
        } else {
            shipDisassemble()
        }
        AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(level, worldPosition)
    }

    private fun shipDisassemble() {
        if (shiptraptionID == NO_SHIPTRAPTION_ID || level!!.isClientSide) { return }
        val level = level as ServerLevel
        val subShip = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return
        val mainShip = level.getShipObjectManagingPos(worldPosition)

        if (!canDisassemble(bearingAxis, subShip, mainShip)) { return }
        val direction = originalDirection ?: blockState.getValue(BearingBlock.FACING)
        val inMain = worldPosition.relative(direction, 1)
        val inSubship = bearingPos.add(bearingAxis, Vector3d()).let { BlockPos.containing(it.x, it.y, it.z) }

        //todo this is stupid
        val aabb = subShip.shipAABB!!
        val blocks = DenseBlockPosSet()
        for (x in aabb.minX() - 1 until  aabb.maxX() + 1) {
        for (z in aabb.minZ() - 1 until  aabb.maxZ() + 1) {
        for (y in aabb.minY() - 1 until  aabb.maxY() + 1) {
            blocks.add(x, y, z)
        } } }

        val subCouldSplit = subShip.getAttachment<SplittingDisablerAttachment>()?.let { if (it.canSplit()) { it.disableSplitting(); true } else {false} } ?: false
        val mainCouldSplit = mainShip?.getAttachment<SplittingDisablerAttachment>()?.let { if (it.canSplit()) { it.disableSplitting(); true } else {false} } ?: false

        val hasMoved = PhysBearingAssembler.moveBlocksFromTo(level, blocks, true, inSubship, inMain, subShip, mainShip)

        if (subCouldSplit) { subShip.getAttachment<SplittingDisablerAttachment>()?.enableSplitting() }
        if (mainCouldSplit) { mainShip?.getAttachment<SplittingDisablerAttachment>()?.enableSplitting() }

        if (!hasMoved) {
            aligning = false
            assembleNextTick = false
            disassembleWhenPossible = false
            return
        }
        lastStateChanged = ticks
        resetState()
    }

    private fun resetState() {
        val level = level as? ServerLevel
        if (level != null) {
            scheduleJointRemoval(level)
        } else {
            jointID = -1
            joint = null
        }
        shiptraptionID = NO_SHIPTRAPTION_ID
        mainShipId = UNKNOWN_MAIN_SHIP_ID
        unresolvedMainShipTicks = 0
        driveWarmupTicks = 0
        isRunning = false
        updateGeneratedRotation()
        assembleNextTick = false
        disassembleWhenPossible = false
        sequencedAngleLimit = -1.0f
        sequencedAngleProgress = 0f
        targetAngle = 0f
        targetAngleUnwrapped = 0.0
        isSpeedDrivenTargetAngle = false
        physTargetAngle = 0.0
        lastFixedJointMainRot = null
        followAngleSmoothInitialized = false
        sendData()
        joint = null
        shouldVerifyConnection = false
        aligning = false
    }

    private fun tryAssembleNextTick() {
        if (!assembleNextTick) {return}
        if (ticks - lastStateChanged <= cooldown) {return}
        assembleNextTick = false
        if (!isRunning) {assemble()}
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

        if (open && !isRunning && openProgress > 0.0f) {
            openProgress -= 0.05f
        } else if (openProgress <= 0.0f) {
            open = false
            openProgress = 0.0f
        }
    }

    fun getActualAngularSpeed(): Float {
        val dir = originalDirection ?: blockState.getValue(BearingBlock.FACING)
        return convertToAngular(getSpeed()) * if (dir == Direction.WEST || dir == Direction.NORTH || dir == Direction.DOWN) 1 else -1
    }

    fun getRealisticAngularSpeed(): Float {
        val dir = originalDirection ?: blockState.getValue(BearingBlock.FACING)
        return getSpeed() * 2f * PI.toFloat() / 60f * if (dir == Direction.WEST || dir == Direction.NORTH || dir == Direction.DOWN) 1 else -1
    }

    private fun wrapAngle720(unwrappedDegrees: Double): Float {
        val range = 360.0 * 2.0
        var wrapped = unwrappedDegrees % range
        if (wrapped < 0.0) wrapped += range
        return wrapped.toFloat()
    }

    private fun getActualAngleUnwrappedDegrees(): Double? {
        val actualRad = getActualAngle() ?: return null
        val wrappedDeg = Math.toDegrees(actualRad)
        return wrappedDeg + 360.0 * Math.round((targetAngleUnwrapped - wrappedDeg) / 360.0)
    }

    override fun tick() {
        super.tick()
        prevAngle = targetAngle
        ticks++
        if (level!!.isClientSide) clientAngleDiff /= 2f
        val mode = movementMode?.get() ?: PhysBearingRotationMode.UNLOCKED
        if (!level!!.isClientSide) {
            if (isRunning && originalDirection == null && level!!.getBlockState(worldPosition).block is BearingBlock) {
                originalDirection = blockState.getValue(BearingBlock.FACING)
            }
            if (isRunning && shiptraptionID != NO_SHIPTRAPTION_ID) {
                // If the connected ship was removed (not just unloaded), close back up.
                if ((level as ServerLevel).shipObjectWorld.allShips.getById(shiptraptionID) == null) {
                    resetState()
                }
            }
            if (isRunning && mainShipId == UNKNOWN_MAIN_SHIP_ID) {
                val resolved = (level as ServerLevel).getShipManagingPos(worldPosition)?.id
                if (resolved != null) {
                    mainShipId = resolved
                    unresolvedMainShipTicks = 0
                } else if (++unresolvedMainShipTicks > 200) {
                    // After a short grace period, assume this is world-attached.
                    mainShipId = NO_SHIPTRAPTION_ID
                    unresolvedMainShipTicks = 0
                }
            }
            if (lastMode != mode && mode == PhysBearingRotationMode.FOLLOW_ANGLE) {
                val shipOn = level!!.getShipObjectManagingPos(blockPos)?.transform
                val shiptraption = level!!.shipObjectWorld.allShips.getById(shiptraptionID)?.transform
                if (shiptraption != null) {
                    val angleDeg = Math.toDegrees(getAngle(bearingAxis, shiptraption, shipOn))
                    targetAngleUnwrapped = angleDeg
                    targetAngle = wrapAngle720(angleDeg)
                }
            }
            lastMode = mode
            tryAssembleNextTick()
            if (disassembleWhenPossible) { shipDisassemble() }
        }
        tickAnimationLogic()
        if (!isRunning) {
            if (!level!!.isClientSide) {
                desiredModeOrdinal = mode.ordinal
                desiredAngularVelocity = 0.0f
                physTargetAngle = 0.0
                physAligning = aligning
            }
            return
        }

        var angleDeltaDeg = 0.0
        if (shiptraptionID == NO_SHIPTRAPTION_ID) {
            targetAngle = 0f
            targetAngleUnwrapped = 0.0
        } else if (jointID != -1) {
            val angularSpeed = -getActualAngularSpeed()
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
            angleDeltaDeg = (angularSpeed - diff).toDouble()

            if (mode == PhysBearingRotationMode.FOLLOW_ANGLE && !aligning && !level!!.isClientSide) {
                val actualAngleDeg = getActualAngleUnwrappedDegrees()
                if (actualAngleDeg != null) {
                    if (abs(angleDeltaDeg) <= 1e-7) {
                        if (isSpeedDrivenTargetAngle) {
                            // When we stop providing rotation input, lock to the current angle (prevents "bounce back").
                            targetAngleUnwrapped = actualAngleDeg
                            targetAngle = wrapAngle720(targetAngleUnwrapped)
                        }
                        isSpeedDrivenTargetAngle = false
                    } else {
                        isSpeedDrivenTargetAngle = true
                        targetAngleUnwrapped += angleDeltaDeg
                        // Prevent target wind-up when rotation is blocked by collisions.
                        val maxErrorDeg = max(FOLLOW_ANGLE_TARGET_ERROR_MIN_DEG, abs(angleDeltaDeg))
                        targetAngleUnwrapped = actualAngleDeg +
                            (targetAngleUnwrapped - actualAngleDeg).coerceIn(-maxErrorDeg, maxErrorDeg)
                        targetAngle = wrapAngle720(targetAngleUnwrapped)
                    }
                } else {
                    // If the ship isn't loaded yet, avoid target wind-up; we'll snap on the first tick we can read angle.
                    isSpeedDrivenTargetAngle = abs(angleDeltaDeg) > 1e-7
                }
            } else {
                isSpeedDrivenTargetAngle = false
                targetAngleUnwrapped += angleDeltaDeg
                targetAngle = wrapAngle720(targetAngleUnwrapped)
            }
        }

        if (!level!!.isClientSide) {
            desiredModeOrdinal = mode.ordinal
            desiredAngularVelocity = if (!aligning && abs(getSpeed()) > 0.0f) {
                getRealisticAngularSpeed()
            } else {
                0.0f
            }
            physAligning = aligning
            physTargetAngle = if (aligning) 0.0 else targetAngleUnwrapped
        }
    }

    override fun onSpeedChanged(previousSpeed: Float) {
        sequencedAngleLimit = -1.0f
        sequencedAngleProgress = 0.0f

        if (sequenceContext != null && sequenceContext.instruction == SequencerInstructions.TURN_ANGLE) {
            sequencedAngleLimit = sequenceContext.getEffectiveValue(theoreticalSpeed.toDouble()).toFloat()
        }
        super.onSpeedChanged(previousSpeed)
    }

    override fun lazyTick() {
        super.lazyTick()
        if (shiptraptionID != NO_SHIPTRAPTION_ID && !level!!.isClientSide) sendData()
    }

    override fun addToTooltip(tooltip: List<Component>, isPlayerSneaking: Boolean): Boolean {
        if (super.addToTooltip(tooltip, isPlayerSneaking)) return true
        if (isPlayerSneaking) return false
        if (getSpeed() == 0f) return false
        if (isRunning) return false
        if (blockState.block !is BearingBlock) return false
        val attachedState = level!!.getBlockState(worldPosition.relative(blockState.getValue(BearingBlock.FACING)))
        if (attachedState.canBeReplaced()) return false
        TooltipHelper.addHint(tooltip, "hint.empty_bearing")
        return true
    }

    fun getActualAngle(): Double? {
        val serverLevel = level as? ServerLevel ?: return null
        val shiptraption = serverLevel.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return null
        val mainShip = serverLevel.getShipManagingPos(worldPosition)
        return getAngle(bearingAxis, shiptraption.transform, mainShip?.transform)
    }

    override fun attach(contraption: ControlledContraptionEntity) {}
    override fun onStall() { if (!level!!.isClientSide) sendData() }
    override fun isValid(): Boolean = !isRemoved
    override fun isAttachedTo(contraption: AbstractContraptionEntity): Boolean = false
    override fun setAngle(forcedAngle: Float) {
        targetAngle = forcedAngle
        targetAngleUnwrapped = forcedAngle.toDouble()
    }
    override fun getLastAssemblyException(): AssemblyException? = lastException
    override fun getBlockPosition(): BlockPos = worldPosition
    override fun isWoodenTop(): Boolean = false

    companion object {
        const val NO_SHIPTRAPTION_ID: Long = -1
        private const val UNKNOWN_MAIN_SHIP_ID: Long = Long.MIN_VALUE
        private const val REVOLUTE_DRIVE_WARMUP_TICKS: Int = 5
        private const val FOLLOW_ANGLE_TARGET_ERROR_MIN_DEG: Double = 0.5
        private const val MAX_MOTOR_ACCEL: Double = 200.0
        private const val MAX_MOTOR_DELTA_OMEGA: Double = 1.0
        private const val MAX_APPLIED_TORQUE_MAG: Double = 1.0E9
        private const val GAME_TICK_SECONDS: Double = 1.0 / 20.0
        private const val FIXED_JOINT_MAX_FORCE: Float = 1.0E10F
        private const val FIXED_JOINT_COMPLIANCE: Double = 1e-12
        private const val FIXED_JOINT_MAX_ANG_ACCEL_RAD_S2: Double = 10_000.0
        private const val FIXED_JOINT_MIN_TORQUE: Double = 1.0E6
        private const val FIXED_JOINT_MAX_TORQUE_CAP: Double = 1.0E12
        private const val JOINT_MATCH_POS_TOLERANCE: Double = 1e-3
        private const val JOINT_MATCH_ROT_DOT_TOLERANCE: Double = 1e-5

        //tolerance is in degrees
        @JvmStatic
        fun canDisassemble(bearingAxis: Vector3d, mainShip: ServerShip, otherShip: ServerShip?, tolerance: Int=5): Boolean {
            if (abs(Math.toDegrees(getAngle(bearingAxis, mainShip.transform, otherShip?.transform))) > tolerance) return false
            return true
        }
    }

    private fun motorAccelCap(dtSeconds: Double, maxDeltaOmega: Double = MAX_MOTOR_DELTA_OMEGA): Double {
        if (dtSeconds <= 1e-6) return MAX_MOTOR_ACCEL
        return min(MAX_MOTOR_ACCEL, maxDeltaOmega / dtSeconds)
    }

    private fun computeUnlockedTorque(
        subShip: PhysShip,
        mainShip: PhysShip?,
        bearingAxis: Vector3dc,
        desiredAngularVelocity: Float,
        subLocalPos: Vector3dc,
        mainLocalPos: Vector3dc,
        dtSeconds: Double,
    ): Vector3d {
        // Axis in world space
        val axisGlobal = Vector3d(bearingAxis)
        mainShip?.transform?.shipToWorldRotation?.transform(axisGlobal)

        val omegaTarget = -desiredAngularVelocity.toDouble()
        val omegaRelative = Vector3d(subShip.angularVelocity).also { rel ->
            if (mainShip != null && !mainShip.isStatic) rel.sub(mainShip.angularVelocity)
        }
        val omegaActual = axisGlobal.dot(omegaRelative)
        val omegaError = omegaTarget - omegaActual * ClockworkConfig.SERVER.unlockedModeRotationResistanceMultiplier

        val effectiveInertia = getEffectiveAngularInertia(subShip, mainShip, axisGlobal, subLocalPos, mainLocalPos)
        if (effectiveInertia <= 0.0) return Vector3d()

        val accelCmd = (omegaError * ClockworkConfig.SERVER.unlockedModeOmegaErrorMultiplier)
            .coerceIn(-motorAccelCap(dtSeconds), motorAccelCap(dtSeconds))
        val torqueMag = effectiveInertia * accelCmd
        return axisGlobal.mul(torqueMag, Vector3d())
    }

    private fun getEffectiveAngularInertia(
        subShip: PhysShip,
        mainShip: PhysShip?,
        axisGlobal: Vector3dc,
        subLocalPos: Vector3dc,
        mainLocalPos: Vector3dc,
    ): Double {
        fun angularInertia(ship: PhysShip, localPos: Vector3dc): Double {
            val globalPos: Vector3dc = ship.transform.shipToWorld.transformPosition(localPos, Vector3d())
            val offset: Vector3dc = globalPos.sub(ship.transform.positionInWorld, Vector3d())
            val offsetPerpToAxis: Vector3dc = offset.sub(axisGlobal.mul(axisGlobal.dot(offset), Vector3d()), Vector3d())
            val axisLocal: Vector3dc = ship.transform.shipToWorldRotation.transformInverse(axisGlobal, Vector3d())
            return ship.momentOfInertia.transform(axisLocal, Vector3d()).dot(axisLocal) + offsetPerpToAxis.lengthSquared() * ship.mass
        }

        fun parallelOperator(left: Double, right: Double): Double = 1.0 / (1.0 / left + 1.0 / right)

        return when {
            !subShip.isStatic && mainShip != null && !mainShip.isStatic -> parallelOperator(
                angularInertia(subShip, subLocalPos),
                angularInertia(mainShip, mainLocalPos),
            )
            !subShip.isStatic -> angularInertia(subShip, subLocalPos)
            mainShip != null && !mainShip.isStatic -> angularInertia(mainShip, mainLocalPos)
            else -> 0.0
        }
    }
}
