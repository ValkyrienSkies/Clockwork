package org.valkyrienskies.clockwork.content.kinetics.plotting

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import org.lwjgl.opengl.GL11
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkablePulse
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

internal class PlottingWireRenderer {
    private data class Quad(val x: DoubleArray, val y: DoubleArray, val color: Int)
    private data class Cable(val from: Pair<Double, Double>, val curve: Curve, val brightness: DoubleArray)

    internal class Curve(val dx: Double, val dy: Double, val start: Pair<Int, Int>, val end: Pair<Int, Int>, val zoom: Double) {
        private val distance = hypot(dx, dy)
        private val reach = max(30 * zoom, distance * .4)
        private val steps = ((distance + 2 * reach) / max(3.0, 4 * zoom)).toInt().coerceIn(12, 128)
        val x = DoubleArray(steps + 1)
        val y = DoubleArray(steps + 1)
        val lengths = DoubleArray(steps + 1)
        val normalX = DoubleArray(steps + 1)
        val normalY = DoubleArray(steps + 1)

        init {
            for (i in 0..steps) {
                val t = i.toDouble() / steps
                val u = 1 - t
                x[i] = 3*u*u*t*start.first*reach + 3*u*t*t*(dx + end.first*reach) + t*t*t*dx
                y[i] = 3*u*u*t*start.second*reach + 3*u*t*t*(dy + end.second*reach) + t*t*t*dy
                if (i > 0) lengths[i] = lengths[i - 1] + hypot(x[i] - x[i - 1], y[i] - y[i - 1])
            }
            for (i in 0..steps) {
                val a = max(0, i - 1)
                val b = minOf(steps, i + 1)
                val length = hypot(x[b] - x[a], y[b] - y[a]).coerceAtLeast(.0001)
                normalX[i] = -(y[b] - y[a]) / length
                normalY[i] = (x[b] - x[a]) / length
            }
        }

        fun matches(dx: Double, dy: Double, start: Pair<Int, Int>, end: Pair<Int, Int>, zoom: Double) =
            abs(this.dx - dx) < .000001 && abs(this.dy - dy) < .000001 &&
                this.start == start && this.end == end && this.zoom == zoom
    }

    private val curves = mutableMapOf<Int, Curve>()
    private val used = mutableSetOf<Int>()
    private val quads = mutableListOf<Quad>()
    private val cables = mutableListOf<Cable>()

    fun begin() {
        used.clear()
        quads.clear()
        cables.clear()
    }

    fun add(from: Pair<Double, Double>, to: Pair<Double, Double>,
            startDirection: Pair<Int, Int>, endDirection: Pair<Int, Int>, type: PlottingConnection,
            time: Double, seed: Int, zoom: Double, left: Int, top: Int, right: Int, bottom: Int, valid: Boolean = true) {
        val dx = to.first - from.first
        val dy = to.second - from.second
        val distance = hypot(dx, dy)
        val reach = if (type == PlottingConnection.THINKABLE) max(30 * zoom, distance * .4) else 0.0
        val c1 = from.first + startDirection.first * reach to from.second + startDirection.second * reach
        val c2 = to.first + endDirection.first * reach to to.second + endDirection.second * reach
        val margin = max(2.0, 22 * zoom)
        if (maxOf(from.first, to.first, c1.first, c2.first) + margin < left ||
            minOf(from.first, to.first, c1.first, c2.first) - margin > right ||
            maxOf(from.second, to.second, c1.second, c2.second) + margin < top ||
            minOf(from.second, to.second, c1.second, c2.second) - margin > bottom) return
        if (type == PlottingConnection.ADJACENT) {
            val end = if (distance < .0001) from.first + startDirection.first to from.second + startDirection.second else to
            val length = hypot(end.first - from.first, end.second - from.second).coerceAtLeast(.0001)
            val nx = -(end.second - from.second) / length * max(.5, 2.5 * zoom)
            val ny = (end.first - from.first) / length * max(.5, 2.5 * zoom)
            quads.add(Quad(doubleArrayOf(from.first - nx, from.first + nx, end.first + nx, end.first - nx),
                doubleArrayOf(from.second - ny, from.second + ny, end.second + ny, end.second - ny),
                if (valid) 0xffb59a64.toInt() else 0xffc36655.toInt()))
            return
        }
        if (type == PlottingConnection.LINK) {
            val steps = max(1, (distance / max(4.0, 8 * zoom)).toInt())
            repeat(steps) { i ->
                val t = (i + 0.5) / steps
                val x = (from.first + dx * t).toInt().toDouble()
                val y = (from.second + dy * t).toInt().toDouble()
                if (x + 2 >= left && x <= right && y + 2 >= top && y <= bottom)
                    quads.add(Quad(doubleArrayOf(x, x, x + 2, x + 2), doubleArrayOf(y, y + 2, y + 2, y), 0x809dabbb.toInt()))
            }
            return
        }
        val curve = curves[seed]?.takeIf { it.matches(dx, dy, startDirection, endDirection, zoom) }
            ?: Curve(dx, dy, startDirection, endDirection, zoom).also { curves[seed] = it }
        used.add(seed)
        val age = (time + seed.mod(40)).mod(72.0)
        val pulse = ThinkablePulse(0, 0, 30).progress(age.toLong(), (age % 1).toFloat())
        val brightness = DoubleArray(curve.x.size) { i ->
            val falloff = if (time < 0 || pulse !in 0.0..1.0) 0.0 else
                (1 - abs(curve.lengths[i] - pulse * curve.lengths.last()) / (64 * zoom * .9)).coerceIn(0.0, 1.0)
            val smooth = falloff * falloff * (3 - 2 * falloff)
            smooth * smooth
        }
        cables.add(Cable(from, curve, brightness))
    }

