package org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data

import org.joml.Vector3dc
import org.valkyrienskies.core.apigame.joints.VSJointAndId

data class PhysBearingCreateData(
    val bearingPos: Vector3dc, // TODO useless
    val bearingAxis: Vector3dc,
    val bearingAngle: Double,
    val bearingRPM: Float,
    val locked: Boolean,
    val shiptraptionID: Long,
    val constraint: VSJointAndId,
)
