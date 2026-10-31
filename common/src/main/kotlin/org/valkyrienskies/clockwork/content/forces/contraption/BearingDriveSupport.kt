package org.valkyrienskies.clockwork.content.forces.contraption

import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.impl.api_impl.config.ConfigPhysicsBackendType
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.core.internal.joints.*
import org.valkyrienskies.core.internal.world.VsiPhysLevel

internal object BearingDriveSupport {
    private data class Load(val inertia: Double, val anchored: Boolean, val bodies: Set<Long>)

    fun supportsAngularDrives(): Boolean =
        VSCoreConfig.SERVER.physics.physicsBackend != ConfigPhysicsBackendType.KRUNCH_CLASSIC

    fun effectiveInertia(level: VsiPhysLevel, jointId: Int, joint: VSJoint, axisWorld: Vector3dc): Double {
        if (!axisWorld.lengthSquared().isFinite() || axisWorld.lengthSquared() < 1e-12) return 0.0
        val a = joint.shipId0?.let(level::getShipById)
        val b = joint.shipId1?.let(level::getShipById)
        val anchor = a?.transform?.shipToWorld?.transformPosition(joint.pose0.pos, Vector3d())
            ?: Vector3d(joint.pose0.pos)
        val axis = Vector3d(axisWorld).normalize()
        val left = load(level, joint.shipId0, jointId, anchor, axis)
        val right = load(level, joint.shipId1, jointId, anchor, axis)
        val overlap = left.bodies.any(right.bodies::contains) || (left.anchored && right.anchored)
        val inertiaA = if (overlap) inertia(a, anchor, axis) else left.inertia
        val inertiaB = if (overlap) inertia(b, anchor, axis) else right.inertia
        val fixedA = if (overlap) a == null || a.isStatic else left.anchored
        val fixedB = if (overlap) b == null || b.isStatic else right.anchored
        val inverse = (if (fixedA || inertiaA <= 0.0) 0.0 else 1.0 / inertiaA) +
            (if (fixedB || inertiaB <= 0.0) 0.0 else 1.0 / inertiaB)
        return if (inverse > 0.0 && inverse.isFinite()) 1.0 / inverse else 0.0
    }

    private fun load(level: VsiPhysLevel, start: Long?, excludedJoint: Int, anchor: Vector3dc, axis: Vector3dc): Load {
        if (start == null) return Load(0.0, true, emptySet())
        val visited = HashSet<Long>()
        val queue = ArrayDeque<Long>()
        queue.add(start)
        var sum = 0.0
        var anchored = false
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (!visited.add(id)) continue
            val ship = level.getShipById(id) ?: continue
            if (ship.isStatic) {
                anchored = true
                continue
            }
            sum += inertia(ship, anchor, axis)
            for (jointId in level.getJointsFromShip(id)) {
                if (jointId == excludedJoint) continue
                val joint = level.getJointById(jointId) ?: continue
                if (!supportsLoad(joint)) continue
                val other = if (joint.shipId0 == id) joint.shipId1 else joint.shipId0
                if (other == null) anchored = true else if (other !in visited) queue.add(other)
            }
        }
        return Load(sum, anchored, visited)
    }

    private fun supportsLoad(joint: VSJoint): Boolean = when (joint) {
        is VSFixedJoint, is VSRevoluteJoint, is VSSphericalJoint -> true
        is VSD6Joint -> listOf(VSD6Joint.D6Axis.X, VSD6Joint.D6Axis.Y, VSD6Joint.D6Axis.Z).all {
            (joint.motions?.get(it) ?: VSD6Joint.D6Motion.LOCKED) == VSD6Joint.D6Motion.LOCKED
        }
        else -> false
    }

    private fun inertia(ship: PhysShip?, anchor: Vector3dc, axis: Vector3dc): Double {
        if (ship == null || ship.isStatic) return 0.0
        val localAxis = ship.transform.shipToWorldRotation.transformInverse(axis, Vector3d())
        val offset = Vector3d(anchor).sub(ship.transform.positionInWorld)
        val perpendicular = offset.sub(Vector3d(axis).mul(offset.dot(axis)), Vector3d())
        val value = ship.momentOfInertia.transform(localAxis, Vector3d()).dot(localAxis) +
            ship.mass * perpendicular.lengthSquared()
        return if (value.isFinite() && value > 0.0) value else 0.0
    }

    fun gimbalTarget(joint: VSJoint, hostRotation: Quaterniondc, targetRotation: Quaterniondc): Quaterniond =
        Quaterniond(targetRotation).mul(joint.pose0.rot).invert()
            .mul(hostRotation).mul(joint.pose1.rot).normalize()

    fun clearDrive(level: VsiPhysLevel, jointId: Int): Boolean {
        val current = level.getJointById(jointId) as? VSD6Joint ?: return true
        if (current.drives == null) return true
        val updated = current.copy(drives = null, drivePosition = null, driveVelocity = null)
        updated.shouldBeSerialized = current.shouldBeSerialized
        return level.updateJoint(jointId, updated)
    }
    fun finiteGain(value: Double): Float = if (value.isFinite())
        value.coerceIn(0.0, Float.MAX_VALUE.toDouble()).toFloat() else 0f
}
