package org.valkyrienskies.clockwork.client.render.goggles

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import org.joml.Matrix4fc
import org.joml.Vector3dc
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.hypot

internal class AeronautGuides(
    private val vertices: VertexConsumer,
    private val pose: Matrix4fc,
    private val projection: Matrix4fc,
    private val origin: Vector3dc
) {
    private val window = Minecraft.getInstance().window
    private val halfWidth = window.guiScaledWidth * .5f
    private val halfHeight = window.guiScaledHeight * .5f

    class Point(val eye: Vector3f, val x: Float, val y: Float, val pixelX: Float, val pixelY: Float) {
        val crosshairDistance get() = maxOf(abs(x), abs(y))
        fun offset(dx: Float, dy: Float) = Point(Vector3f(eye).add(dx * pixelX, dy * pixelY, 0f),
            x + dx, y + dy, pixelX, pixelY)
    }

    fun point(position: Vector3dc): Point? {
        val eye = pose.transformPosition((position.x() - origin.x()).toFloat(),
            (position.y() - origin.y()).toFloat(), (position.z() - origin.z()).toFloat(), Vector3f())
        val clip = projection.transform(Vector4f(eye, 1f))
        if (!clip.isFinite || clip.w <= .001f || abs(clip.z) > clip.w) return null
        return Point(eye, clip.x / clip.w * halfWidth, clip.y / clip.w * halfHeight,
            clip.w / (projection.m00() * halfWidth), clip.w / (projection.m11() * halfHeight))
    }

    private fun vertex(p: Point, dx: Float, dy: Float, color: Int) {
        vertices.vertex((p.eye.x + dx * p.pixelX).toDouble(), (p.eye.y + dy * p.pixelY).toDouble(), p.eye.z.toDouble())
            .color(color shr 16 and 255, color shr 8 and 255, color and 255, color ushr 24).endVertex()
    }

    fun stroke(a: Point, b: Point, widthA: Float, widthB: Float, colorA: Int, colorB: Int = colorA) {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val length = hypot(dx, dy)
        if (length < .01f) return
        val nx = -dy * .5f / length
        val ny = dx * .5f / length
        vertex(a, nx * widthA, ny * widthA, colorA)
        vertex(b, nx * widthB, ny * widthB, colorB)
        vertex(b, -nx * widthB, -ny * widthB, colorB)
        vertex(a, -nx * widthA, -ny * widthA, colorA)
    }

    fun marker(p: Point, color: Int, hollow: Boolean, hovered: Boolean, small: Boolean) {
        val r = if (hollow && !small) 7f else if (hovered) 5f else 3f
        val a = p.offset(-r, -r)
        val b = p.offset(r, -r)
        val c = p.offset(r, r)
        val d = p.offset(-r, r)
        stroke(a, b, 1f, 1f, color)
        stroke(b, c, 1f, 1f, color)
        stroke(c, d, 1f, 1f, color)
        stroke(d, a, 1f, 1f, color)
        if (!hollow) {
            vertex(p, -1.5f, -1.5f, color)
            vertex(p, 1.5f, -1.5f, color)
            vertex(p, 1.5f, 1.5f, color)
            vertex(p, -1.5f, 1.5f, color)
        }
    }

    fun arrow(a: Point, b: Point, color: Int) {
        val length = hypot(b.x - a.x, b.y - a.y)
        if (length < 4f) return
        val dx = (b.x - a.x) / length
        val dy = (b.y - a.y) / length
        stroke(a, b, 1f, 1f, color)
        stroke(b, b.offset(-6 * dx - 3 * dy, -6 * dy + 3 * dx), 1f, 1f, color)
        stroke(b, b.offset(-6 * dx + 3 * dy, -6 * dy - 3 * dx), 1f, 1f, color)
    }
}
