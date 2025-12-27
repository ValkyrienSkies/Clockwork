package org.valkyrienskies.clockwork.util

import org.joml.primitives.AABBi
import org.joml.primitives.AABBic

object AABBHelper {

    private fun isEmpty(b: AABBi): Boolean =
        b.minX == b.maxX || b.minY == b.maxY || b.minZ == b.maxZ

    private fun intersectionHalfOpen(a: AABBi, c: AABBi): AABBi? {
        val ix0 = maxOf(a.minX, c.minX)
        val iy0 = maxOf(a.minY, c.minY)
        val iz0 = maxOf(a.minZ, c.minZ)
        val ix1 = minOf(a.maxX, c.maxX)
        val iy1 = minOf(a.maxY, c.maxY)
        val iz1 = minOf(a.maxZ, c.maxZ)
        val i = AABBi(ix0, iy0, iz0, ix1, iy1, iz1)
        return if (isEmpty(i)) null else i
    }

    @JvmStatic
    fun AABBic.subtractWithAABB(subtraction: AABBic): List<AABBic> {
        val i = intersectionHalfOpen(this as AABBi, subtraction as AABBi) ?: return listOf(this)

        val result = ArrayList<AABBic>(6)
        fun addIfNonEmpty(a: AABBi) {
            if (!isEmpty(a)) result.add(a)
        }

        // Left / Right slabs (X)
        addIfNonEmpty(AABBi(this.minX, this.minY, this.minZ, i.minX, this.maxY, this.maxZ))
        addIfNonEmpty(AABBi(i.maxX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ))

        // Middle X-range
        val mx0 = i.minX
        val mx1 = i.maxX

        // Bottom / Top slabs (Y) within middle X
        addIfNonEmpty(AABBi(mx0, this.minY, this.minZ, mx1, i.minY, this.maxZ))
        addIfNonEmpty(AABBi(mx0, i.maxY, this.minZ, mx1, this.maxY, this.maxZ))

        // Middle X & Y-range
        val my0 = i.minY
        val my1 = i.maxY

        // Back / Front slabs (Z) within middle X,Y
        addIfNonEmpty(AABBi(mx0, my0, this.minZ, mx1, my1, i.minZ))
        addIfNonEmpty(AABBi(mx0, my0, i.maxZ, mx1, my1, this.maxZ))

        return result
    }

    @JvmStatic
    fun Iterable<AABBic>.subtractWithAABB(subtraction: AABBic): List<AABBic> {
        val out = ArrayList<AABBic>()
        for (a in this) out.addAll(a.subtractWithAABB(subtraction))
        return out
    }

    private fun pack2(a: Int, b: Int): Long =
        (a.toLong() shl 32) xor (b.toLong() and 0xFFFF_FFFFL)

    private data class Key2(val k1: Long, val k2: Long)

    //ts so vibecoded might as well credit chatgpt in the mod toml
    /**
     * Fast merge for voxel AABBs.
     *
     * Merges any two boxes that:
     * - touch exactly on one axis (acc.max == b.min),
     * - and have identical spans on the other two axes.
     *
     * Runs X/Y/Z passes until stable.
     */
    fun mergeAdjacentFast(boxes: Iterable<AABBic>): List<AABBic> {
        var cur = boxes.asSequence()
            .map { AABBi(it) }         // copy to avoid mutating caller's instances
            .filterNot(::isEmpty)
            .toList()

        fun mergePassX(input: List<AABBi>): Pair<List<AABBi>, Boolean> {
            val groups = HashMap<Key2, MutableList<AABBi>>(input.size * 2)
            for (b in input) {
                val key = Key2(pack2(b.minY, b.maxY), pack2(b.minZ, b.maxZ))
                groups.getOrPut(key) { ArrayList() }.add(b)
            }

            val out = ArrayList<AABBi>(input.size)
            var changed = false

            for (g in groups.values) {
                g.sortWith(compareBy<AABBi> { it.minX }.thenBy { it.maxX })
                var acc = g[0]
                for (i in 1 until g.size) {
                    val b = g[i]
                    if (acc.maxX == b.minX) {
                        // same Y/Z by construction (group key)
                        acc = AABBi(acc.minX, acc.minY, acc.minZ, b.maxX, acc.maxY, acc.maxZ)
                        changed = true
                    } else {
                        out.add(acc)
                        acc = b
                    }
                }
                out.add(acc)
            }
            return out to changed
        }

        fun mergePassY(input: List<AABBi>): Pair<List<AABBi>, Boolean> {
            val groups = HashMap<Key2, MutableList<AABBi>>(input.size * 2)
            for (b in input) {
                val key = Key2(pack2(b.minX, b.maxX), pack2(b.minZ, b.maxZ))
                groups.getOrPut(key) { ArrayList() }.add(b)
            }

            val out = ArrayList<AABBi>(input.size)
            var changed = false

            for (g in groups.values) {
                g.sortWith(compareBy<AABBi> { it.minY }.thenBy { it.maxY })
                var acc = g[0]
                for (i in 1 until g.size) {
                    val b = g[i]
                    if (acc.maxY == b.minY) {
                        acc = AABBi(acc.minX, acc.minY, acc.minZ, acc.maxX, b.maxY, acc.maxZ)
                        changed = true
                    } else {
                        out.add(acc)
                        acc = b
                    }
                }
                out.add(acc)
            }
            return out to changed
        }

        fun mergePassZ(input: List<AABBi>): Pair<List<AABBi>, Boolean> {
            val groups = HashMap<Key2, MutableList<AABBi>>(input.size * 2)
            for (b in input) {
                val key = Key2(pack2(b.minX, b.maxX), pack2(b.minY, b.maxY))
                groups.getOrPut(key) { ArrayList() }.add(b)
            }

            val out = ArrayList<AABBi>(input.size)
            var changed = false

            for (g in groups.values) {
                g.sortWith(compareBy<AABBi> { it.minZ }.thenBy { it.maxZ })
                var acc = g[0]
                for (i in 1 until g.size) {
                    val b = g[i]
                    if (acc.maxZ == b.minZ) {
                        acc = AABBi(acc.minX, acc.minY, acc.minZ, acc.maxX, acc.maxY, b.maxZ)
                        changed = true
                    } else {
                        out.add(acc)
                        acc = b
                    }
                }
                out.add(acc)
            }
            return out to changed
        }

        var anyChanged: Boolean
        do {
            anyChanged = false

            val (xMerged, cx) = mergePassX(cur)
            cur = xMerged
            anyChanged = anyChanged || cx

            val (yMerged, cy) = mergePassY(cur)
            cur = yMerged
            anyChanged = anyChanged || cy

            val (zMerged, cz) = mergePassZ(cur)
            cur = zMerged
            anyChanged = anyChanged || cz
        } while (anyChanged)

        return cur
    }
}
