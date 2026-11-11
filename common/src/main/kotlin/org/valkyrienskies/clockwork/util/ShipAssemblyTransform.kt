package org.valkyrienskies.clockwork.util

import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.internal.ships.VsiServerShip
import org.valkyrienskies.mod.common.vsCore

fun alignAssembledShip(ship: ServerShip, parent: ServerShip?, sourceAnchor: Vector3dc, shipAnchor: Vector3dc) {
    val center = Vector3d(ship.inertiaData.centerOfMass)
    val position = Vector3d(center).sub(shipAnchor).add(sourceAnchor)
    parent?.transform?.shipToWorld?.transformPosition(position)
    val movingParent = parent?.takeUnless { it.isStatic }
    val angularVelocity = movingParent?.angularVelocity ?: Vector3d()
    val velocity = if (movingParent == null) Vector3d() else {
        val parentCenter = movingParent.transform.shipToWorld.transformPosition(movingParent.inertiaData.centerOfMass, Vector3d())
        angularVelocity.cross(position.sub(parentCenter, Vector3d()), Vector3d()).add(movingParent.velocity)
    }
    val transform = vsCore.newBodyTransform(position, parent?.transform?.shipToWorldRotation ?: Quaterniond(),
        parent?.transform?.scaling ?: Vector3d(1.0), center)
    (ship as VsiServerShip).unsafeSetKinematics(vsCore.newBodyKinematics(velocity, angularVelocity, transform))
}
