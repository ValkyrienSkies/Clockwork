package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.world.phys.Vec3
import kotlin.math.max

object WandRopePhysics {
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
