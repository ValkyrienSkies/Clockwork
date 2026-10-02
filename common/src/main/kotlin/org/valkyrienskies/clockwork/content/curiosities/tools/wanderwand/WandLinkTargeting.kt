package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.world.phys.Vec3

object WandLinkTargeting {
    /** Small capsule around the glue. Sampling is bounded and only runs when breaking a link. */
    fun intersects(eye: Vec3, direction: Vec3, range: Double, a: Vec3?, b: Vec3?): Boolean {
        if (a == null || b == null) return false
        val delta = b.subtract(a)
        val count = (delta.length() * 8).toInt().coerceIn(1, 256)
        for (i in 0..count) {
            val p = a.lerp(b, i.toDouble() / count).subtract(eye)
            val along = p.dot(direction)
            if (along in 0.0..range && p.subtract(direction.scale(along)).lengthSqr() < 0.3 * 0.3) return true
        }
        return false
    }
}
