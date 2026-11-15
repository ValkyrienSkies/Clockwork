package org.valkyrienskies.clockwork.util.render

import net.minecraft.world.phys.Vec3

/**
 * Builds a line out of straight segments whose quads meet cleanly at every bend, so translucent lines
 * neither leave gaps nor draw anything twice.
 *
 * Each segment has a set of *stations*: offsets from its centre line, in order across its width (two
 * for a plain ribbon, more when the edges fade out). Where two segments touch:
 * - stations on the inside of the bend meet at the point where their edges cross;
 * - stations on the outside stay square to their own segment, and the wedge between them is filled.
 *
 * Feed segments in order with [segment]. A segment that doesn't start where the previous one ended
 * begins a new run. Call [end] when the line is finished, or [close] to join a loop back to its
 * start; segments are emitted only once their neighbours are known, so nothing is drawn before then.
 *
 * Quads go to [emit] as four calls each, with the station index and the tag of the segment the
 * vertex belongs to. Segments are emitted across the width; the wedges are emitted with the same
 * vertex order. Both need a render state without back-face culling.
 */
class JoinedStrip<T>(private val emit: (point: Vec3, station: Int, tag: T) -> Unit) {

    private class Segment<T>(val a: Vec3, val b: Vec3, val length: Double, val offsets: Array<Vec3>, val tag: T) {
        val direction: Vec3 = b.subtract(a).scale(1.0 / length)
        val start = Array(offsets.size) { a.add(offsets[it]) }
        val end = Array(offsets.size) { b.add(offsets[it]) }
    }

    /** Held back until the run ends, so a closed loop can still move its starting corner. */
    private var first: Segment<T>? = null
    private var last: Segment<T>? = null

    fun segment(a: Vec3, b: Vec3, offsets: Array<Vec3>, tag: T) {
        val length = a.distanceTo(b)
        if (length < 1e-9) return
        val segment = Segment(a, b, length, offsets, tag)
        val previous = last
        if (previous != null && previous.offsets.size == offsets.size && touches(previous.b, a)) {
            join(previous, segment)
            if (previous !== first) draw(previous)
        } else {
            end()
            first = segment
        }
        last = segment
    }

    /** Joins the last segment back to the first when the run ends where it began, then finishes it. */
    fun close() {
        val start = first
        val finish = last
        if (start != null && finish != null && start !== finish && touches(finish.b, start.a)) join(finish, start)
        end()
    }

    /** Emits whatever is still held back. */
    fun end() {
        first?.let(::draw)
        last?.let { if (it !== first) draw(it) }
        first = null
        last = null
    }

    private fun join(from: Segment<T>, to: Segment<T>) {
        val p = to.a
        val count = from.offsets.size
        val outer = BooleanArray(count)
        for (i in 0 until count) {
            val sideFrom = from.offsets[i]
            val sideTo = to.offsets[i]
            // A station on the side the line turns away from is on the outside of the bend.
            if (sideFrom.dot(to.direction) < 0) {
                outer[i] = true
                from.end[i] = p.add(sideFrom)
                to.start[i] = p.add(sideTo)
            } else {
                val corner = p.add(miter(sideFrom, sideTo, minOf(from.length, to.length)))
                from.end[i] = corner
                to.start[i] = corner
            }
        }
        for (i in 0 until count - 1) if (outer[i] || outer[i + 1]) {
            emit(from.end[i], i, from.tag)
            emit(from.end[i + 1], i + 1, from.tag)
            emit(to.start[i + 1], i + 1, to.tag)
            emit(to.start[i], i, to.tag)
        }
    }

    private fun draw(segment: Segment<T>) {
        for (i in 0 until segment.offsets.size - 1) {
            emit(segment.start[i], i, segment.tag)
            emit(segment.start[i + 1], i + 1, segment.tag)
            emit(segment.end[i + 1], i + 1, segment.tag)
            emit(segment.end[i], i, segment.tag)
        }
    }

    companion object {
        // Loops built from cos/sin don't land exactly on their start point.
        private fun touches(p: Vec3, q: Vec3) = p.distanceToSqr(q) < 1e-10

        /**
         * Offset from the joint to where two edges cross: the edges sit at [sideFrom] and [sideTo] from
         * the centre line, each running perpendicular to its offset. Solves m·sideFrom = |sideFrom|² and
         * m·sideTo = |sideTo|² within the plane of the two offsets. Very sharp bends would push the
         * corner past the neighbouring segments, so it's held within [limit].
         */
        private fun miter(sideFrom: Vec3, sideTo: Vec3, limit: Double): Vec3 {
            val a = sideFrom.lengthSqr()
            val b = sideTo.lengthSqr()
            val c = sideFrom.dot(sideTo)
            val det = a * b - c * c
            val corner = if (det <= 1e-9 * a * b) sideFrom.add(sideTo).scale(0.5)
                else sideFrom.scale(b * (a - c) / det).add(sideTo.scale(a * (b - c) / det))
            val length = corner.length()
            return if (length > limit) corner.scale(limit / length) else corner
        }
    }
}
