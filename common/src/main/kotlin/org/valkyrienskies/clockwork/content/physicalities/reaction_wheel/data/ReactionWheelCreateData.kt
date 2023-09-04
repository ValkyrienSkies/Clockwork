package org.valkyrienskies.clockwork.content.physicalities.reaction_wheel.data

import org.joml.Vector3dc
data class ReactionWheelCreateData(
    val wheelPos: Vector3dc,
    val wheelAxis: Vector3dc,
    val wheelSpeed: Float,
    val spinup: Boolean,
    val spindown: Boolean,
    val active: Boolean,
    val sourceSpeed: Float
)