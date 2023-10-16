package org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data

import org.joml.Vector3dc
import org.valkyrienskies.core.apigame.constraints.VSConstraintAndId

data class PhysBearingCreateData(
    val bearingPos: Vector3dc,
    val bearingAxis: Vector3dc,
    val bearingAngle: Double,
    val bearingRPM: Float,
    val locked: Boolean,
    val shiptraptionID: Long,
    val constraint: VSConstraintAndId,
    val hingeConstraint: VSConstraintAndId,
    val posDampConstraint: VSConstraintAndId?,
    val rotDampConstraint: VSConstraintAndId?,
    val secondAttachment: VSConstraintAndId? = null,
)
