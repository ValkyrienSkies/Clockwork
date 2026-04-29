package org.valkyrienskies.clockwork.util

import org.valkyrienskies.core.internal.joints.VSJoint
import org.valkyrienskies.core.internal.joints.VSJointAndId
import org.valkyrienskies.core.internal.joints.VSFixedJoint
import org.valkyrienskies.core.internal.joints.VSJointId
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import org.valkyrienskies.mod.common.util.GameToPhysicsAdapter
import kotlin.math.abs

private const val JOINT_POSITION_EPSILON = 1.0e-3
private const val JOINT_ROTATION_DOT_EPSILON = 1.0e-5

fun VSJoint.matchesByAnchors(other: VSJoint): Boolean {
    if (this::class != other::class) {
        return false
    }

    return matchesDirectAnchors(other) || matchesSwappedAnchors(other)
}

fun GameToPhysicsAdapter.findMatchingJoint(target: VSJoint): VSJointAndId? =
    getAllJoints().findMatchingJoint(target)

fun GameToPhysicsAdapter.findMatchingJoints(target: VSJoint): List<VSJointAndId> =
    getAllJoints().findMatchingJoints(target)

fun GameToPhysicsAdapter.findMatchingJointIds(target: VSJoint): List<VSJointId> =
    getAllJoints().findMatchingJointIds(target)

fun VsiPhysLevel.findMatchingJoint(target: VSJoint): VSJointAndId? =
    getAllJoints().findMatchingJoint(target)

fun VsiPhysLevel.findMatchingJoints(target: VSJoint): List<VSJointAndId> =
    getAllJoints().findMatchingJoints(target)

fun VsiPhysLevel.findMatchingJointIds(target: VSJoint): List<VSJointId> =
    getAllJoints().findMatchingJointIds(target)

fun GameToPhysicsAdapter.findCompatiblePhysBearingJoint(target: VSJoint): VSJointAndId? =
    getAllJoints().findCompatiblePhysBearingJoint(target)

fun GameToPhysicsAdapter.findCompatiblePhysBearingJointIds(target: VSJoint): List<VSJointId> =
    getAllJoints().findCompatiblePhysBearingJointIds(target)

fun VsiPhysLevel.findCompatiblePhysBearingJoint(target: VSJoint): VSJointAndId? =
    getAllJoints().findCompatiblePhysBearingJoint(target)

fun VsiPhysLevel.findCompatiblePhysBearingJointIds(target: VSJoint): List<VSJointId> =
    getAllJoints().findCompatiblePhysBearingJointIds(target)

fun GameToPhysicsAdapter.removeCompatiblePhysBearingJointsExcept(target: VSJoint, keepJointId: VSJointId): Int {
    val duplicateIds = findCompatiblePhysBearingJointIds(target).filter { it != keepJointId }
    duplicateIds.forEach(::removeJoint)
    return duplicateIds.size
}

fun VsiPhysLevel.removeCompatiblePhysBearingJointsExcept(target: VSJoint, keepJointId: VSJointId): Int {
    val duplicateIds = findCompatiblePhysBearingJointIds(target).filter { it != keepJointId }
    duplicateIds.forEach(::removeJoint)
    return duplicateIds.size
}

fun GameToPhysicsAdapter.removeMatchingJointsExcept(target: VSJoint, keepJointId: VSJointId): Int {
    val duplicateIds = findMatchingJointIds(target).filter { it != keepJointId }
    duplicateIds.forEach(::removeJoint)
    return duplicateIds.size
}

fun VsiPhysLevel.removeMatchingJointsExcept(target: VSJoint, keepJointId: VSJointId): Int {
    val duplicateIds = findMatchingJointIds(target).filter { it != keepJointId }
    duplicateIds.forEach(::removeJoint)
    return duplicateIds.size
}

private fun Map<VSJointId, VSJoint>.findMatchingJoint(target: VSJoint): VSJointAndId? {
    return entries.firstOrNull { (_, joint) -> joint.matchesByAnchors(target) }
        ?.let { (jointId, joint) -> VSJointAndId(jointId, joint) }
}

private fun Map<VSJointId, VSJoint>.findCompatiblePhysBearingJoint(target: VSJoint): VSJointAndId? {
    return entries.firstOrNull { (_, joint) -> joint.matchesCompatiblePhysBearingJoint(target) }
        ?.let { (jointId, joint) -> VSJointAndId(jointId, joint) }
}

private fun Map<VSJointId, VSJoint>.findMatchingJoints(target: VSJoint): List<VSJointAndId> {
    return entries.asSequence()
        .filter { (_, joint) -> joint.matchesByAnchors(target) }
        .map { (jointId, joint) -> VSJointAndId(jointId, joint) }
        .toList()
}

private fun Map<VSJointId, VSJoint>.findMatchingJointIds(target: VSJoint): List<VSJointId> {
    return entries.asSequence()
        .filter { (_, joint) -> joint.matchesByAnchors(target) }
        .map { (jointId, _) -> jointId }
        .toList()
}

private fun Map<VSJointId, VSJoint>.findCompatiblePhysBearingJointIds(target: VSJoint): List<VSJointId> {
    return entries.asSequence()
        .filter { (_, joint) -> joint.matchesCompatiblePhysBearingJoint(target) }
        .map { (jointId, _) -> jointId }
        .toList()
}

private fun VSJoint.matchesDirectAnchors(other: VSJoint): Boolean {
    return shipId0 == other.shipId0 &&
        shipId1 == other.shipId1 &&
        pose0.matchesPose(other.pose0) &&
        pose1.matchesPose(other.pose1)
}

private fun VSJoint.matchesSwappedAnchors(other: VSJoint): Boolean {
    return shipId0 == other.shipId1 &&
        shipId1 == other.shipId0 &&
        pose0.matchesPose(other.pose1) &&
        pose1.matchesPose(other.pose0)
}

private fun VSJoint.matchesCompatiblePhysBearingJoint(other: VSJoint): Boolean {
    return isPhysBearingJointFamily() &&
        other.isPhysBearingJointFamily() &&
        (matchesDirectAnchors(other) || matchesSwappedAnchors(other))
}

private fun VSJoint.isPhysBearingJointFamily(): Boolean =
    this is VSFixedJoint || this is VSRevoluteJoint

private fun VSJointPose.matchesPose(other: VSJointPose): Boolean {
    return pos.distanceSquared(other.pos) <= JOINT_POSITION_EPSILON * JOINT_POSITION_EPSILON &&
        abs(rot.x() * other.rot.x() + rot.y() * other.rot.y() + rot.z() * other.rot.z() + rot.w() * other.rot.w()) >=
        1.0 - JOINT_ROTATION_DOT_EPSILON
}
