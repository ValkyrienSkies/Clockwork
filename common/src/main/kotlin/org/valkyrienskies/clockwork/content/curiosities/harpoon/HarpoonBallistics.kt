package org.valkyrienskies.clockwork.content.curiosities.harpoon

import org.joml.Vector3d
import org.joml.Vector3dc

object HarpoonBallistics {
    const val RADIUS = 0.125
    const val MAX_RANGE = 128.0
    const val LIFETIME_TICKS = 200L

    /** VS velocities are metres/second. Include rotation at the muzzle, not just centre-of-mass motion. */
    fun launchVelocity(direction: Vector3dc, charge: Double, linear: Vector3dc = Vector3d(),
                       angular: Vector3dc = Vector3d(), lever: Vector3dc = Vector3d()): Vector3d =
        Vector3d(direction).normalize().mul(10.0 + 50.0 * charge.coerceIn(0.0, 1.0))
            .add(angular.cross(lever, Vector3d())).add(linear)
}
