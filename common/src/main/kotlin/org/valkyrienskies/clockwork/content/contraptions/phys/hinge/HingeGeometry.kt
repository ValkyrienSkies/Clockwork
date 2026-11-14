package org.valkyrienskies.clockwork.content.contraptions.phys.hinge

import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.core.internal.joints.VSD6Joint
import org.valkyrienskies.core.internal.joints.VSJointMaxForceTorque
import org.valkyrienskies.core.internal.joints.VSJointPose
import java.util.EnumMap
import kotlin.math.atan2

object HingeGeometry {
    const val MAX_FORCE = 100_000_000f
    const val MAX_TORQUE = 50_000_000f
    const val RECONNECT_DISTANCE = 2.5
    const val COMPLIANCE = 5e-9
    const val SETTLE_TICKS = 10
    const val BREAK_GRACE_TICKS = 40

    fun orientation(facing: Direction, roll: Int): Quaterniond {
        val result = when (facing) {
            Direction.UP -> Quaterniond()
            Direction.DOWN -> Quaterniond().rotationX(Math.PI)
            else -> Quaterniond().rotationY(-Math.toRadians(when (facing) {
                Direction.EAST -> 90.0
                Direction.SOUTH -> 180.0
                Direction.WEST -> 270.0
                else -> 0.0
            })).rotateX(-Math.PI / 2)
        }
        return result.rotateY(roll * Math.PI / 2)
    }

    fun orientation(state: BlockState) = orientation(state.getValue(HingeBlock.FACING), state.getValue(HingeBlock.ROLL))
    fun axis(state: BlockState): Direction = orientation(state).transform(Vector3d(1.0, 0.0, 0.0)).let {
        Direction.getNearest(it.x, it.y, it.z)
    }

    fun joint(a: Long?, b: Long?, poseA: VSJointPose, poseB: VSJointPose, lower: Int, upper: Int, age: Int = SETTLE_TICKS): VSD6Joint {
        val motions = EnumMap<VSD6Joint.D6Axis, VSD6Joint.D6Motion>(VSD6Joint.D6Axis::class.java)
        VSD6Joint.D6Axis.values().forEach { motions[it] = VSD6Joint.D6Motion.LOCKED }
        if (lower != 0 || upper != 0) motions[VSD6Joint.D6Axis.TWIST] = VSD6Joint.D6Motion.LIMITED
        return VSD6Joint(a, poseA, b, poseB, VSJointMaxForceTorque(Float.MAX_VALUE, Float.MAX_VALUE),
            compliance = compliance(age), motions = motions, twistLimit = if (lower == 0 && upper == 0) null else VSD6Joint.AngularLimitPair(
                Math.toRadians(lower.coerceIn(-180, 0).toDouble()).toFloat(),
                Math.toRadians(upper.coerceIn(0, 180).toDouble()).toFloat())).also { it.shouldBeSerialized = false }
    }

    fun compliance(age: Int): Double {
        val progress = (age.toDouble() / SETTLE_TICKS).coerceIn(0.0, 1.0)
        return COMPLIANCE + (1e-7 - COMPLIANCE) * (1.0 - progress) * (1.0 - progress)
    }

    fun angle(a: Quaterniond, b: Quaterniond): Double {
        val relative = Quaterniond(a).invert().mul(b)
        val sign = if (relative.w < 0) -1.0 else 1.0
        return Math.toDegrees(2 * atan2(relative.x * sign, relative.w * sign))
    }
}
