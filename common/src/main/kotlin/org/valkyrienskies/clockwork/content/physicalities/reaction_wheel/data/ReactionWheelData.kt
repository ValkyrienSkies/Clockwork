package org.valkyrienskies.clockwork.content.physicalities.reaction_wheel.data

import org.joml.Vector3d
import org.joml.Vector3dc

class ReactionWheelData(
    val wheelPos: Vector3dc,
    val wheelAxis: Vector3dc,
    var wheelSpeed: Double,
    private val spinup: Boolean,
    private val spindown: Boolean,
    var active: Boolean,
    var sourceSpeed: Double
) {
    var prevAngMomentum: Vector3dc = Vector3d()
}
