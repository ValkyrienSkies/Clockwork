package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import org.joml.Quaterniondc
import org.joml.Vector3dc

data class GravitronForceInducerData(
    val idealPos: Vector3dc,
    val idealRot: Quaterniondc,
    val grabbedPos: Vector3dc,
    val supportedMass: Double = 0.0,
    val rigidMass: Double = 0.0,
    val connectedMass: Boolean = false,
    val portalTarget: PortalTarget? = null,
) {
    data class PortalTarget(val position: Vector3dc, val rotation: Quaterniondc,
                            val entranceAnchor: Vector3dc, val exitAnchor: Vector3dc)

    fun targetThroughPortal(anchor: Vector3dc) = portalTarget?.takeIf {
        it.exitAnchor.distanceSquared(anchor) < it.entranceAnchor.distanceSquared(anchor)
    }
}
