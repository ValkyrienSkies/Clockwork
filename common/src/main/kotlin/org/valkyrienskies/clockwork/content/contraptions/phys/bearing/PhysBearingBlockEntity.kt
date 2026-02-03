package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllSoundEvents
import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import com.simibubi.create.foundation.item.TooltipHelper
import com.simibubi.create.foundation.utility.ServerSpeedProvider
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.joml.*
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingData
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingUpdateData
import org.valkyrienskies.clockwork.content.forces.contraption.BearingController
import org.valkyrienskies.clockwork.content.forces.contraption.BearingController.Companion.getAngle
import org.valkyrienskies.clockwork.platform.api.ContraptionController
import org.valkyrienskies.clockwork.platform.api.ContraptionController.LockedMode
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.ClockworkConstants.Nbt.ORIGINAL_DIRECTION
import org.valkyrienskies.clockwork.util.ClockworkUtils.getVector3d
import org.valkyrienskies.clockwork.util.GlueAssembler.collectGlued
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.updateJoint
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

import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil
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
    var movementMode: ScrollOptionBehaviour<LockedMode>? = null
    var isRunning = false
        private set
    var shiptraptionID = NO_SHIPTRAPTION_ID
        private set
    @Volatile var targetAngle = 0f
        get() = field
        private set(idk) {field = idk}
    var disassembleWhenPossible = false
        private set
    @Volatile var joint : VSJoint? = null
        private set
    @Volatile var jointID : Int = -1
        private set

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
    @Volatile private var aligning = false
    private var bearingAxis: Vector3d = Vector3d()
    private var bearingID: Int = -1

    private var lastSpeed = 0f
    private var lastMode = LockedMode.UNLOCKED
    private var lastAligningState = false
    private var missingSubShipTicks = 0

    private var followAngleStalled = false
    private var followAngleStallTicks = 0
    private var followAngleStallDirSign = 0
    private var lastActualAngleRadForStall: Double? = null

    // Phys-thread reads these; game-thread writes them.
    @Volatile private var servoMode: LockedMode = LockedMode.UNLOCKED
    @Volatile private var lockedHoldAngleRad: Double? = null
    @Volatile private var followOmegaFeedForwardRadSec: Double = 0.0

    private var servoStrengthBehaviour: ServoStrengthScrollValueBehaviour? = null
    private var servoStiffnessBehaviour: ServoStiffnessScrollValueBehaviour? = null

    @Volatile private var servoStrengthSetting: Int = SERVO_STRENGTH_DEFAULT
    @Volatile private var servoStiffnessSetting: Int = SERVO_STIFFNESS_DEFAULT

    // Derived (phys-thread) servo parameters.
    @Volatile private var servoKp: Double = SERVO_KP
    @Volatile private var servoKd: Double = SERVO_KD
    @Volatile private var servoMaxAlpha: Double = SERVO_MAX_ALPHA
    @Volatile private var servoTorqueLimit: Double = SERVO_MAX_TORQUE.toDouble()

    private var controllerCreationData: PhysBearingData? = null
    private var controllerUpdateData: PhysBearingUpdateData? = null
    private var loadingFn: ((ServerLevel) -> Unit)? = null

    init {
        setLazyTickRate(3)
        recomputeServoTuning()
    }

    private fun movementModeChanged(value: Int) {
        if (level == null || level!!.isClientSide) {return}
        // Immediately switch joint behavior when the player changes modes.
        tryUpdateData()
        sendData()
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        movementMode = ScrollOptionBehaviour(
            LockedMode::class.java, Component.translatable("$MOD_ID.phys_bearing.rotation_mode"),
            this, movementModeSlot
        )
        movementMode!!.withCallback{movementModeChanged(it)}
        movementMode!!.requiresWrench()
        behaviours.add(movementMode!!)

        servoStrengthBehaviour = ServoStrengthScrollValueBehaviour(
            Component.translatable("$MOD_ID.phys_bearing.servo_strength"),
            this,
            ServoSettingValueBoxTransform(8.0, 4.0)
        ).also {
            it.between(SERVO_STRENGTH_MIN, SERVO_STRENGTH_MAX)
            it.currentValue = servoStrengthSetting
            it.withCallback { v -> setServoStrengthSetting(v, sendUpdate = true) }
            it.requiresWrench()
            behaviours.add(it)
        }

        servoStiffnessBehaviour = ServoStiffnessScrollValueBehaviour(
            Component.translatable("$MOD_ID.phys_bearing.servo_stiffness"),
            this,
            ServoSettingValueBoxTransform(8.0, 12.0)
        ).also {
            it.between(SERVO_STIFFNESS_MIN, SERVO_STIFFNESS_MAX)
            it.currentValue = servoStiffnessSetting
            it.withCallback { v -> setServoStiffnessSetting(v, sendUpdate = true) }
            it.requiresWrench()
            behaviours.add(it)
        }
    }

    private fun setServoStrengthSetting(value: Int, sendUpdate: Boolean) {
        val clamped = value.coerceIn(SERVO_STRENGTH_MIN, SERVO_STRENGTH_MAX)
        if (servoStrengthSetting == clamped) return
        servoStrengthSetting = clamped
        recomputeServoTuning()
        if (sendUpdate && level != null && !level!!.isClientSide) {
            updateDrive()
            sendData()
        }
    }

    private fun setServoStiffnessSetting(value: Int, sendUpdate: Boolean) {
        val clamped = value.coerceIn(SERVO_STIFFNESS_MIN, SERVO_STIFFNESS_MAX)
        if (servoStiffnessSetting == clamped) return
        servoStiffnessSetting = clamped
        recomputeServoTuning()
        if (sendUpdate && level != null && !level!!.isClientSide) {
            updateDrive()
            sendData()
        }
    }

    private fun lerpLog(min: Double, max: Double, t: Double): Double {
        if (min <= 0.0 || max <= 0.0) return min + (max - min) * t
        val lnMin = ln(min)
        val lnMax = ln(max)
        return exp(lnMin + (lnMax - lnMin) * t)
    }

    private fun recomputeServoTuning() {
        val strengthT = servoStrengthSetting.toDouble() / SERVO_STRENGTH_MAX.toDouble()
        val stiffnessT = servoStiffnessSetting.toDouble() / SERVO_STIFFNESS_MAX.toDouble()

        val wn = lerpLog(SERVO_WN_MIN, SERVO_WN_MAX, stiffnessT.coerceIn(0.0, 1.0))
        val testScale = SERVO_TEST_SCALE
        servoKp = wn * wn * testScale
        servoKd = 2.0 * SERVO_DAMPING_RATIO * wn * testScale

        servoMaxAlpha = lerpLog(SERVO_ALPHA_MIN, SERVO_ALPHA_MAX, strengthT.coerceIn(0.0, 1.0)) * testScale
        servoTorqueLimit = lerpLog(SERVO_FORCE_TORQUE_MIN, SERVO_FORCE_TORQUE_MAX, strengthT.coerceIn(0.0, 1.0)) * testScale
    }

    private fun updateDrive() {
        val level = level as? ServerLevel ?: return
        val existing = joint ?: return
        val mode = movementMode?.get() ?: LockedMode.UNLOCKED

        // Keep the joint REVOLUTE in all modes; servo is implemented via driveVelocity in physTick.
        val revolute = when (existing) {
            is VSRevoluteJoint -> existing
            is VSFixedJoint -> VSRevoluteJoint(
                existing.shipId0, existing.pose0, existing.shipId1, existing.pose1,
                maxForceTorque = VSJointMaxForceTorque(SERVO_MAX_FORCE, SERVO_MAX_TORQUE),
                compliance = SERVO_COMPLIANCE,
                driveFreeSpin = true
            )
            else -> return
        }

        val servoActive = aligning || mode != LockedMode.UNLOCKED
        val baseJoint = if (servoActive) {
            revolute.copy(
                maxForceTorque = VSJointMaxForceTorque(SERVO_MAX_FORCE, SERVO_MAX_TORQUE),
                compliance = SERVO_COMPLIANCE,
                // We don't rely on the revolute motor for servo behavior; see physTick torque servo.
                driveVelocity = null,
                driveForceLimit = null,
                driveGearRatio = null,
                driveFreeSpin = false
            )
        } else {
            revolute.copy(
                maxForceTorque = VSJointMaxForceTorque(SERVO_MAX_FORCE, SERVO_MAX_TORQUE),
                compliance = SERVO_COMPLIANCE,
                driveVelocity = null,
                driveForceLimit = null,
                driveFreeSpin = true
            )
        }

        joint = baseJoint
        servoMode = mode
        // Sign matches tick() targetAngle integration and BearingController's ideal omega convention.
        followOmegaFeedForwardRadSec =
            if (!aligning && mode == LockedMode.FOLLOW_ANGLE && !followAngleStalled) -getRealisticAngularSpeed().toDouble() else 0.0

        controllerUpdateData = PhysBearingUpdateData(
            Math.toRadians(targetAngle.toDouble()),
            if (mode == LockedMode.UNLOCKED && !aligning) getRealisticAngularSpeed() else 0f,
            servoActive
        )

        if (jointID != -1) {
            level.gtpa.updateJoint(jointID, baseJoint)
        }
    }

    @Volatile override lateinit var dimension: DimensionId
    @Volatile private var lastAngle = targetAngle
    @Volatile private var curAngle = targetAngle

    private fun shortestAngleErrorRad(target: Double, current: Double): Double {
        // Returns error in (-pi, pi].
        val d = target - current
        return atan2(sin(d), cos(d))
    }

    private fun normalizeAngleDeg0To720(angleDeg: Double): Float {
        val period = 360.0 * 2.0
        var wrapped = angleDeg % period
        if (wrapped < 0.0) wrapped += period
        return wrapped.toFloat()
    }

    private fun clearFollowAngleStallState() {
        followAngleStalled = false
        followAngleStallTicks = 0
        followAngleStallDirSign = 0
        lastActualAngleRadForStall = null
    }

    private fun updateFollowAngleStallState(cmdSpeedDegPerTick: Float) {
        val level = level as? ServerLevel ?: return
        val mode = movementMode?.get() ?: LockedMode.UNLOCKED
        if (!isRunning || aligning || mode != LockedMode.FOLLOW_ANGLE || shiptraptionID == NO_SHIPTRAPTION_ID) {
            if (followAngleStalled || followAngleStallTicks != 0 || lastActualAngleRadForStall != null) {
                clearFollowAngleStallState()
                updateDrive()
                sendData()
            }
            return
        }

        val cmdMag = abs(cmdSpeedDegPerTick)
        val cmdSign = cmdSpeedDegPerTick.sign.toInt()

        // If the player stops commanding rotation, clear stall so the next input can move again.
        if (cmdMag < FOLLOW_STALL_MIN_CMD_DEG_PER_TICK || cmdSign == 0) {
            if (followAngleStalled) {
                clearFollowAngleStallState()
                if (DEBUG_SERVO) {
                    ClockworkMod.LOGGER.info("[PhysBearing] follow stall cleared (cmd=0)")
                }
                updateDrive()
                sendData()
            } else {
                followAngleStallTicks = 0
                followAngleStallDirSign = 0
                lastActualAngleRadForStall = null
            }
            return
        }

        // If stalled, only clear when the player reverses direction.
        if (followAngleStalled) {
            if (cmdSign != followAngleStallDirSign) {
                clearFollowAngleStallState()
                if (DEBUG_SERVO) {
                    ClockworkMod.LOGGER.info("[PhysBearing] follow stall cleared (dir change)")
                }
                updateDrive()
                sendData()
            }
            return
        }

        val actualRad = getActualAngle() ?: run {
            // Can't observe motion yet; don't accumulate stall ticks.
            followAngleStallTicks = 0
            lastActualAngleRadForStall = null
            return
        }

        val lastRad = lastActualAngleRadForStall
        lastActualAngleRadForStall = actualRad

        // Direction changed => restart stall detection.
        if (followAngleStallDirSign != 0 && cmdSign != followAngleStallDirSign) {
            followAngleStallTicks = 0
        }
        followAngleStallDirSign = cmdSign

        if (lastRad == null) return

        val actualDeltaRad = abs(shortestAngleErrorRad(actualRad, lastRad))
        val cmdDeltaRad = cmdMag.toDouble() * (Math.PI / 180.0) // deg/tick -> rad/tick
        val epsRad = max(FOLLOW_STALL_EPS_RAD_BASE, cmdDeltaRad * FOLLOW_STALL_EPS_FRACTION)

        if (actualDeltaRad < epsRad) {
            followAngleStallTicks++
        } else {
            followAngleStallTicks = 0
        }

        if (followAngleStallTicks < FOLLOW_STALL_TICKS) return

        // Stalled: stop advancing the expected angle and remove omega feed-forward so we stop pushing into collisions.
        followAngleStalled = true
        followAngleStallTicks = 0
        followAngleStallDirSign = cmdSign
        targetAngle = normalizeAngleDeg0To720(Math.toDegrees(actualRad))

        if (DEBUG_SERVO) {
            ClockworkMod.LOGGER.info(
                "[PhysBearing] follow stalled (cmdDegTick={}, actualDeltaRad={}, epsRad={})",
                cmdSpeedDegPerTick,
                actualDeltaRad,
                epsRad
            )
        }

        updateDrive()
        sendData()
    }

    private fun getAngularInertia(physShip: PhysShip, localPos: Vector3dc, axisGlobal: Vector3dc): Double {
        val globalPos: Vector3dc = physShip.transform.shipToWorld.transformPosition(localPos, Vector3d())
        val offset: Vector3dc = globalPos.sub(physShip.transform.positionInWorld, Vector3d())
        return getAngularInertia(
            physShip.momentOfInertia,
            physShip.transform.shipToWorldRotation,
            physShip.mass,
            offset,
            axisGlobal
        )
    }

    private fun getAngularInertia(
        inertiaTensorLocal: Matrix3dc,
        rotation: Quaterniondc,
        mass: Double,
        offsetGlobal: Vector3dc,
        axisGlobal: Vector3dc
    ): Double {
        val offsetPerpToAxis: Vector3dc =
            offsetGlobal.sub(axisGlobal.mul(axisGlobal.dot(offsetGlobal), Vector3d()), Vector3d())
        val axisLocal: Vector3dc = rotation.transformInverse(axisGlobal, Vector3d())
        return inertiaTensorLocal.transform(axisLocal, Vector3d())
            .dot(axisLocal) + offsetPerpToAxis.lengthSquared() * mass
    }

    private fun parallelOperator(left: Double, right: Double): Double {
        return 1.0 / (1.0 / left + 1.0 / right)
    }

    override fun physTick(physShip: PhysShip?, physLevel: PhysLevel) {
        if (isRemoved || !isRunning) return
        val jointId = jointID
        if (jointId == -1) return

        val revolute = joint as? VSRevoluteJoint ?: return
        val mode = servoMode
        val servoActive = aligning || mode != LockedMode.UNLOCKED
        if (!servoActive) return

        // We need both transforms to compute angle; if ships aren't loaded yet, just wait.
        val subShip = revolute.shipId0?.let { physLevel.getShipById(it) } ?: run {
            if (DEBUG_SERVO) ClockworkMod.LOGGER.info("[PhysBearing] physTick: subShip not loaded yet (jointId={}, ship0={})", jointId, revolute.shipId0)
            return
        }
        val mainShip = revolute.shipId1?.let { physLevel.getShipById(it) }

        val currentAngleRad = getAngle(bearingAxis, subShip.transform, mainShip?.transform)
        val targetAngleRad = when {
            aligning -> 0.0
            mode == LockedMode.LOCKED -> lockedHoldAngleRad ?: currentAngleRad.also { lockedHoldAngleRad = it }
            else -> Math.toRadians(targetAngle.toDouble())
        }

        var errorRad = shortestAngleErrorRad(targetAngleRad, currentAngleRad)
        if (abs(errorRad) < SERVO_DEADBAND_RAD) errorRad = 0.0
        val axisWorld = bearingAxis.get(Vector3d())
        mainShip?.transform?.shipToWorldRotation?.transform(axisWorld)

        val relOmegaWorld = Vector3d(subShip.angularVelocity)
        if (mainShip != null) relOmegaWorld.sub(mainShip.angularVelocity)
        val omegaActual = relOmegaWorld.dot(axisWorld) // rad/s along the bearing axis

        // FOLLOW_ANGLE should respond immediately to kinetic input; use a feed-forward target omega and a PD in
        // acceleration-space. This avoids the "infinite torque" behavior that destabilizes heavy/offset ships.
        val omegaFf = if (!aligning && mode == LockedMode.FOLLOW_ANGLE) {
            followOmegaFeedForwardRadSec.coerceIn(-SERVO_MAX_OMEGA, SERVO_MAX_OMEGA)
        } else {
            0.0
        }
        val omegaErr = omegaFf - omegaActual
        val errorForControl = errorRad.coerceIn(-SERVO_MAX_ERROR_RAD, SERVO_MAX_ERROR_RAD)
        var alphaCmd = servoKp * errorForControl + servoKd * omegaErr
        alphaCmd = alphaCmd.coerceIn(-servoMaxAlpha, servoMaxAlpha)

        // Ensure the joint isn't treated as free-spin in servo modes. We do NOT rely on the joint motor here because
        // it's backend-dependent; instead we apply an explicit torque servo below.
        val updated = revolute.copy(
            maxForceTorque = VSJointMaxForceTorque(SERVO_MAX_FORCE, SERVO_MAX_TORQUE),
            compliance = SERVO_COMPLIANCE,
            driveVelocity = null,
            driveForceLimit = null,
            driveGearRatio = null,
            driveFreeSpin = false
        )
        if (updated != revolute) {
            joint = updated
            (physLevel as? VsiPhysLevel)?.updateJoint(jointId, updated)
        }

        // Torque servo (works even when revolute drive velocity is not honored).
        val torqueMassMultiplier: Double = run {
            val canSub = !subShip.isStatic
            val canMain = mainShip != null && !mainShip.isStatic
            if (!canSub && !canMain) return

            val subInertia = if (canSub) getAngularInertia(subShip, updated.pose0.pos, axisWorld) else 0.0
            val mainInertia = if (canMain) getAngularInertia(mainShip!!, updated.pose1.pos, axisWorld) else 0.0

            when {
                canSub && canMain -> parallelOperator(subInertia, mainInertia)
                canSub -> subInertia
                else -> mainInertia
            }
        }

        // Torque = I_eff * alpha_cmd, clamped.
        var torqueMag = torqueMassMultiplier * alphaCmd
        val maxTorque = servoTorqueLimit
        torqueMag = torqueMag.coerceIn(-maxTorque, maxTorque)

        val torqueVec = axisWorld.mul(torqueMag, Vector3d())
        if (!subShip.isStatic) {
            subShip.applyWorldTorque(torqueVec)
        }
        if (mainShip != null && !mainShip.isStatic) {
            mainShip.applyWorldTorque(torqueVec.mul(-1.0, Vector3d()))
        }
    }

    public override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)

        tag.putBoolean(ClockworkConstants.Nbt.RUNNING, isRunning)
        tag.putFloat(ClockworkConstants.Nbt.ANGLE, targetAngle)
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
        if (clientPacket) {
            tag.putBoolean("followAngleStalled", followAngleStalled)
        }

        val mapper = VSJacksonUtil.dtoMapper
        // Avoid saving a potentially large/non-zero drive velocity from mid-servo movement.
        val jointForSave = (joint as? VSRevoluteJoint)?.copy(driveVelocity = null) ?: joint
        if (jointForSave is VSRevoluteJoint) {
            tag.putByteArray("joint", mapper.writeValueAsBytes(jointForSave))
        }

        tag.putInt("jointID", jointID)

        if (shiptraptionID == NO_SHIPTRAPTION_ID) return

        tag.putLong(ClockworkConstants.Nbt.OLD_POS, worldPosition.asLong())
        //to make it more general
        tag.putVector3d(ClockworkConstants.Nbt.OLD_SHIPTRAPTION_CENTER, bearingPos)
        tag.putVector3d(ClockworkConstants.Nbt.NEW_SHIPTRAPTION_CENTER, bearingPos)
    }

    private fun loadTheRest(tag: CompoundTag, level: ServerLevel) {
        val joint = this.joint ?: return
        val mainId = level.getShipManagingPos(worldPosition)?.id

        val oldBPos = BlockPos.of(tag.getLong(ClockworkConstants.Nbt.OLD_POS))
        val oldPos = oldBPos.toJOMLD()

        val newPos = worldPosition.toJOMLD()

        val oldSPos = tag.getVector3d(ClockworkConstants.Nbt.OLD_SHIPTRAPTION_CENTER) ?: return
        val newSPos = tag.getVector3d(ClockworkConstants.Nbt.NEW_SHIPTRAPTION_CENTER) ?: return

        bearingPos = bearingPos.sub(oldSPos).add(newSPos)

        // Worlds may contain legacy fixed-joint NBT. Always convert to a revolute joint on load.
        val revolute = when (joint) {
            is VSRevoluteJoint -> joint
            is VSFixedJoint -> VSRevoluteJoint(
                joint.shipId0, joint.pose0, joint.shipId1, joint.pose1,
                maxForceTorque = VSJointMaxForceTorque(SERVO_MAX_FORCE, SERVO_MAX_TORQUE),
                compliance = SERVO_COMPLIANCE,
                driveFreeSpin = true
            )
            else -> throw AssertionError()
        }

        this.joint = revolute.copy(
            shipId0 = shiptraptionID,
            pose0 = VSJointPose(revolute.pose0.pos - oldSPos + newSPos, revolute.pose0.rot),
            shipId1 = mainId,
            pose1 = VSJointPose(revolute.pose1.pos - oldPos + newPos, revolute.pose1.rot),
            driveVelocity = null, // start neutral; physTick will apply servo if needed
        )

        controllerCreationData = PhysBearingData(
            bearingAxis.get(Vector3d()),
            Math.toRadians(targetAngle.toDouble()),
            getRealisticAngularSpeed(),
            (movementMode?.get() ?: LockedMode.UNLOCKED) != LockedMode.UNLOCKED,
            aligning,
            mainId ?: -1L,
            this.joint?.pose1?.pos?.get(Vector3d()) ?: Vector3d(),
            this.joint?.pose0?.pos?.get(Vector3d()) ?: Vector3d()
        )

        tryMakeJoint()
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        val angleBefore = targetAngle
        open = tag.getBoolean(ClockworkConstants.Nbt.OPEN)
        isRunning = tag.getBoolean(ClockworkConstants.Nbt.RUNNING)
        targetAngle = tag.getFloat(ClockworkConstants.Nbt.ANGLE)
        lastAngle = targetAngle
        curAngle = targetAngle
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
            }
        } else {
            shiptraptionID = NO_SHIPTRAPTION_ID
        }
        sequencedAngleLimit = tag.getFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_LIMIT)
        sequencedAngleProgress = tag.getFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_PROGRESS)

        bearingPos = tag.getVector3d("bearingPos")!!
        bearingAxis = tag.getVector3d("bearingAxis")!!
        aligning = tag.getBoolean("aligning")
        if (clientPacket && tag.contains("followAngleStalled")) {
            followAngleStalled = tag.getBoolean("followAngleStalled")
        }

        val mapper = VSJacksonUtil.dtoMapper

        if (tag.contains("constraint")) {
            val temp = mapper.readValue(tag.getByteArray("constraint"), VSJointAndId::class.java)
            joint = temp.joint as VSRevoluteJoint
            jointID = temp.jointId
        } else if (tag.contains("joint")) {
            joint = mapper.readValue(tag.getByteArray("joint"), VSRevoluteJoint::class.java).copy(driveVelocity = null)
            jointID = tag.getInt("jointID")
        } else if (tag.contains("fjoint")) {
            // Legacy worlds: fixed-joint FOLLOW_ANGLE implementation. Convert immediately to revolute.
            val fj = mapper.readValue(tag.getByteArray("fjoint"), VSFixedJoint::class.java)
            joint = VSRevoluteJoint(
                fj.shipId0, fj.pose0, fj.shipId1, fj.pose1,
                maxForceTorque = VSJointMaxForceTorque(SERVO_MAX_FORCE, SERVO_MAX_TORQUE),
                compliance = SERVO_COMPLIANCE,
                driveFreeSpin = true
            )
            jointID = tag.getInt("jointID")
        }

        super.read(tag, clientPacket)
        // Behaviours were just read; sync derived servo parameters from the persisted values.
        servoStrengthBehaviour?.also { setServoStrengthSetting(it.currentValue, sendUpdate = false) }
        servoStiffnessBehaviour?.also { setServoStiffnessSetting(it.currentValue, sendUpdate = false) }
        if (clientPacket) {return}

        // may not have level on read so i have to do this
        loadingFn = { level -> loadTheRest(tag, level) }

        val level = level as? ServerLevel ?: return
        loadingFn!!(level)
        loadingFn = null
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
            val mode = movementMode?.get() ?: LockedMode.UNLOCKED
            if (aligning || mode == LockedMode.LOCKED) return 0f
            if (mode == LockedMode.FOLLOW_ANGLE && followAngleStalled) return 0f
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

    fun tryMakeJoint() {
        val joint = joint ?: return

        ClockworkMod.physTickOnce(level.dimensionId!!) { level, _, tryNextTick ->
            level as VsiPhysLevel
            if (
                joint.shipId0 != null && level.getShipById(joint.shipId0!!) == null ||
                joint.shipId1 != null && level.getShipById(joint.shipId1!!) == null
            ) {
                if (DEBUG_SERVO) ClockworkMod.LOGGER.info("[PhysBearing] tryMakeJoint: ships not loaded yet; retrying next tick (ship0={}, ship1={}, jointId={})", joint.shipId0, joint.shipId1, jointID)
                tryNextTick()
                return@physTickOnce
            }
            val existing = level.getJointById(jointID)
            if (existing != null) {
                // If the stored jointId is still valid, prefer updating in-place over creating a second joint.
                if (existing is VSRevoluteJoint && joint is VSRevoluteJoint &&
                    existing.shipId0 == joint.shipId0 && existing.shipId1 == joint.shipId1
                ) {
                    level.updateJoint(jointID, joint)
                    isRunning = true
                    return@physTickOnce
                }
                if (existing == joint) {
                    isRunning = true
                    return@physTickOnce
                }
            }
            val id = level.addJoint(joint)
            if (id == -1) {
                tryNextTick()
                return@physTickOnce
            }
            this.jointID = id

            isRunning = true
            lastStateChanged = ticks
        }
    }

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

        val shipOnID = shipOn?.id

        val posInWorld = shipOn?.transform?.shipToWorld?.transformPosition(
            posInOwnerShip - bearingPos + shiptraption.inertiaData.centerOfMass , Vector3d()
        ) ?: (worldPos - bearingPos + shiptraption.inertiaData.centerOfMass)
        val rotInWorld = shipOn?.transform?.shipToWorldRotation ?: Quaterniond()
        val scaling    = shipOn?.transform?.shipToWorldScaling ?: Vector3d(1.0, 1.0, 1.0)

        shiptraption.unsafeSetTransform(BodyTransformFactory.create(
            posInWorld, rotInWorld, scaling, shiptraption.transform.positionInModel
        ))

        val ship1rot = getHingeRotation(direction)
        val ship2rot = getHingeRotation(direction)

        val extraDist = 1.0
