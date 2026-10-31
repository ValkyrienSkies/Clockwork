package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.util.AABBHelper.mergeAdjacentFast
import org.valkyrienskies.clockwork.util.AABBHelper.subtractWithAABB

object WandSelectionMath {
    fun contains(box: AABBic, x: Int, y: Int, z: Int) = x >= box.minX() && x < box.maxX() &&
        y >= box.minY() && y < box.maxY() && z >= box.minZ() && z < box.maxZ()
    fun volume(box: AABBic): Long = (box.maxX().toLong() - box.minX()).coerceAtLeast(0) *
        (box.maxY().toLong() - box.minY()).coerceAtLeast(0) * (box.maxZ().toLong() - box.minZ()).coerceAtLeast(0)
    /** Keep a disjoint union so overlapping selections cannot duplicate or resurrect blocks. */
    fun union(existing: List<AABBic>, box: AABBic): List<AABBic> = mergeAdjacentFast(existing.subtractWithAABB(box) + box)
    fun normalize(boxes: List<AABBic>): List<AABBic> = boxes.fold(emptyList()) { result, box -> union(result, box) }
}
