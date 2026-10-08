package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.min

object WandRopePhysics {
    private const val SWING_ACCELERATION = 0.012
    private const val SWING_ASSIST_SPEED = 1.2

    /** Extra air control along the swing arc, without pushing outward against the rope. */
    fun accelerateSwing(position: Vec3, velocity: Vec3, anchor: Vec3, anchorVelocity: Vec3, input: Vec3): Vec3 {
        val radial = position.subtract(anchor)
        if (radial.lengthSqr() < 1e-8) return velocity
        val normal = radial.normalize()
        val horizontal = Vec3(input.x, 0.0, input.z)
        val movement = if (horizontal.lengthSqr() > 1.0) horizontal.normalize() else horizontal
        val tangent = movement.subtract(normal.scale(movement.dot(normal)))
        val strength = tangent.length()
        if (strength < 1e-8) return velocity
        val direction = tangent.scale(1.0 / strength)
        val along = velocity.subtract(anchorVelocity).dot(direction)
        // Limit only the added input assist; keep momentum from gravity and moving ships intact.
        val acceleration = min(SWING_ACCELERATION * strength, max(0.0, SWING_ASSIST_SPEED - along))
        return velocity.add(direction.scale(acceleration))
    }

    /** Remove only outward radial motion. Tangential motion remains available for swinging. */
    fun constrain(position: Vec3, velocity: Vec3, anchor: Vec3, anchorVelocity: Vec3, length: Double): Vec3? {
        val delta = position.subtract(anchor)
        val distance = delta.length()
        if (distance < 0.001 || distance < length - 0.15) return null
        val normal = delta.scale(1.0 / distance)
        val relative = velocity.subtract(anchorVelocity)
        val outward = max(0.0, relative.dot(normal))
        val correction = ((distance - length).coerceAtLeast(0.0) * 0.25).coerceAtMost(0.9)
        return velocity.subtract(normal.scale(outward + correction))
    }
}
