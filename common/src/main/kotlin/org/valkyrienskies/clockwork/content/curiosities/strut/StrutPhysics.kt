package org.valkyrienskies.clockwork.content.curiosities.strut

import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotItem
import org.valkyrienskies.clockwork.content.curiosities.spring.SpringJoints
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.core.internal.joints.VSFixedJoint
import org.valkyrienskies.core.internal.joints.VSJointMaxForceTorque
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.round

class StrutItem(properties: Properties) : RopeKnotItem(properties)

internal object StrutPhysics {
    const val TURN_DEGREES = 2.5
    private const val BREAK_ACCELERATION = 8000.0
    private const val LINEAR_ERROR = 0.25
    private val angularError = Math.toRadians(12.0)

    private fun Vec3.vector() = Vector3d(x, y, z)
    private fun rotation(level: Level, anchor: WandAnchor) = Quaterniond(
        level.shipObjectWorld.loadedShips.getById(anchor.shipId)?.transform?.shipToWorldRotation ?: Quaterniond())

    fun poses(level: ServerLevel, a: WandAnchor, b: WandAnchor): Pair<VSJointPose, VSJointPose> {
        val target = b.world(level)!!.vector()
        level.shipObjectWorld.loadedShips.getById(a.shipId)?.worldToShip?.transformPosition(target)
        return VSJointPose(target, rotation(level, a).invert().mul(rotation(level, b))) to VSJointPose(b.local().vector(), Quaterniond())
    }

    fun mass(level: ServerLevel, link: WandLinks.Link): Double {
        val first = level.shipObjectWorld.loadedShips.getById(link.a.shipId)
        val second = level.shipObjectWorld.loadedShips.getById(link.b.shipId)
        return SpringJoints.effectiveMass(first?.inertiaData?.mass, second?.inertiaData?.mass,
            first?.isStatic == true, second?.isStatic == true)
    }

    fun joint(link: WandLinks.Link, a: Long, b: Long): VSFixedJoint {
        val force = link.mass * BREAK_ACCELERATION
        return VSFixedJoint(a, link.pose0, b, link.pose1, VSJointMaxForceTorque(force.toFloat(), (force * .75).toFloat()),
            compliance = LINEAR_ERROR / force)
    }

    fun stress(level: ServerLevel, link: WandLinks.Link, end: Vec3): Double {
        val desired = Vector3d(link.pose0.pos)
        level.shipObjectWorld.loadedShips.getById(link.a.shipId)?.shipToWorld?.transformPosition(desired)
        val angle = 2 * acos(abs(rotation(level, link.a).mul(link.pose0.rot)
            .dot(rotation(level, link.b).mul(link.pose1.rot))).coerceIn(0.0, 1.0))
        return maxOf(desired.distance(end.vector()) / LINEAR_ERROR, angle / angularError)
    }

    fun axis(level: Level, anchor: WandAnchor, look: Vec3): Direction {
        val local = rotation(level, anchor).transformInverse(look.vector())
        return Direction.getNearest(local.x, local.y, local.z)
    }

    fun nudge(level: ServerLevel, link: WandLinks.Link, axis: Direction, clicks: Int, rotate: Boolean) {
        fun scale(anchor: WandAnchor) = level.shipObjectWorld.loadedShips.getById(anchor.shipId)?.transform?.shipToWorldScaling ?: Vector3d(1.0)
        nudge(link, axis, clicks, rotate, scale(link.a), scale(link.b))
    }

    fun nudge(link: WandLinks.Link, axis: Direction, clicks: Int, rotate: Boolean,
              scaleA: Vector3dc = Vector3d(1.0), scaleB: Vector3dc = Vector3d(1.0)) {
        val unit = Vector3d(axis.stepX.toDouble(), axis.stepY.toDouble(), axis.stepZ.toDouble())
        val relative = Quaterniond(link.pose0.rot).div(link.pose1.rot)
        val mountOffset = Vector3d(link.pose1.pos).sub(Vec3.atCenterOf(link.b.pos).vector()).mul(scaleB)
        val inverseScale = Vector3d(1.0 / scaleA.x(), 1.0 / scaleA.y(), 1.0 / scaleA.z())
        val oldMount = relative.transform(Vector3d(mountOffset)).mul(inverseScale)
        if (rotate) {
            val cyclic = when (axis.axis) {
                Direction.Axis.X -> Quaterniond(relative.y, relative.z, relative.x, relative.w)
                Direction.Axis.Y -> Quaterniond(relative.z, relative.x, relative.y, relative.w)
                else -> relative
            }
            val angles = cyclic.getEulerAnglesZYX(Vector3d())
            if (abs(2 * (cyclic.w * cyclic.y - cyclic.z * cyclic.x)) > 1.0 - 1e-12) {
                angles.set(0.0, Math.copySign(Math.PI / 2, angles.y), 2 * atan2(cyclic.z, cyclic.w))
            }
            val step = Math.toRadians(TURN_DEGREES)
            angles.set(round(angles.x / step), round(angles.y / step),
                round(angles.z / step) + clicks * axis.axisDirection.step).mul(step)
            cyclic.rotationZYX(angles.z, angles.y, angles.x)
            val snapped = when (axis.axis) {
                Direction.Axis.X -> Quaterniond(cyclic.z, cyclic.x, cyclic.y, cyclic.w)
                Direction.Axis.Y -> Quaterniond(cyclic.y, cyclic.z, cyclic.x, cyclic.w)
                else -> cyclic
            }
            val newMount = snapped.transform(mountOffset).mul(inverseScale)
            link.pose0 = VSJointPose(Vector3d(link.pose0.pos).add(newMount.sub(oldMount)), snapped.mul(link.pose1.rot).normalize())
        } else {
            val origin = Vec3.atCenterOf(link.a.pos).vector().add(oldMount)
            val offset = Vector3d(link.pose0.pos).sub(origin)
            offset.set(round(offset.x * 16), round(offset.y * 16), round(offset.z * 16)).fma(clicks.toDouble(), unit).div(16.0)
            link.pose0 = link.pose0.copy(pos = offset.add(origin))
        }
    }

    fun resize(link: WandLinks.Link, length: Double) {
        val origin = link.a.local().vector()
        val delta = Vector3d(link.pose0.pos).sub(origin)
        if (delta.lengthSquared() < 1e-10) return
        link.pose0 = link.pose0.copy(pos = delta.mul(length / link.length).add(origin))
        link.length = length
    }

    fun targetLength(level: ServerLevel, link: WandLinks.Link): Double {
        val offset = Vector3d(link.pose0.pos).sub(link.a.local().vector())
        level.shipObjectWorld.loadedShips.getById(link.a.shipId)?.shipToWorld?.transformDirection(offset)
        return offset.length()
    }
}
