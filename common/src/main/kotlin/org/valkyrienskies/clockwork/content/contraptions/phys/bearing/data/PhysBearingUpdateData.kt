package org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data

import org.valkyrienskies.core.apigame.constraints.VSFixedOrientationConstraint
import org.valkyrienskies.core.apigame.constraints.VSHingeOrientationConstraint

data class PhysBearingUpdateData(
        val bearingAngle: Double,
        val bearingRPM: Float,
        val locked: Boolean,
        val hingeConstraint: VSHingeOrientationConstraint?,
        val angleConstraint: VSFixedOrientationConstraint?
)