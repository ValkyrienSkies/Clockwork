package org.valkyrienskies.clockwork.util

import org.joml.Vector3dc
import org.valkyrienskies.core.internal.joints.VSJoint
import org.valkyrienskies.core.internal.joints.VSJointPose

fun VSJoint.hasFinitePoseData(): Boolean = pose0.hasFinitePoseData() && pose1.hasFinitePoseData()

fun VSJointPose.hasFinitePoseData(): Boolean {
    return pos.hasFiniteComponents() &&
        rot.x().isFinite() &&
        rot.y().isFinite() &&
        rot.z().isFinite() &&
        rot.w().isFinite()
}

fun Vector3dc.hasFiniteComponents(): Boolean = x().isFinite() && y().isFinite() && z().isFinite()
