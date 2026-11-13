package org.valkyrienskies.clockwork.util.render

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f

/**
 * Draws a bending, camera-facing ribbon whose segments meet cleanly at each bend: no gap on the outside
 * and no doubled-up overlap on the inside, so translucent and additive lines stay evenly shaded.
 *
 * Feed segments in order with [segment], then finish with [end], or with [close] for a loop. Nothing is
 * drawn until a segment's neighbours are known, so a line that is never finished is never fully drawn.
 * A segment that doesn't start where the previous one ended begins a new run, so callers can skip
 * segments to break the line.
 *
 * Positions are camera-relative, as for [RenderUtil.addRibbonSegment]. Each layer of a multi-layer line
 * (glow, core, highlight) needs its own [RibbonLine].
 */
class RibbonLine(private val vc: VertexConsumer, private val pose: Matrix4f) {

    private class Color(val r: Float, val g: Float, val b: Float, val alpha: Float)

    private val strip = JoinedStrip<Color> { p, station, color ->
        vc.vertex(pose, p.x.toFloat(), p.y.toFloat(), p.z.toFloat())
            .color(color.r, color.g, color.b, color.alpha).uv(station.toFloat(), 0.5f).endVertex()
    }

    /** Same arguments as [RenderUtil.addRibbonSegment]. */
    fun segment(a: Vec3, b: Vec3, thickness: Float, r: Float, g: Float, bl: Float, alpha: Float): RibbonLine {
        val side = side(a, b, thickness) ?: return this
        strip.segment(a, b, arrayOf(side.scale(-1.0), side), Color(r, g, bl, alpha))
        return this
    }

    /** Draws everything still pending. */
    fun end() = strip.end()

    /** Joins the last segment back to the first when the line ends where it began, then draws it. */
    fun close() = strip.close()

    companion object {
        /** Half-width offset of a camera-facing ribbon from [a] to [b], or null for a zero-length segment. */
        fun side(a: Vec3, b: Vec3, thickness: Float): Vec3? {
            val dir = b.subtract(a)
            val len = dir.length()
            if (len < 1e-6) return null
            val dirN = dir.scale(1.0 / len)

            // a/b are camera-relative, so camera is origin
            val view = a.add(b).scale(0.5).normalize()
            var side = dirN.cross(view)
            if (side.length() < 1e-6) {
                side = dirN.cross(Vec3(0.0, 1.0, 0.0))
                if (side.length() < 1e-6) side = dirN.cross(Vec3(1.0, 0.0, 0.0))
            }
            return side.normalize().scale(thickness.toDouble())
        }
    }
}
