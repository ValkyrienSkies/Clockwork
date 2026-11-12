package org.valkyrienskies.clockwork.content.schematics

import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.internal.joints.VSJoint
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.mod.common.schematic.VdexConstraintEntry
import org.valkyrienskies.core.internal.joints.VSFixedJoint
import org.valkyrienskies.core.internal.joints.VSDistanceJoint
import org.valkyrienskies.core.internal.joints.VSSpringJoint
import org.valkyrienskies.core.internal.joints.VSPrismaticJoint
import org.valkyrienskies.core.internal.joints.VSSphericalJoint
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint
import org.valkyrienskies.core.internal.joints.VSGearJoint
import org.valkyrienskies.core.internal.joints.VSRackAndPinionJoint
import org.valkyrienskies.core.internal.joints.VSD6Joint

object VdexBuildPhysics {
    fun remap(entry: VdexConstraintEntry, ships: List<ServerShip>): VSJoint {
        val joint = entry.joint
        val a = ships[entry.metadata.shipIndex0]
        val b = ships[entry.metadata.shipIndex1]
        val p0 = VSJointPose(a.shipAABB!!.center(Vector3d()).add(entry.metadata.position0offset), joint.pose0.rot)
        val p1 = VSJointPose(b.shipAABB!!.center(Vector3d()).add(entry.metadata.position1offset), joint.pose1.rot)
        return when (joint) {
            is VSFixedJoint -> joint.copy(shipId0 = a.id, shipId1 = b.id, pose0 = p0, pose1 = p1)
            is VSDistanceJoint -> joint.copy(shipId0 = a.id, shipId1 = b.id, pose0 = p0, pose1 = p1)
            is VSSpringJoint -> joint.copy(shipId0 = a.id, shipId1 = b.id, pose0 = p0, pose1 = p1)
            is VSPrismaticJoint -> joint.copy(shipId0 = a.id, shipId1 = b.id, pose0 = p0, pose1 = p1)
            is VSSphericalJoint -> joint.copy(shipId0 = a.id, shipId1 = b.id, pose0 = p0, pose1 = p1)
            is VSRevoluteJoint -> joint.copy(shipId0 = a.id, shipId1 = b.id, pose0 = p0, pose1 = p1)
            is VSGearJoint -> joint.copy(shipId0 = a.id, shipId1 = b.id, pose0 = p0, pose1 = p1)
            is VSRackAndPinionJoint -> joint.copy(shipId0 = a.id, shipId1 = b.id, pose0 = p0, pose1 = p1)
            is VSD6Joint -> joint.copy(shipId0 = a.id, shipId1 = b.id, pose0 = p0, pose1 = p1)
            else -> error("Unsupported schematic joint")
        }
    }
}
