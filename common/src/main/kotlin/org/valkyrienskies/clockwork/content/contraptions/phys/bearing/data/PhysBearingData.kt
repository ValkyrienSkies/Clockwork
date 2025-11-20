package org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data

import org.joml.Vector3d
import org.joml.Vector3dc

data class PhysBearingData(
    val bearingAxis: Vector3dc? = null,
    var bearingAngle: Double = 0.0,
    var angularSpeed: Float = 0f,
    var angleFollowing: Boolean = false,
    var aligning: Boolean = false,

    var mainShipId: Long = -1,
    var mainPos: Vector3d = Vector3d(),
    var subPos: Vector3d = Vector3d(),
    var actualAngle: Double = 0.0,
)