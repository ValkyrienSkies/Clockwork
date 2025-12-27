package org.valkyrienskies.clockwork.content.contraptions.propeller.copter

import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import net.createmod.catnip.math.VecHelper
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.contraption.CopterContraptionEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.contraption.PropellerContraption
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropUpdateData
import org.valkyrienskies.clockwork.util.MathFunctions
import org.valkyrienskies.clockwork.util.PIDQuaternion
import org.valkyrienskies.clockwork.util.PIDstance
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.api.toMinecraft
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp

class CopterBearingBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : PropellerBearingBlockEntity(type, pos,
    state, brass = true
), BlockEntityPhysicsListener {

    override var dimension: DimensionId
        get() = level!!.dimensionId
        set(value) { }

    val facing: Direction
        get() = blockState.getValue(BlockStateProperties.FACING)

    @Volatile
    var tiltVector: Vec3 = Vec3(0.0, 1.0, 0.0)
    @Volatile
    var targetTiltVector: Vec3 = tiltVector

    var clientTargetTiltVector: Vec3 = Vec3(0.0, 1.0, 0.0)

    @Volatile
    private var tiltCooldown: Int = 0

    @Volatile
    var thrustDirection: Vec3 = tiltVector

    @Volatile
    var tiltQuaternion: Quaternionf = Quaternionf(0f, 0f, 0f, 1f)
    @Volatile
    var targetTiltQuaternion: Quaternionf = Quaternionf(0f, 0f, 0f, 1f)

    var clientTargetTiltQuat: Quaternionf = Quaternionf(0f, 0f, 0f, 1f)
    var clientTiltQuat: Quaternionf = Quaternionf(0f, 0f, 0f, 1f)

    @Volatile
    var blockNormalVector: Vec3? = null

    private var iAngle = 0.0           // integrates angle error (radians * seconds)
    private var lastErrAxis = Vector3d(0.0, 0.0, 0.0)

    init {
        tiltQuaternion.normalize()
        targetTiltQuaternion.normalize()
    }


    private fun lerpTarget() {
        if (stopping) {
            tiltVector = VecHelper.lerp((disassemblyProgress / totalDisassemblyTime).toFloat(), blockNormalVector, tiltVector)
            // Update other variables based on the new tiltVector
            thrustDirection = tiltVector
            tiltQuaternion = MathFunctions.quatFromVecRot(blockNormalVector!!, tiltVector)
            return
        }

        val a = 1.0 - exp(-(1.0/60.0) / 0.25)

        // Smooth rotation
        var resultQuat = tiltQuaternion

        val delta = Quaterniond(tiltQuaternion).conjugate().mul(Quaterniond(targetTiltQuaternion))  // current^-1 * target
        delta.normalize()

        // Convert delta to axis-angle
        val angle = 2.0 * kotlin.math.acos(delta.w.coerceIn(-1.0, 1.0))
        if (angle < 1e-9) {
            resultQuat = Quaternionf(targetTiltQuaternion)
        } else {
            val maxRadPerSec = Math.toRadians(120.0)
            val maxStep = maxRadPerSec * (1.0/60.0)
            val t = (maxStep / angle).coerceIn(0.0, 1.0)

            resultQuat = (tiltQuaternion).slerp(targetTiltQuaternion, t.toFloat(), Quaternionf())
        }


        // Drive vectors from the quat (avoid lerp of vectors)
        val n = Vector3d(blockNormalVector!!.x, blockNormalVector!!.y, blockNormalVector!!.z).normalize()
        val v = Vector3d(n).rotate(Quaterniond(resultQuat)).normalize()

        tiltVector = Vec3(v.x, v.y, v.z)
        thrustDirection = tiltVector
        tiltQuaternion = resultQuat
    }


    fun setTiltTarget(target: Vec3, serverSide: Boolean = false) {
        if (serverSide) {
            val direction: Direction = facing
            blockNormalVector = Vec3(direction.stepX.toDouble(), direction.stepY.toDouble(), direction.stepZ.toDouble())

            val clampedTiltVector = MathFunctions.clampVecIntoCone(target, blockNormalVector!!, Math.toRadians(24.0))

            targetTiltVector = clampedTiltVector
            targetTiltQuaternion = MathFunctions.quatFromVecRot(blockNormalVector!!, targetTiltVector)
        } else {
            val direction: Direction = blockState.getValue(BlockStateProperties.FACING)
            blockNormalVector = Vec3(direction.stepX.toDouble(), direction.stepY.toDouble(), direction.stepZ.toDouble())

            val clampedTiltVector = MathFunctions.clampVecIntoCone(target, blockNormalVector!!, Math.toRadians(24.0))

            clientTargetTiltVector = clampedTiltVector

            clientTargetTiltQuat = MathFunctions.quatFromVecRot(blockNormalVector!!, clientTargetTiltVector)
        }
    }

    private fun computeStabilizedTarget(
        blockAxis: Vector3d,          // blockNormalVector normalized (ship local)
        desiredUp: Vector3d,           // worldUp rotated into ship local, normalized
        omegaShipLocal: Vector3d,      // ship angular velocity in ship local (rad/s)
        kp: Double,
        ki: Double,
        kd: Double,
        dt: Double,
        iClampRad: Double = Math.toRadians(6.0),   // max integral contribution in "angle units"
        integrateBelowRad: Double = Math.toRadians(12.0),
        leakPerSec: Double = 0.5
    ): Vector3d {
        val maxStep = Math.toRadians(120.0) * dt                   // rad

        val currentAxis = Vector3d(blockAxis).normalize()

        // Error axis points in the direction we need to rotate currentAxis to match desiredUp
        val errAxis = Vector3d(currentAxis).cross(desiredUp)
        val sinErr = errAxis.length()

        if (sinErr < 1e-9) {
            // decay integral when essentially aligned
            iAngle *= exp(-leakPerSec * dt)
            return desiredUp
        }

        errAxis.div(sinErr) // normalize error axis

        // Approx angle error (small-angle ok; for larger angles use atan2)
        val cosErr = currentAxis.dot(desiredUp).coerceIn(-1.0, 1.0)
        val errAngle = atan2(sinErr, cosErr) // [0..pi]

        // Damping: oppose angular velocity around the same correction axis
        val angRateAlongErr = omegaShipLocal.dot(errAxis)

        // --- Anti-windup gating ---
        // Estimate whether we'd saturate without I
        val uPD = kp * errAngle - kd * angRateAlongErr
        val stepPD = uPD * dt
        val wouldSaturate = abs(stepPD) >= maxStep * 0.999

        val nearEnough = errAngle < integrateBelowRad

        // leak integral always
        iAngle *= exp(-leakPerSec * dt)

        // integrate only when close and not saturated
        if (nearEnough && !wouldSaturate) {
            iAngle += errAngle * dt
            // clamp integral state (prevents windup)
            iAngle = iAngle.coerceIn(-iClampRad, iClampRad)
        }

        val commandedRate = kp * errAngle + ki * iAngle - kd * angRateAlongErr   // rad/s
        var step = commandedRate * dt                              // rad

        step = step.coerceIn(-maxStep, maxStep)

        // Apply a small rotation step around errAxis toward desiredUp
        val qStep = Quaterniond().fromAxisAngleRad(errAxis.x, errAxis.y, errAxis.z, step)
        return Vector3d(currentAxis).rotate(qStep).normalize()
    }

    override fun physTick(
        physShip: PhysShip?,
        physLevel: PhysLevel
    ) {
        if (physShip == null) return
        if (!this.running) return

        val invRotation = physShip.transform.shipToWorldRotation.invert(Quaterniond())
        //val modifiedInvRotation = Quaterniond(invRotation.x, -invRotation.y, invRotation.z, invRotation.w)
        //val localTarget = MathFunctions.rotateVecWithQuat(facing.normal.toJOMLD().mul(getDirectionScale().toDouble()).toMinecraft(), invRotation)
        val desiredLocal = Vector3d(0.0, 1.0, 0.0).rotate(invRotation).mul(getDirectionScale().toDouble()).normalize() // or negate if needed
        val blockAxis = tiltVector.toJOML().normalize() // .rotate(physShip.transform.shipToWorldRotation)

        // You need ω in ship-local (rad/s). If you only have world ω, rotate it by invShipRot too.
        val omegaLocal = Vector3d(physShip.angularVelocity)
        .rotate(invRotation)

        val stabilized = computeStabilizedTarget(
            blockAxis = blockAxis,
            desiredUp = desiredLocal,
            omegaShipLocal = omegaLocal,
            kp = 4.0,      // start 3..10
            ki = 0.8,
            kd = 4.0,      // start 1..6
            dt = 1.0/60.0
        )

        setTiltTarget(Vec3(stabilized.x, stabilized.y, stabilized.z), true)
        lerpTarget()

        tiltCooldown++
    }

    override fun tick() {
        super.tick()
        if (this.level == null) return

        if (propellerContraption is CopterContraptionEntity) {
            val copterContraption = propellerContraption as CopterContraptionEntity
            copterContraption.tiltQuaternion = clientTiltQuat
            copterContraption.superDirection = blockState.getValue(BlockStateProperties.FACING)
        }

        if (level!!.isClientSide) return clientTick()
        if (running) {
            if (level is ServerLevel && !isVirtual) {
                val ship = (level as ServerLevel).getLoadedShipManagingPos(
                    blockPos
                )
                if (ship != null) {
                    // lerpTarget()
                }
            }
        }
    }

    override fun assemble() {
        super.assemble()
        iAngle = 0.0
    }

    override fun disassemble() {
        super.disassemble()
        iAngle = 0.0
    }

    private fun clientTick() {
        if (level!!.isClientSide) {
            val ship = (level as ClientLevel).getLoadedShipManagingPos(
                blockPos
            )
            if (ship != null && this.running) {
                val invRotation = ship.renderTransform.shipToWorldRotation.invert(Quaterniond())
                //val modifiedInvRotation = Quaterniond(invRotation.x, -invRotation.y, invRotation.z, invRotation.w)

                //val localTarget = MathFunctions.rotateVecWithQuat(facing.normal.toJOMLD().mul(getDirectionScale().toDouble()).toMinecraft(), invRotation)

                val desiredLocal = Vector3d(facing.normal.toJOMLD()).rotate(invRotation).mul(getDirectionScale().toDouble()) // or negate if needed

                val trueTarget = if (stopping) {
                    VecHelper.lerp((disassemblyProgress / totalDisassemblyTime).toFloat(), blockNormalVector, clientTargetTiltVector)
                } else {
                    Vec3(desiredLocal.x, desiredLocal.y, desiredLocal.z)
                }

                setTiltTarget(trueTarget)
            } else {
                setTiltTarget(blockNormalVector ?: Vec3(0.0, 1.0, 0.0))
            }
        }
    }

    override fun createContraptionEntity(contraption: PropellerContraption): ControlledContraptionEntity {
        return CopterContraptionEntity.create(level, this, contraption)
    }

    fun getDirectionScale(): Float {
        var speed = getSpeed()
        if (speed == 0f) {
            return 1f
        }
        val facing = blockState.getValue(BlockStateProperties.FACING)
        speed = convertToDirection(speed, facing)
        if (rotationDirection.value == 1) {
            speed *= -1f
        }
        return if (speed > 0) 1f else -1f
    }

    override fun newUpdateData(): PropUpdateData {
        return PropUpdateData(currentOmega, angle, isInverted(), active, blades, tiltVector.toJOML())
    }

}
