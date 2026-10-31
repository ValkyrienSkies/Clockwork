package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.world.phys.Vec3

object WandLinkTargeting {
    /** visibleRange ends at the first raycast block, so strands behind walls cannot be removed. */
    fun removalDistance(link: WandLinks.Link, eye: Vec3, direction: Vec3, visibleRange: Double,
                        target: WandAnchor?, a: Vec3?, b: Vec3?): Double? {
        fun matches(anchor: WandAnchor) = target != null && anchor.pos == target.pos && anchor.shipId == target.shipId
        val endpoint = if (matches(link.a) || matches(link.b)) visibleRange else null
        val strand = if (link.rope) {
            if (a != null && b != null) WandRopeCurve(a, b, link.length).pick(eye, direction, visibleRange)?.distance else null
        } else glueDistance(eye, direction, visibleRange, a, b)
        return listOfNotNull(endpoint, strand).minOrNull()
    }

    /** Small capsule around the glue. Sampling is bounded and only runs when breaking a link. */
    fun intersects(eye: Vec3, direction: Vec3, range: Double, a: Vec3?, b: Vec3?): Boolean =
        glueDistance(eye, direction, range, a, b) != null

    private fun glueDistance(eye: Vec3, direction: Vec3, range: Double, a: Vec3?, b: Vec3?): Double? {
        if (a == null || b == null) return null
        val delta = b.subtract(a)
        val count = (delta.length() * 8).toInt().coerceIn(1, 256)
        var nearest: Double? = null
        for (i in 0..count) {
            val p = a.lerp(b, i.toDouble() / count).subtract(eye)
            val along = p.dot(direction)
            if (along in 0.0..range && along < (nearest ?: Double.MAX_VALUE) &&
                p.subtract(direction.scale(along)).lengthSqr() < 0.3 * 0.3) nearest = along
        }
        return nearest
    }
}
