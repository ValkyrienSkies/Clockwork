package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.valkyrienskies.core.internal.joints.*
import java.util.EnumMap
import kotlin.math.max

/** SI units for ship joints; player velocities arrive in metres per game tick. */
internal object WandLinkPhysics {
    const val GLUE_RAMP_TICKS = 60
    const val ROPE_STIFFNESS = 625.0
    const val ROPE_DAMPING = 40.0
    const val ROPE_BREAK_ACCELERATION = 1050.0
    const val GLUE_STIFFNESS = 144.0
    const val GLUE_DAMPING = 20.0
    // Glue tolerates more tension than rope without becoming stiffer or pulling harder.
    const val GLUE_BREAK_ACCELERATION = 2100.0
    const val GLUE_BREAK_TORQUE = 840.0
    const val GLUE_MAX_STRETCH = 6.0
    const val GRAPPLE_BREAK_FORCE = 48000.0

    /** The world is a valid fixed body, but glue must connect two different bodies. */
    fun canAttach(a: WandAnchor, b: WandAnchor): Boolean =
        (a.shipId >= 0 || b.shipId >= 0) && a.shipId != b.shipId

    fun ramp(ticks: Int): Double {
        val t = (ticks.toDouble() / GLUE_RAMP_TICKS).coerceIn(0.0, 1.0)
        return t * t * (3 - 2 * t)
    }

    fun effectiveMass(a: Double?, b: Double?): Double = when {
        a == null -> b ?: 100.0
        b == null -> a
        else -> a * b / (a + b).coerceAtLeast(1.0)
    }.coerceIn(100.0, 25000.0)

    /** Both frames' X axes point out of A and into B. Keep the closest existing twist. */
    fun gluePoses(a: WandAnchor, b: WandAnchor, rotationA: Quaterniondc, rotationB: Quaterniondc): Pair<VSJointPose, VSJointPose> {
        val nA = Vector3d(a.face.stepX.toDouble(), a.face.stepY.toDouble(), a.face.stepZ.toDouble())
        val nB = Vector3d(-b.face.stepX.toDouble(), -b.face.stepY.toDouble(), -b.face.stepZ.toDouble())
        val frameA = Quaterniond().rotationTo(Vector3d(1.0, 0.0, 0.0), nA)
        val relative = Quaterniond(rotationB).invert().mul(rotationA)
        val frameB = Quaterniond().rotationTo(relative.transform(nA, Vector3d()), nB).mul(relative).mul(frameA)
        fun atFace(anchor: WandAnchor, q: Quaterniond): VSJointPose = VSJointPose(
            Vector3d(anchor.pos.x + 0.5 + anchor.face.stepX * 0.5,
                anchor.pos.y + 0.5 + anchor.face.stepY * 0.5, anchor.pos.z + 0.5 + anchor.face.stepZ * 0.5), q)
        return atFace(a, frameA) to atFace(b, frameB)
    }

    fun glueJoint(a: Long, b: Long, poseA: VSJointPose, poseB: VSJointPose, mass: Double, age: Int): VSD6Joint {
        val strength = ramp(age)
        val motions = EnumMap<VSD6Joint.D6Axis, VSD6Joint.D6Motion>(VSD6Joint.D6Axis::class.java)
        VSD6Joint.D6Axis.values().forEach { motions[it] = VSD6Joint.D6Motion.FREE }
        val drives = EnumMap<VSD6Joint.D6Drive, VSD6Joint.D6JointDrive>(VSD6Joint.D6Drive::class.java)
        for (axis in listOf(VSD6Joint.D6Drive.X, VSD6Joint.D6Drive.Y, VSD6Joint.D6Drive.Z)) {
            drives[axis] = VSD6Joint.D6JointDrive((mass * GLUE_STIFFNESS * strength).toFloat(),
                (mass * GLUE_DAMPING * strength).toFloat(), (mass * 24 * strength).toFloat())
        }
        drives[VSD6Joint.D6Drive.SLERP] = VSD6Joint.D6JointDrive((mass * 36 * strength).toFloat(),
            (mass * 10 * strength).toFloat(), (mass * 12 * strength).toFloat())
        return VSD6Joint(a, poseA, b, poseB, VSJointMaxForceTorque((mass * GLUE_BREAK_ACCELERATION).toFloat(), (mass * GLUE_BREAK_TORQUE).toFloat()),
            motions = motions, drives = drives, drivePosition = VSD6Joint.DrivePosition(VSJointPose(Vector3d(), Quaterniond())))
    }

    /** Also retire the saved record on solvers which don't publish native joint-break callbacks. */
    fun overloaded(rope: Boolean, stretch: Double, separatingSpeed: Double, age: Int): Boolean {
        if (rope) {
            // A rope carries no tension until its slack has been taken up.
            if (stretch < 0.0) return false
        } else {
            if (age < GLUE_RAMP_TICKS) return false
            // The initial alignment can start farther away; settled glue has a firm reach limit.
            if (stretch > GLUE_MAX_STRETCH) return true
        }
        val demand = max(0.0, stretch) * (if (rope) ROPE_STIFFNESS else GLUE_STIFFNESS) +
            max(0.0, separatingSpeed) * (if (rope) ROPE_DAMPING else GLUE_DAMPING)
        return demand > if (rope) ROPE_BREAK_ACCELERATION else GLUE_BREAK_ACCELERATION
    }

    fun grappleOverloaded(position: Vec3, velocity: Vec3, anchor: Vec3, anchorVelocity: Vec3, length: Double): Boolean {
        val delta = position.subtract(anchor)
        val distance = delta.length()
        if (distance < length - 0.15 || distance < 0.001) return false
        val relative = velocity.subtract(anchorVelocity)
        val radial = relative.dot(delta.scale(1 / distance))
        val tangentSquared = max(0.0, relative.lengthSqr() - radial * radial)
        // 80 kg player, radial arrest plus centripetal load; ordinary gravity and reeling have headroom.
        val force = 80 * (max(0.0, radial) * 400 + max(0.0, distance - length) * 100 + tangentSquared * 400 / max(2.0, length))
        return force > GRAPPLE_BREAK_FORCE
    }
}