//        val realSpeed = if (getSpeed().absoluteValue > 0.0f) getRealisticAngularSpeed() else 0.0f
//        val newDriveVelocity = if (realSpeed != 0.0f) VSRevoluteJoint.VSRevoluteDriveVelocity(getRealisticAngularSpeed(), true) else null
        joint = VSRevoluteJoint(
            shiptraptionID, VSJointPose(bearingPos.fma(-extraDist, axis, Vector3d()), ship1rot),
            shipOnID, VSJointPose(posInOwnerShip.fma(-extraDist, axis, Vector3d()), ship2rot),
            maxForceTorque = VSJointMaxForceTorque(SERVO_MAX_FORCE, SERVO_MAX_TORQUE),
            compliance = SERVO_COMPLIANCE,
            driveFreeSpin = true
//            driveVelocity = newDriveVelocity,
        )

        this.bearingAxis = axis
        this.bearingPos = bearingPos

        controllerCreationData = PhysBearingData(
            bearingAxis.get(Vector3d()),
            Math.toRadians(targetAngle.toDouble()),
            getRealisticAngularSpeed(),
            (movementMode?.get() ?: LockedMode.UNLOCKED) != LockedMode.UNLOCKED,
            aligning,
            shipOnID ?: -1L,
            joint!!.pose1.pos.get(Vector3d()),
            joint!!.pose0.pos.get(Vector3d())
        )

        tryMakeJoint()
        updateDrive()

        sendData()
        updateGeneratedRotation()
    }

    override fun destroy() {
        val level = level ?: return
        if (level.isClientSide || level !is ServerLevel) return

        val ship = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return
        BearingController.getOrCreate(ship)!!.removePhysBearing(bearingID)

        joint?.let { level.gtpa.removeJoint(jointID) }
    }

    fun disassemble() {
        if (!isRunning && shiptraptionID == NO_SHIPTRAPTION_ID) return
        if (ticks - lastStateChanged <= cooldown) return
        targetAngle = 0f
        if (shiptraptionID == NO_SHIPTRAPTION_ID) return
        val level = level as ServerLevel
        val ship = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return resetState()

        if (!canDisassemble(bearingAxis, ship, level.getShipObjectManagingPos(worldPosition))) {
            disassembleWhenPossible = !disassembleWhenPossible
            aligning = !aligning
            BearingController.getOrCreate(ship)!!.bearingData[bearingID]?.let { it.aligning = this.aligning }
            updateDrive()
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
            updateDrive()
            return
        }
        BearingController.getOrCreate(subShip)!!.removePhysBearing(bearingID)

        lastStateChanged = ticks
        resetState()
    }

    private fun resetState() {
        bearingID = -1
        shiptraptionID = NO_SHIPTRAPTION_ID
        isRunning = false
        updateGeneratedRotation()
        assembleNextTick = false
        disassembleWhenPossible = false
        sequencedAngleLimit = -1.0f
        sequencedAngleProgress = 0f
        targetAngle = 0f
        sendData()
        jointID = -1
        aligning = false
        servoMode = LockedMode.UNLOCKED
        lockedHoldAngleRad = null
        followOmegaFeedForwardRadSec = 0.0
        lastAligningState = false

        lastAngle = 0f
        curAngle = 0f

        clearFollowAngleStallState()
    }

    private fun tryAssembleNextTick() {
        if (!assembleNextTick) {return}
        if (ticks - lastStateChanged <= cooldown) {return}
        assembleNextTick = false
        if (!isRunning) {assemble()}
    }

    private fun tryUpdateData() {
        if (shiptraptionID == NO_SHIPTRAPTION_ID) {return}
        val mode = movementMode?.get() ?: LockedMode.UNLOCKED
        val speedNow = getSpeed()
        val aligningNow = aligning

        if (lastSpeed == speedNow && lastMode == mode && lastAligningState == aligningNow) {
            return
        }

        // When entering a hold mode, capture the current physical angle to avoid any instantaneous jump.
        if (lastMode != mode && (mode == LockedMode.FOLLOW_ANGLE || mode == LockedMode.LOCKED)) {
            val shipOn = level!!.getShipObjectManagingPos(blockPos)?.transform
            val shiptraption = level!!.shipObjectWorld.allShips.getById(shiptraptionID)?.transform ?: return
            val currentRad = getAngle(bearingAxis, shiptraption, shipOn)
            targetAngle = Math.toDegrees(currentRad).toFloat()
            lockedHoldAngleRad = if (mode == LockedMode.LOCKED) currentRad else null
        } else if (lastMode == LockedMode.LOCKED && mode != LockedMode.LOCKED) {
            lockedHoldAngleRad = null
        }

        lastSpeed = speedNow
        lastMode = mode
        lastAligningState = aligningNow

        updateDrive()
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
        val dir = originalDirection!!
        return convertToAngular(getSpeed()) * if (dir == Direction.WEST || dir == Direction.NORTH || dir == Direction.DOWN) 1 else -1
    }

    fun getRealisticAngularSpeed(): Float {
        val dir = originalDirection!!
        return getSpeed() * 2f * PI.toFloat() / 60f * if (dir == Direction.WEST || dir == Direction.NORTH || dir == Direction.DOWN) 1 else -1
    }

    override fun tick() {
        super.tick()
        prevAngle = targetAngle
        ticks++
        if (level!!.isClientSide) clientAngleDiff /= 2f
        if (!level!!.isClientSide) {
            loadingFn?.also {
                it(level as ServerLevel)
                loadingFn = null
            }

            // If the attached sub-ship was deleted (e.g., removed by VS), stop the bearing and let it close.
            // Use allShips (not loadedShips) so chunk load ordering doesn't spuriously disable the bearing.
            if (isRunning && shiptraptionID != NO_SHIPTRAPTION_ID) {
                val sLevel = level as ServerLevel
                val exists = sLevel.shipObjectWorld.allShips.getById(shiptraptionID) != null
                if (!exists) {
                    missingSubShipTicks++
                    if (missingSubShipTicks >= MISSING_SUBSHIP_GRACE_TICKS) {
                        if (DEBUG_SERVO) {
                            ClockworkMod.LOGGER.info("[PhysBearing] subShip missing; disabling (shipId={}, jointId={})", shiptraptionID, jointID)
                        }
                        if (jointID != -1) sLevel.gtpa.removeJoint(jointID)
                        controllerCreationData = null
                        controllerUpdateData = null
                        // Ensure the close animation can run even if we were mid-opening.
                        if (!open && openProgress > 0f) open = true
                        opening = false
                        resetState()
                        tickAnimationLogic()
                        return
                    }
                } else {
                    missingSubShipTicks = 0
                }
            } else {
                missingSubShipTicks = 0
            }

            val subShip = (level as ServerLevel).shipObjectWorld.loadedShips.getById(shiptraptionID)
            controllerCreationData?.also {
                bearingID = BearingController
                    .getOrCreate(subShip ?: return@also)!!
                    .addPhysBearing(it)
                controllerCreationData = null
            }
            controllerUpdateData?.also {
                BearingController
                    .getOrCreate(subShip ?: return@also)!!
                    .updatePhysBearing(bearingID, it)
                controllerUpdateData = null
            }
            tryAssembleNextTick()
            if (disassembleWhenPossible) { shipDisassemble() }
        }
        tickAnimationLogic()
        if (!isRunning) return
        val mode = movementMode?.get() ?: LockedMode.UNLOCKED
        if (mode != LockedMode.FOLLOW_ANGLE && (followAngleStalled || followAngleStallTicks != 0 || lastActualAngleRadForStall != null)) {
            // Stall state is only meaningful for FOLLOW_ANGLE.
            clearFollowAngleStallState()
        }
        if (shiptraptionID == NO_SHIPTRAPTION_ID) {
            targetAngle = 0f
        } else if (joint != null && jointID != -1 && !aligning && mode != LockedMode.LOCKED) {
            val angularSpeed = -getActualAngularSpeed()
            if (!level!!.isClientSide && mode == LockedMode.FOLLOW_ANGLE) {
                updateFollowAngleStallState(angularSpeed)
            }
            if (mode == LockedMode.FOLLOW_ANGLE && followAngleStalled) {
                // Don't advance the expected angle while stalled.
            } else {
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
            //this is stupid
            lastAngle = when {
                newAngle >= 360f * 2 -> lastAngle - 360f * 2
                newAngle < 0f -> lastAngle + 360f * 2
                else -> lastAngle
            }
            curAngle = when {
                newAngle >= 360f * 2 -> curAngle - 360f * 2
                newAngle < 0f -> curAngle + 360f * 2
                else -> curAngle
            }
            targetAngle = when {
                newAngle >= 360f * 2 -> newAngle - 360f * 2
                newAngle < 0f -> newAngle + 360f * 2
                else -> newAngle
            }
            }
        }
        //needs to be after targetAngle change
        if (!level!!.isClientSide) { tryUpdateData() }
    }

    override fun onSpeedChanged(previousSpeed: Float) {
        sequencedAngleLimit = -1.0f
        sequencedAngleProgress = 0.0f

        if (sequenceContext != null && sequenceContext.instruction == SequencerInstructions.TURN_ANGLE) {
            sequencedAngleLimit = sequenceContext.getEffectiveValue(theoreticalSpeed.toDouble()).toFloat()
        }

        if (level != null && !level!!.isClientSide && joint != null) {
            lastSpeed = getSpeed()
            updateDrive()
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
        val level = level as ServerLevel
        val shiptraption = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return null
        val mainShip = level.getShipManagingPos(worldPosition)
        return getAngle(bearingAxis, shiptraption.transform, mainShip?.transform)
    }

    override fun attach(contraption: ControlledContraptionEntity) {}
    override fun onStall() { if (!level!!.isClientSide) sendData() }
    override fun isValid(): Boolean = !isRemoved
    override fun isAttachedTo(contraption: AbstractContraptionEntity): Boolean = false
    override fun setAngle(forcedAngle: Float) { targetAngle = forcedAngle }
    override fun getLastAssemblyException(): AssemblyException? = lastException
    override fun getBlockPosition(): BlockPos = worldPosition
    override fun isWoodenTop(): Boolean = false

    private class ServoSettingValueBoxTransform(private val y: Double, private val x: Double) : ValueBoxTransform.Sided() {
        override fun getSouthLocation(): Vec3 {
            return VecHelper.voxelSpace(x, y, 15.5)
        }

        override fun getLocalOffset(level: LevelAccessor, pos: BlockPos, state: BlockState): Vec3 {
            return super.getLocalOffset(level, pos, state)
                .add(
                    Vec3.atLowerCornerOf(state.getValue(BearingBlock.FACING).normal)
                        .scale((-2 / 16f).toDouble())
                )
        }

        override fun rotate(level: LevelAccessor, pos: BlockPos, state: BlockState, ms: PoseStack) {
            if (!side.axis.isHorizontal) {
                TransformStack.of(ms)
                    .rotateYDegrees((AngleHelper.horizontalAngle(state.getValue(BearingBlock.FACING)) + 180))
            }
            super.rotate(level, pos, state, ms)
        }

        override fun isSideActive(state: BlockState, direction: Direction): Boolean {
            // Match Create's bearing mode selector faces: show on the 4 side faces around the bearing axis.
            return direction.axis != state.getValue(BearingBlock.FACING).axis
        }
    }

    private open class ServoTuningScrollValueBehaviour(
        label: Component,
        be: GeneratingKineticBlockEntity,
        slot: ValueBoxTransform,
        private val behaviourNetId: Int
    ) : ScrollValueBehaviour(label, be, slot) {
        // ScrollValueBehaviour.value is protected; expose it for BE init/sync.
        var currentValue: Int
            get() = value
            set(v) { value = v }

        override fun netId(): Int = behaviourNetId
    }

    private class ServoStrengthScrollValueBehaviour(
        label: Component,
        be: GeneratingKineticBlockEntity,
        slot: ValueBoxTransform
    ) : ServoTuningScrollValueBehaviour(label, be, slot, 1) {

        override fun getType(): BehaviourType<*> = TYPE

        override fun getClipboardKey(): String = "PhysBearingServoStrength"

        override fun write(nbt: CompoundTag, clientPacket: Boolean) {
            nbt.putInt(NBT_KEY, currentValue)
        }

        override fun read(nbt: CompoundTag, clientPacket: Boolean) {
            if (nbt.contains(NBT_KEY)) currentValue = nbt.getInt(NBT_KEY)
        }

        companion object {
            val TYPE: BehaviourType<ServoStrengthScrollValueBehaviour> =
                BehaviourType("phys_bearing_servo_strength")
            private const val NBT_KEY = "ServoStrength"
        }
    }

    private class ServoStiffnessScrollValueBehaviour(
        label: Component,
        be: GeneratingKineticBlockEntity,
        slot: ValueBoxTransform
    ) : ServoTuningScrollValueBehaviour(label, be, slot, 2) {

        override fun getType(): BehaviourType<*> = TYPE

        override fun getClipboardKey(): String = "PhysBearingServoStiffness"

        override fun write(nbt: CompoundTag, clientPacket: Boolean) {
            nbt.putInt(NBT_KEY, currentValue)
        }

        override fun read(nbt: CompoundTag, clientPacket: Boolean) {
            if (nbt.contains(NBT_KEY)) currentValue = nbt.getInt(NBT_KEY)
        }

        companion object {
            val TYPE: BehaviourType<ServoStiffnessScrollValueBehaviour> =
                BehaviourType("phys_bearing_servo_stiffness")
            private const val NBT_KEY = "ServoStiffness"
        }
    }

    companion object {
        const val NO_SHIPTRAPTION_ID: Long = -1

        // Per-bearing tuning UI (Create value boxes).
        private const val SERVO_STRENGTH_MIN = 0
        private const val SERVO_STRENGTH_MAX = 100
        private const val SERVO_STRENGTH_DEFAULT = 50
        private const val SERVO_STIFFNESS_MIN = 0
        private const val SERVO_STIFFNESS_MAX = 100
        private const val SERVO_STIFFNESS_DEFAULT = 50

        // Slider mapping:
        // - "Stiffness" controls closed-loop natural frequency (wn), with Kp = wn^2.
        // - Kd is chosen for a fixed damping ratio to minimize sway/overshoot.
        // - "Strength" caps both max torque and max angular acceleration.
        // Temporary: boost slider magnitudes for testing (e.g., UI 100 behaves like 1000 if set to 10.0).
        private const val SERVO_TEST_SCALE = 2.0
        private const val SERVO_DAMPING_RATIO = 1.25
        private const val SERVO_WN_MIN = 4.0
        private const val SERVO_WN_MAX = 64.0
        private const val SERVO_ALPHA_MIN = 30.0
        private const val SERVO_ALPHA_MAX = 480.0
        private const val SERVO_FORCE_TORQUE_MIN = 1.0e8
        private const val SERVO_FORCE_TORQUE_MAX = 1.0e10

        private const val MISSING_SUBSHIP_GRACE_TICKS = 20

        // FOLLOW_ANGLE stall detection: if we keep commanding rotation but the actual angle isn't moving,
        // stop advancing the expected angle so we don't push into collisions indefinitely.
        private const val FOLLOW_STALL_MIN_CMD_DEG_PER_TICK = 0.05f
        private const val FOLLOW_STALL_TICKS = 5
        private const val FOLLOW_STALL_EPS_RAD_BASE = 1.0e-4
        private const val FOLLOW_STALL_EPS_FRACTION = 0.005

        // Servo tuning (acceleration-space PD):
        //   alpha_cmd = Kp * angle_error + Kd * (omega_target - omega_actual)
        // Then we apply torque = I_eff * alpha_cmd.
        private const val SERVO_KP = 250.0 // (rad/s^2)/rad
        private const val SERVO_KD = 40.0  // (rad/s^2)/(rad/s)
        private const val SERVO_MAX_OMEGA = 80.0 // rad/s (feed-forward clamp)
        private const val SERVO_MAX_ALPHA = 120.0 // rad/s^2 (hard stability clamp)
        private const val SERVO_MAX_ERROR_RAD = 0.75 // rad (~43 deg), avoids absurd snap torques on large steps
        private const val SERVO_DEADBAND_RAD = 1.0e-4 // ~0.006 degrees

        // Joint strength. Values are intentionally very high to avoid "sagging".
        private const val SERVO_MAX_FORCE = 1.0e9f
        private const val SERVO_MAX_TORQUE = 1.0e9f
        private const val SERVO_DRIVE_FORCE_LIMIT = 1.0e9f

        // Extremely stiff, but not pathological (avoid ~1e-100 which can blow up solvers).
        private const val SERVO_COMPLIANCE = 1.0e-10

        private const val DEBUG_SERVO = false

        //tolerance is in degrees
        @JvmStatic
        fun canDisassemble(bearingAxis: Vector3d, mainShip: ServerShip, otherShip: ServerShip?, tolerance: Int=5): Boolean {
            if (abs(Math.toDegrees(getAngle(bearingAxis, mainShip.transform, otherShip?.transform))) > tolerance) return false
            return true
        }
    }
}
