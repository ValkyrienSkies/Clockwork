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
    @Volatile private var groundBodyId: Long = UNKNOWN_GROUND_BODY_ID
    @Volatile private var driveWarmupTicks: Int = 0
    @Volatile private var shouldVerifyConnection: Boolean = false
    @Volatile private var desiredModeOrdinal: Int = PhysBearingRotationMode.UNLOCKED.ordinal
    @Volatile private var desiredAngularVelocity: Float = 0f
    @Volatile private var physTargetAngle: Double = 0.0
    @Volatile private var physAligning: Boolean = false
    @Volatile private var collisionEnableCooldownTicks: Int = 0
    private var followAngleSmoothInitialized: Boolean = false
    private var followAngleSmoothTimeSeconds: Double = 0.0
    private var followAngleSmoothFromAngleRad: Double = 0.0
    private var followAngleSmoothToAngleRad: Double = 0.0
    private var followAngleSmoothCurrentAngleRad: Double = 0.0
    private var followAngleHoldAngleRad: Double? = null

    private var rotationStallDetectTicks: Int = 0
    private var rotationStallCooldownTicks: Int = 0

    // Reused vectors to avoid per-phys-tick allocations (helps prevent GC hitches).
    private val tmpAxisGlobal: Vector3d = Vector3d()
    private val tmpOmegaRelative: Vector3d = Vector3d()
    private val tmpTorque: Vector3d = Vector3d()
    private val tmpTorqueOpp: Vector3d = Vector3d()
    private val tmpGlobalPos: Vector3d = Vector3d()
    private val tmpOffset: Vector3d = Vector3d()
    private val tmpProj: Vector3d = Vector3d()
    private val tmpOffsetPerp: Vector3d = Vector3d()
    private val tmpAxisLocal: Vector3d = Vector3d()
    private val tmpMoiAxisLocal: Vector3d = Vector3d()

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

    private fun normalizeJointShipId(id: Long?): Long? = when (id) {
        null -> null
        NO_SHIPTRAPTION_ID -> null
        else -> id
    }

    private fun normalizeJointShipIdTreatingGroundAsNull(id: Long?): Long? {
        val normalized = normalizeJointShipId(id)
        return if (groundBodyId != UNKNOWN_GROUND_BODY_ID && normalized == groundBodyId) null else normalized
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
        if (collisionEnableCooldownTicks > 0) collisionEnableCooldownTicks--

        // If this bearing is mounted on a ship, prefer the physics-provided owning ship id. This prevents cases where
        // game ticks run before the owning ship is fully loaded and we accidentally treat it as world-attached.
        if (physShip != null && mainShipId != physShip.id) {
            mainShipId = physShip.id
        }

        // Prevent creating a world-attached joint if this bearing is mounted on an unloaded ship.
        if (mainShipId == UNKNOWN_MAIN_SHIP_ID) return
        val subPhysShip = physLevel.getShipById(shiptraptionID) ?: return
        val mainPhysShip = if (mainShipId == NO_SHIPTRAPTION_ID) null else physLevel.getShipById(mainShipId) ?: return
        fun ensureCollisionEnabled(force: Boolean = false) {
            // Joints can implicitly disable collisions between the connected bodies; enable them so sub-ships can't
            // clip through their parent or the world.
            if (!force && collisionEnableCooldownTicks > 0) return
            collisionEnableCooldownTicks = COLLISION_ENABLE_PERIOD_TICKS
            val otherId = when {
                mainPhysShip != null -> mainPhysShip.id
                mainShipId == NO_SHIPTRAPTION_ID && groundBodyId != UNKNOWN_GROUND_BODY_ID -> groundBodyId
                else -> null
            } ?: return
            physLevel.enableCollisionBetween(shiptraptionID, otherId)
            physLevel.enableCollisionBetween(otherId, shiptraptionID)
        }

        val desiredMode = PhysBearingRotationMode.entries.getOrElse(desiredModeOrdinal) { PhysBearingRotationMode.UNLOCKED }
        val shouldFollowAngle = desiredMode == PhysBearingRotationMode.FOLLOW_ANGLE || physAligning
        val mainId: Long? = if (mainShipId == NO_SHIPTRAPTION_ID) null else mainShipId

        val direction = originalDirection ?: return
        val axis = bearingAxis
        val hingeRot = getHingeRotation(direction)

        // Detect when rotation is blocked (typically by collisions) and periodically pause driving so we don't keep
        // pushing ships through each other.
        val axisGlobal = tmpAxisGlobal.set(axis)
        mainPhysShip?.transform?.shipToWorldRotation?.transform(axisGlobal)
        val omegaRelative = tmpOmegaRelative.set(subPhysShip.angularVelocity)
        if (mainPhysShip != null && !mainPhysShip.isStatic) omegaRelative.sub(mainPhysShip.angularVelocity)
        val omegaActual = axisGlobal.dot(omegaRelative)
        val wantsRotationInput = !physAligning && abs(desiredAngularVelocity) > 1e-6f
        if (wantsRotationInput && driveWarmupTicks == 0) {
            val omegaTarget = -desiredAngularVelocity.toDouble()
            val omegaAbsThreshold = max(
                ROTATION_STALL_ACTUAL_OMEGA_EPS_RAD_S,
                abs(omegaTarget) * ROTATION_STALL_RELATIVE_OMEGA_FACTOR,
            )
            if (rotationStallCooldownTicks > 0) {
                if (abs(omegaActual) > omegaAbsThreshold) {
                    rotationStallCooldownTicks = 0
                } else {
                    rotationStallCooldownTicks--
                }
                rotationStallDetectTicks = 0
            } else if (abs(omegaActual) >= omegaAbsThreshold) {
                rotationStallDetectTicks = 0
            } else if (++rotationStallDetectTicks >= ROTATION_STALL_DETECT_TICKS) {
                rotationStallDetectTicks = 0
                rotationStallCooldownTicks = ROTATION_STALL_COOLDOWN_TICKS
            }
        } else {
            rotationStallDetectTicks = 0
            rotationStallCooldownTicks = 0
        }

        // Place the hinge on the shared face between the bearing block and the first block of the attached ship.
        // This avoids large lever-arms that can cause instability when the sub-ship is heavy/off-center.
        val defaultMainPos = Vector3d(worldPosition.center.toJOML()).fma(0.5, axis, Vector3d())
        var existing: VSJoint? = if (jointID != -1) vsiPhysLevel.getJointById(jointID) else null

        fun jointIds(joint: VSJoint): Pair<Long?, Long?>? = when (joint) {
            is VSRevoluteJoint -> normalizeJointShipIdTreatingGroundAsNull(joint.shipId0) to
                normalizeJointShipIdTreatingGroundAsNull(joint.shipId1)
            is VSFixedJoint -> normalizeJointShipIdTreatingGroundAsNull(joint.shipId0) to
                normalizeJointShipIdTreatingGroundAsNull(joint.shipId1)
            else -> null
        }

        fun jointMatchesExpectedShips(joint: VSJoint): Boolean {
            val (id0, id1) = jointIds(joint) ?: return true
            val expectedMainId = normalizeJointShipIdTreatingGroundAsNull(mainId)
            return (id0 == shiptraptionID && id1 == expectedMainId) || (id1 == shiptraptionID && id0 == expectedMainId)
        }

        fun jointInvolvesSubShip(joint: VSJoint): Boolean {
            val (id0, id1) = jointIds(joint) ?: return false
            return id0 == shiptraptionID || id1 == shiptraptionID
        }

        // Long loads can temporarily mis-resolve the main ship; if we recovered a joint that doesn't connect the
        // expected ships, drop it and recreate with the correct ids. Never remove an unrelated joint.
        if (existing != null && !jointMatchesExpectedShips(existing!!)) {
            if (jointInvolvesSubShip(existing!!)) {
                vsiPhysLevel.removeJoint(jointID)
            }
            jointID = -1
            joint = null
            existing = null
            shouldVerifyConnection = true
        }

        fun getSubMainPoseData(joint: VSJoint): Triple<Vector3dc, Vector3dc, Boolean>? {
            val expectedMainId = normalizeJointShipIdTreatingGroundAsNull(mainId)
            return when (joint) {
                is VSRevoluteJoint -> {
                    val id0 = normalizeJointShipIdTreatingGroundAsNull(joint.shipId0)
                    val id1 = normalizeJointShipIdTreatingGroundAsNull(joint.shipId1)
                    when {
                        id0 == shiptraptionID && id1 == expectedMainId -> Triple(joint.pose0.pos, joint.pose1.pos, true)
                        id1 == shiptraptionID && id0 == expectedMainId -> Triple(joint.pose1.pos, joint.pose0.pos, false)
                        else -> null
                    }
                }
                is VSFixedJoint -> {
                    val id0 = normalizeJointShipIdTreatingGroundAsNull(joint.shipId0)
                    val id1 = normalizeJointShipIdTreatingGroundAsNull(joint.shipId1)
                    when {
                        id0 == shiptraptionID && id1 == expectedMainId -> Triple(joint.pose0.pos, joint.pose1.pos, true)
                        id1 == shiptraptionID && id0 == expectedMainId -> Triple(joint.pose1.pos, joint.pose0.pos, false)
                        else -> null
                    }
                }
                else -> null
            }
        }

        val existingPoseData = existing?.let(::getSubMainPoseData)
        val (subPos, mainPos, subIsPose0) = existingPoseData ?: run {
            // On reconnect, prefer using the actual hinge world position to compute local anchors. This avoids large
            // impulses when ships unload/reload or when a ship has been re-centered by VS.
            val hingeWorldPos = if (mainPhysShip != null) {
                mainPhysShip.transform.shipToWorld.transformPosition(defaultMainPos, Vector3d())
            } else {
                Vector3d(defaultMainPos)
            }
            val storedSubLocalPos = Vector3d(bearingPos).fma(0.5, axis, Vector3d())
            val storedSubWorldPos = subPhysShip.transform.shipToWorld.transformPosition(storedSubLocalPos, Vector3d())
            val subLocalPos = if (storedSubWorldPos.distanceSquared(hingeWorldPos)
                <= RECONNECT_STORED_ANCHOR_MAX_DIST * RECONNECT_STORED_ANCHOR_MAX_DIST
            ) {
                storedSubLocalPos
            } else {
                subPhysShip.transform.worldToShip.transformPosition(hingeWorldPos, Vector3d())
            }
            Triple(subLocalPos, defaultMainPos, mainId != null)
        }
        val subPose = VSJointPose(Vector3d(subPos).fma(JOINT_ANCHOR_INSET, axis, Vector3d()), hingeRot)
        val mainPose = VSJointPose(Vector3d(mainPos).fma(-JOINT_ANCHOR_INSET, axis, Vector3d()), hingeRot)

        fun unwrapAngleNear(wrappedAngleRad: Double, referenceAngleRad: Double): Double {
            val twoPi = Math.PI * 2.0
            return wrappedAngleRad + twoPi * round((referenceAngleRad - wrappedAngleRad) / twoPi)
        }
        val rotationApplied = wantsRotationInput && rotationStallCooldownTicks == 0 && driveWarmupTicks == 0
        if (!shouldFollowAngle || physAligning || rotationApplied) {
            followAngleHoldAngleRad = null
        } else if (followAngleHoldAngleRad == null) {
            val wrapped = getAngle(axis, subPhysShip.transform, mainPhysShip?.transform)
            followAngleHoldAngleRad = unwrapAngleNear(wrapped, Math.toRadians(physTargetAngle))
        }

        if (shouldVerifyConnection) {
            if (existing != null) {
                joint = existing
                shouldVerifyConnection = false
            } else {
                jointID = -1
                joint = null
                followAngleHoldAngleRad = null
                followAngleSmoothInitialized = false
                val newJoint: VSJoint = if (subIsPose0) {
                    VSRevoluteJoint(
                        shiptraptionID,
                        subPose,
                        mainId,
                        mainPose,
                        maxForceTorque = REVOLUTE_JOINT_MAX_FORCE_TORQUE,
                        compliance = REVOLUTE_JOINT_COMPLIANCE,
                        driveFreeSpin = true,
                    )
                } else {
                    VSRevoluteJoint(
                        mainId,
                        mainPose,
                        shiptraptionID,
                        subPose,
                        maxForceTorque = REVOLUTE_JOINT_MAX_FORCE_TORQUE,
                        compliance = REVOLUTE_JOINT_COMPLIANCE,
                        driveFreeSpin = true,
                    )
                }
                val id = vsiPhysLevel.addJoint(newJoint)
                if (id != -1) {
                    jointID = id
                    joint = newJoint
                    shouldVerifyConnection = false
                    driveWarmupTicks = REVOLUTE_DRIVE_WARMUP_TICKS
                }
                ensureCollisionEnabled(force = true)
                return
            }
        } else if (existing == null) {
            ensureCollisionEnabled()
            return
        }

        val updated: VSJoint = when (existing) {
            is VSRevoluteJoint -> {
                val needsUpdate = existing.maxForceTorque != REVOLUTE_JOINT_MAX_FORCE_TORQUE ||
                    existing.compliance != REVOLUTE_JOINT_COMPLIANCE ||
                    existing.driveVelocity != null ||
                    existing.driveForceLimit != null ||
                    existing.driveGearRatio != null ||
                    existing.driveFreeSpin != true
                if (needsUpdate) {
                    existing.copy(
                        maxForceTorque = REVOLUTE_JOINT_MAX_FORCE_TORQUE,
                        compliance = REVOLUTE_JOINT_COMPLIANCE,
                        driveVelocity = null,
                        driveForceLimit = null,
                        driveGearRatio = null,
                        driveFreeSpin = true,
                    )
                } else {
                    existing
                }
            }
            is VSFixedJoint -> {
                // Legacy worlds may still have an old fixed joint; convert it to a revolute and drive with torque.
                followAngleSmoothInitialized = false
                driveWarmupTicks = max(driveWarmupTicks, REVOLUTE_DRIVE_WARMUP_TICKS)
                VSRevoluteJoint(
                    existing.shipId0,
                    VSJointPose(Vector3d(existing.pose0.pos), hingeRot),
                    existing.shipId1,
                    VSJointPose(Vector3d(existing.pose1.pos), hingeRot),
                    maxForceTorque = REVOLUTE_JOINT_MAX_FORCE_TORQUE,
                    compliance = REVOLUTE_JOINT_COMPLIANCE,
                    driveFreeSpin = true,
                )
            }
            else -> return
        }
        if (updated !== existing) {
            vsiPhysLevel.updateJoint(jointID, updated)
        }
        joint = updated

        ensureCollisionEnabled()

        // Drive revolute rotation via applied torque (both modes).
        if (driveWarmupTicks > 0) {
            driveWarmupTicks--
            return
        }

        val revoluteJoint = updated as? VSRevoluteJoint ?: return
        val (subLocalPos, mainLocalPos) = if (revoluteJoint.shipId0 == shiptraptionID) {
            revoluteJoint.pose0.pos to revoluteJoint.pose1.pos
        } else {
            revoluteJoint.pose1.pos to revoluteJoint.pose0.pos
        }

        if (!shouldFollowAngle && !rotationApplied) return

        val torque = tmpTorque.set(0.0, 0.0, 0.0)
        if (shouldFollowAngle) {
            val followTargetAngleRad = when {
                physAligning -> 0.0
                rotationApplied -> Math.toRadians(physTargetAngle)
                else -> followAngleHoldAngleRad ?: Math.toRadians(physTargetAngle)
            }
            val smoothTargetAngleRad = smoothFollowAngleTarget(followTargetAngleRad, dtSeconds, rotationApplied)
            val wrapped = getAngle(axis, subPhysShip.transform, mainPhysShip?.transform)
            val currentAngleRad = unwrapAngleNear(wrapped, smoothTargetAngleRad)
            val angleErrorRad = smoothTargetAngleRad - currentAngleRad

            val omegaFeedforward = if (rotationApplied) -desiredAngularVelocity.toDouble() else 0.0
            val omegaTarget = omegaFeedforward + angleErrorRad * FOLLOW_ANGLE_OMEGA_FROM_ANGLE_MULT
            val omegaError = omegaTarget - omegaActual * FOLLOW_ANGLE_ROTATION_RESISTANCE_MULTIPLIER
            val effectiveInertia = getEffectiveAngularInertia(subPhysShip, mainPhysShip, axisGlobal, subLocalPos, mainLocalPos)
            if (effectiveInertia > 0.0) {
                val maxDeltaOmega = if (rotationApplied) FOLLOW_ANGLE_MAX_DELTA_OMEGA_MOVING else FOLLOW_ANGLE_MAX_DELTA_OMEGA_STOPPING
                val accelCmd = (omegaError * FOLLOW_ANGLE_OMEGA_ERROR_MULTIPLIER)
                    .coerceIn(-motorAccelCap(dtSeconds, maxDeltaOmega), motorAccelCap(dtSeconds, maxDeltaOmega))
                val torqueMag = effectiveInertia * accelCmd
                torque.set(axisGlobal).mul(torqueMag)
            }
        } else {
            computeUnlockedTorque(
                outTorque = torque,
                subShip = subPhysShip,
                mainShip = mainPhysShip,
                axisGlobal = axisGlobal,
                omegaActual = omegaActual,
                desiredAngularVelocity = desiredAngularVelocity,
                subLocalPos = subLocalPos,
                mainLocalPos = mainLocalPos,
                dtSeconds = dtSeconds,
            )
        }

        if (torque.lengthSquared() > 1e-18) {
            val torqueMag = torque.length()
            if (torqueMag > MAX_APPLIED_TORQUE_MAG) {
                torque.mul(MAX_APPLIED_TORQUE_MAG / torqueMag)
            }
            if (!subPhysShip.isStatic) subPhysShip.applyWorldTorque(torque)
            if (mainPhysShip != null && !mainPhysShip.isStatic) {
                mainPhysShip.applyWorldTorque(tmpTorqueOpp.set(torque).mul(-1.0))
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
        val serverLevel = level as? ServerLevel
        if (serverLevel != null && mainShipId == NO_SHIPTRAPTION_ID
            && serverLevel.isChunkInShipyard(worldPosition.x shr 4, worldPosition.z shr 4)
        ) {
            mainShipId = UNKNOWN_MAIN_SHIP_ID
        }
        groundBodyId = serverLevel?.shipObjectWorld?.dimensionToGroundBodyIdImmutable?.get(serverLevel.dimensionId) ?: UNKNOWN_GROUND_BODY_ID
        driveWarmupTicks = if (isRunning) REVOLUTE_DRIVE_WARMUP_TICKS else 0
        followAngleSmoothInitialized = false
        joint = null
        jointID = if (isRunning && tag.contains("jointID")) tag.getInt("jointID") else -1

        super.read(tag, clientPacket)
        movementMode?.let {
            it.value = it.value.coerceIn(0, PhysBearingRotationMode.entries.size - 1)
        }
        desiredModeOrdinal = movementMode?.get()?.ordinal ?: PhysBearingRotationMode.UNLOCKED.ordinal
        desiredAngularVelocity = 0.0f
        rotationStallDetectTicks = 0
        rotationStallCooldownTicks = 0
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
            val allowIdOnlyMatch = snapshot.mainShipIdKnown && snapshot.mainShipId == null
            if (jointMatchesSnapshot(existing, snapshot) || (allowIdOnlyMatch && jointMatchesExpectedShipIds(existing, snapshot))) {
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
            val shipId0 = when (currentJoint) {
                is VSRevoluteJoint -> normalizeJointShipIdTreatingGroundAsNull(currentJoint.shipId0)
                is VSFixedJoint -> normalizeJointShipIdTreatingGroundAsNull(currentJoint.shipId0)
                else -> null
            }
            val shipId1 = when (currentJoint) {
                is VSRevoluteJoint -> normalizeJointShipIdTreatingGroundAsNull(currentJoint.shipId1)
                is VSFixedJoint -> normalizeJointShipIdTreatingGroundAsNull(currentJoint.shipId1)
                else -> null
            }
            val expectedMainNormalized = normalizeJointShipIdTreatingGroundAsNull(expectedMainShipId)
            val (subPose, mainPose) = when {
                shipId0 == subShipId && (!expectedMainShipIdKnown || shipId1 == expectedMainNormalized) -> when (currentJoint) {
                    is VSRevoluteJoint -> currentJoint.pose0 to currentJoint.pose1
                    is VSFixedJoint -> currentJoint.pose0 to currentJoint.pose1
                    else -> throw AssertionError()
                }
                shipId1 == subShipId && (!expectedMainShipIdKnown || shipId0 == expectedMainNormalized) -> when (currentJoint) {
                    is VSRevoluteJoint -> currentJoint.pose1 to currentJoint.pose0
                    is VSFixedJoint -> currentJoint.pose1 to currentJoint.pose0
                    else -> throw AssertionError()
                }
                else -> when (currentJoint) {
                    is VSRevoluteJoint -> currentJoint.pose0 to currentJoint.pose1
                    is VSFixedJoint -> currentJoint.pose0 to currentJoint.pose1
                    else -> throw AssertionError()
                }
            }
            return JointRemovalSnapshot(
                subShipId = subShipId,
                mainShipId = expectedMainShipId,
                mainShipIdKnown = expectedMainShipIdKnown,
                rotationsKnown = true,
                subPos = Vector3d(subPose.pos),
                subRot = Quaterniond(subPose.rot),
                mainPos = Vector3d(mainPose.pos),
                mainRot = Quaterniond(mainPose.rot),
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

    private fun jointMatchesExpectedShipIds(joint: VSJoint, expected: JointRemovalSnapshot): Boolean {
        val (id0, id1) = when (joint) {
            is VSRevoluteJoint -> normalizeJointShipIdTreatingGroundAsNull(joint.shipId0) to
                normalizeJointShipIdTreatingGroundAsNull(joint.shipId1)
            is VSFixedJoint -> normalizeJointShipIdTreatingGroundAsNull(joint.shipId0) to
                normalizeJointShipIdTreatingGroundAsNull(joint.shipId1)
            else -> return false
        }
        val expectedMainId = normalizeJointShipIdTreatingGroundAsNull(expected.mainShipId)
        return if (expected.mainShipIdKnown) {
            (id0 == expected.subShipId && id1 == expectedMainId) ||
                (id1 == expected.subShipId && id0 == expectedMainId)
        } else {
            id0 == expected.subShipId || id1 == expected.subShipId
        }
    }

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
        if (expected.mainShipIdKnown
            && normalizeJointShipIdTreatingGroundAsNull(mainShipIdCandidate) != normalizeJointShipIdTreatingGroundAsNull(expected.mainShipId)
        ) return false

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
        this.groundBodyId = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId] ?: UNKNOWN_GROUND_BODY_ID
        this.driveWarmupTicks = 0
        this.joint = null
        this.jointID = -1
        this.shouldVerifyConnection = true
        this.isRunning = true
        this.lastStateChanged = ticks

        sendData()
        updateGeneratedRotation()
    }

    override fun remove() {
        val level = level
        if (level is ServerLevel && !level.isClientSide) {
            scheduleJointRemoval(level)
        }
        super.remove()
    }

    override fun destroy() {
        val level = level
        if (level is ServerLevel && !level.isClientSide) {
            scheduleJointRemoval(level)
        }
        super.destroy()
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
        groundBodyId = UNKNOWN_GROUND_BODY_ID
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
        followAngleSmoothInitialized = false
        rotationStallDetectTicks = 0
        rotationStallCooldownTicks = 0
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
            val serverLevel = level as ServerLevel
            groundBodyId = serverLevel.shipObjectWorld.dimensionToGroundBodyIdImmutable[serverLevel.dimensionId] ?: UNKNOWN_GROUND_BODY_ID
            if (isRunning && originalDirection == null && level!!.getBlockState(worldPosition).block is BearingBlock) {
                originalDirection = blockState.getValue(BearingBlock.FACING)
            }
            if (isRunning && shiptraptionID != NO_SHIPTRAPTION_ID) {
                // If the connected ship was removed (not just unloaded), close back up.
                if (serverLevel.shipObjectWorld.allShips.getById(shiptraptionID) == null) {
                    resetState()
                }
            }
            if (isRunning && mainShipId == UNKNOWN_MAIN_SHIP_ID) {
                val resolved = serverLevel.getShipManagingPos(worldPosition)?.id
                if (resolved != null) {
                    mainShipId = resolved
                } else if (!serverLevel.isChunkInShipyard(worldPosition.x shr 4, worldPosition.z shr 4)) {
                    // Only treat this as world-attached if this chunk isn't part of any shipyard. Otherwise keep
                    // waiting; assuming world-attached can create a ground joint at the wrong location and launch
                    // the ship when it finally finishes loading.
                    mainShipId = NO_SHIPTRAPTION_ID
                }
            } else if (isRunning && mainShipId == NO_SHIPTRAPTION_ID) {
                // If we ever persisted a wrong world-attached main id while the bearing is actually on a ship,
                // correct it here before physics creates a ground joint at the wrong location.
                if (serverLevel.isChunkInShipyard(worldPosition.x shr 4, worldPosition.z shr 4)) {
                    val resolved = serverLevel.getShipManagingPos(worldPosition)?.id
                    mainShipId = resolved ?: UNKNOWN_MAIN_SHIP_ID
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
        private const val UNKNOWN_GROUND_BODY_ID: Long = Long.MIN_VALUE + 1L
        private const val REVOLUTE_DRIVE_WARMUP_TICKS: Int = 5
        private const val FOLLOW_ANGLE_TARGET_ERROR_MIN_DEG: Double = 0.5
        private const val MAX_MOTOR_ACCEL: Double = 200.0
        private const val MAX_MOTOR_DELTA_OMEGA: Double = 1.0
        private const val MAX_APPLIED_TORQUE_MAG: Double = 1.0E14
        private const val GAME_TICK_SECONDS: Double = 1.0 / 20.0
        private const val RECONNECT_STORED_ANCHOR_MAX_DIST: Double = 0.25
        private const val JOINT_ANCHOR_INSET: Double = 0.01
        private const val COLLISION_ENABLE_PERIOD_TICKS: Int = 20

        private const val REVOLUTE_JOINT_COMPLIANCE: Double = 1e-10
        private const val REVOLUTE_JOINT_MAX_FORCE: Float = 1.0E12F
        private const val REVOLUTE_JOINT_MAX_TORQUE: Float = 1.0E15F
        private val REVOLUTE_JOINT_MAX_FORCE_TORQUE: VSJointMaxForceTorque =
            VSJointMaxForceTorque(REVOLUTE_JOINT_MAX_FORCE, REVOLUTE_JOINT_MAX_TORQUE)

        private const val FOLLOW_ANGLE_ROTATION_RESISTANCE_MULTIPLIER: Double = 1.0
        private const val FOLLOW_ANGLE_OMEGA_FROM_ANGLE_MULT: Double = 20.0
        private const val FOLLOW_ANGLE_OMEGA_ERROR_MULTIPLIER: Double = 120.0
        private const val FOLLOW_ANGLE_MAX_DELTA_OMEGA_MOVING: Double = 1.0
        private const val FOLLOW_ANGLE_MAX_DELTA_OMEGA_STOPPING: Double = 4.0

        private const val ROTATION_STALL_ACTUAL_OMEGA_EPS_RAD_S: Double = 0.02
        private const val ROTATION_STALL_RELATIVE_OMEGA_FACTOR: Double = 0.2
        private const val ROTATION_STALL_DETECT_TICKS: Int = 6
        private const val ROTATION_STALL_COOLDOWN_TICKS: Int = 6
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
        outTorque: Vector3d,
        subShip: PhysShip,
        mainShip: PhysShip?,
        axisGlobal: Vector3dc,
        omegaActual: Double,
        desiredAngularVelocity: Float,
        subLocalPos: Vector3dc,
        mainLocalPos: Vector3dc,
        dtSeconds: Double,
    ) {
        val omegaTarget = -desiredAngularVelocity.toDouble()
        val omegaError = omegaTarget - omegaActual * ClockworkConfig.SERVER.unlockedModeRotationResistanceMultiplier

        val effectiveInertia = getEffectiveAngularInertia(subShip, mainShip, axisGlobal, subLocalPos, mainLocalPos)
        if (effectiveInertia <= 0.0) {
            outTorque.zero()
            return
        }

        val accelCmd = (omegaError * ClockworkConfig.SERVER.unlockedModeOmegaErrorMultiplier)
            .coerceIn(-motorAccelCap(dtSeconds), motorAccelCap(dtSeconds))
        val torqueMag = effectiveInertia * accelCmd
        outTorque.set(axisGlobal).mul(torqueMag)
    }

    private fun getEffectiveAngularInertia(
        subShip: PhysShip,
        mainShip: PhysShip?,
        axisGlobal: Vector3dc,
        subLocalPos: Vector3dc,
        mainLocalPos: Vector3dc,
    ): Double {
        fun angularInertia(ship: PhysShip, localPos: Vector3dc): Double {
            ship.transform.shipToWorld.transformPosition(localPos, tmpGlobalPos)
            tmpOffset.set(tmpGlobalPos).sub(ship.transform.positionInWorld)
            tmpProj.set(axisGlobal).mul(axisGlobal.dot(tmpOffset))
            tmpOffsetPerp.set(tmpOffset).sub(tmpProj)
            ship.transform.shipToWorldRotation.transformInverse(axisGlobal, tmpAxisLocal)
            ship.momentOfInertia.transform(tmpAxisLocal, tmpMoiAxisLocal)
            return tmpMoiAxisLocal.dot(tmpAxisLocal) + tmpOffsetPerp.lengthSquared() * ship.mass
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