    fun draw(g: GuiGraphics, zoom: Double) {
        curves.keys.retainAll(used)
        if (quads.isEmpty() && cables.isEmpty()) return
        g.flush()
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableCull()
        try {
            val builder = Tesselator.getInstance().builder
            if (quads.isNotEmpty()) {
                RenderSystem.setShader(GameRenderer::getPositionColorShader)
                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
                for (quad in quads) for (i in 0..3) {
                    builder.vertex(g.pose().last().pose(), quad.x[i].toFloat(), quad.y[i].toFloat(), 0f)
                        .color(quad.color shr 16 and 255, quad.color shr 8 and 255, quad.color and 255, quad.color ushr 24).endVertex()
                }
                BufferUploader.drawWithShader(builder.end())
            }
            if (cables.isEmpty()) return
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader)
            RenderSystem.setShaderTexture(0, SIDE)
            RenderSystem.bindTexture(RenderSystem.getShaderTexture(0))
            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)
            val repeatLength = 40 * zoom
            fun mesh(glowing: Boolean) {
                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
                for (cable in cables) {
                    val curve = cable.curve
                    fun vertex(i: Int, side: Int) {
                        val radius = repeatLength * .5 * if (glowing) 1 + .08 * cable.brightness[i] else 1.0
                        builder.vertex(g.pose().last().pose(),
                            (cable.from.first + curve.x[i] + curve.normalX[i] * side * radius).toFloat(),
                            (cable.from.second + curve.y[i] + curve.normalY[i] * side * radius).toFloat(), 0f)
                            .uv(if (side < 0) 0f else 1f, (curve.lengths[i] / repeatLength).toFloat())
                            .color(255, 255, 255, if (glowing) (cable.brightness[i] * 255).toInt() else 255).endVertex()
                    }
                    for (i in 0 until curve.x.lastIndex) {
                        if (glowing && cable.brightness[i] == 0.0 && cable.brightness[i + 1] == 0.0) continue
                        vertex(i, -1); vertex(i, 1); vertex(i + 1, 1); vertex(i + 1, -1)
                    }
                }
                BufferUploader.drawWithShader(builder.end())
            }
            mesh(false)
            if (cables.any { cable -> cable.brightness.any { it > 0 } }) {
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE)
                mesh(true)
            }
        } finally {
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableCull()
        }
    }

    companion object {
        private val SIDE = ClockworkMod.asResource("textures/entity/thinkable_side.png")

        fun socket(g: GuiGraphics, point: Pair<Double, Double>, zoom: Double) {
            val x = point.first.toInt()
            val y = point.second.toInt()
            val radius = max(2, (2 * zoom).toInt())
            g.fill(x - radius - 1, y - radius - 1, x + radius + 1, y + radius + 1, 0xff202428.toInt())
            g.fill(x - radius, y - radius, x + radius, y + radius, 0xffefefea.toInt())
        }
    }
}
