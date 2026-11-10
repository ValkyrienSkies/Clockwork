package org.valkyrienskies.clockwork.content.curiosities.spring

import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.valkyrienskies.core.internal.joints.VSD6Joint
import org.valkyrienskies.core.internal.joints.VSJointMaxForceTorque
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.core.internal.joints.VSSpringJoint
import java.util.EnumMap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max

internal object SpringJoints {
    private const val STIFFNESS = 350.0
    private const val DAMPING = 28.0
    private const val TWIST_STIFFNESS = 28.0
    private const val TWIST_DAMPING = 5.25
    private const val BREAK_ACCELERATION = 7200.0

    fun effectiveMass(a: Double?, b: Double?, fixedA: Boolean = false, fixedB: Boolean = false): Double {
        val first = a?.takeIf { !fixedA && it.isFinite() && it > 0 }
        val second = b?.takeIf { !fixedB && it.isFinite() && it > 0 }
        return (when {
            first == null -> second ?: 100.0
            second == null -> first
            else -> 1.0 / (1.0 / first + 1.0 / second)
        }).coerceIn(100.0, 1.0e9)
    }

    fun create(a: Long, b: Long, poseA: VSJointPose, poseB: VSJointPose, length: Double, mass: Double, settings: SpringSettings = SpringSettings()): Pair<VSSpringJoint, VSD6Joint> {
        val effectiveMass = mass.coerceIn(100.0, 1.0e9)
        val limits = VSJointMaxForceTorque((effectiveMass * BREAK_ACCELERATION).toFloat(), Float.MAX_VALUE)
        val spring = VSSpringJoint(a, poseA, b, poseB, limits,
            restLength = length.toFloat(), stiffness = (effectiveMass * STIFFNESS * settings.stiffness).toFloat(),
            damping = (effectiveMass * DAMPING * settings.damping).toFloat())
        val motions = EnumMap<VSD6Joint.D6Axis, VSD6Joint.D6Motion>(VSD6Joint.D6Axis::class.java)
        VSD6Joint.D6Axis.values().forEach { motions[it] = VSD6Joint.D6Motion.FREE }
        val drives = EnumMap<VSD6Joint.D6Drive, VSD6Joint.D6JointDrive>(VSD6Joint.D6Drive::class.java)
        if (settings.twist > 0) drives[VSD6Joint.D6Drive.TWIST] = VSD6Joint.D6JointDrive(
            (effectiveMass * TWIST_STIFFNESS * settings.twist).toFloat(), (effectiveMass * TWIST_DAMPING * settings.damping).toFloat(),
            (effectiveMass * 60.0).toFloat())
        val alignment = VSD6Joint(a, poseA, b, poseB, motions = motions, drives = drives,
            drivePosition = VSD6Joint.DrivePosition(VSJointPose(Vector3d(), Quaterniond())))
        return spring to alignment
    }

    fun twistAngle(frameA: Quaterniondc, frameB: Quaterniondc): Double {
        val relative = Quaterniond(frameA).invert().mul(frameB)
        if (relative.x * relative.x + relative.w * relative.w < 1e-12) return 0.0
        val x = if (relative.w < 0) -relative.x else relative.x
        return 2 * atan2(x, abs(relative.w))
    }

    fun stressRatio(extension: Double, speed: Double, settings: SpringSettings = SpringSettings()): Double =
        (abs(extension) * STIFFNESS * settings.stiffness + max(0.0, speed * kotlin.math.sign(extension)) * DAMPING * settings.damping) / BREAK_ACCELERATION

    fun overloaded(extension: Double, speed: Double, settings: SpringSettings = SpringSettings()): Boolean = stressRatio(extension, speed, settings) > 1.0
}
