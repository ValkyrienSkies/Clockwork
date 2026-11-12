package org.valkyrienskies.clockwork.content.schematics

import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.texture.TextureAtlas
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import org.valkyrienskies.clockwork.ClockworkShaders
import kotlin.math.max

class VdexSketchRenderer : AutoCloseable {
    private var target: TextureTarget? = null
    private var captured: List<Any>? = null
    var yaw = -35f
    var pitch = 25f
    var zoom = 1f

    fun reset() { yaw = -35f; pitch = 25f; zoom = 1f }

    fun render(graphics: GuiGraphics, data: VdexBlueprintData, x: Int, y: Int, width: Int, height: Int): Float {
        val mesh = VdexBlueprintPreview.mesh(data)
        val buffer = mesh.buffer ?: return mesh.progress
        val geometry = ClockworkShaders.blueprintGeometry() ?: return 0f
        val sketch = ClockworkShaders.blueprintSketch() ?: return 0f
        val mc = Minecraft.getInstance()
        val pixelScale = mc.window.guiScale.coerceIn(1.0, 3.0)
        val w = (width * pixelScale).toInt().coerceAtMost(1536)
        val h = (height * pixelScale).toInt().coerceAtMost(1024)
        graphics.flush()
        val previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        val previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        val viewport = IntArray(4).also { GL11.glGetIntegerv(GL11.GL_VIEWPORT, it) }
        val scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
        val depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
        val cull = GL11.glIsEnabled(GL11.GL_CULL_FACE)
        val blend = GL11.glIsEnabled(GL11.GL_BLEND)
        val depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)
        try {
            val key = listOf(data, buffer, geometry, w, h, yaw, pitch, zoom)
            if (captured != key) {
                GL11.glDisable(GL11.GL_SCISSOR_TEST)
                if (target?.width != w || target?.height != h) {
                    target?.destroyBuffers()
                    target = TextureTarget(w, h, true, Minecraft.ON_OSX).also { it.setClearColor(0f, 0f, 0f, 0f) }
                }
                val scene = target!!
                RenderSystem.depthMask(true)
                scene.clear(Minecraft.ON_OSX)
                scene.bindWrite(true)
                RenderSystem.enableDepthTest(); RenderSystem.disableBlend(); RenderSystem.disableCull()
                val center = data.bounds.center
                val rotation = Matrix4f().rotateX(Math.toRadians(pitch.toDouble()).toFloat()).rotateY(Math.toRadians(yaw.toDouble()).toFloat())
                var extentX = 0f
                var extentY = 0f
                var extentZ = 0f
                for (px in listOf(data.bounds.minX, data.bounds.maxX)) for (py in listOf(data.bounds.minY, data.bounds.maxY)) for (pz in listOf(data.bounds.minZ, data.bounds.maxZ)) {
                    val p = rotation.transformPosition(Vector3f((px - center.x).toFloat(), (py - center.y).toFloat(), (pz - center.z).toFloat()))
                    extentX = max(extentX, kotlin.math.abs(p.x)); extentY = max(extentY, kotlin.math.abs(p.y)); extentZ = max(extentZ, kotlin.math.abs(p.z))
                }
                val aspect = w.toFloat() / h
                val halfY = max(.5f, max(extentY, extentX / aspect)) * 1.15f / zoom
                val projection = Matrix4f().setOrtho(-halfY * aspect, halfY * aspect, -halfY, halfY, -extentZ - 10f, extentZ + 10f)
                val model = rotation.translate(-center.x.toFloat(), -center.y.toFloat(), -center.z.toFloat())
                RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS)
                buffer.bind()
                buffer.drawWithShader(model, projection, geometry)
                VertexBuffer.unbind()
                captured = key
            }
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw)
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead)
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST)
            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull()
            RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc()
            RenderSystem.setShader { sketch }
            RenderSystem.setShaderTexture(0, target!!.colorTextureId)
            sketch.safeGetUniform("SketchSize").set(w.toFloat(), h.toFloat())
            sketch.safeGetUniform("PixelScale").set(pixelScale.toFloat())
            val matrix = graphics.pose().last().pose()
            val builder = Tesselator.getInstance().builder
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
            builder.vertex(matrix, x.toFloat(), (y + height).toFloat(), 0f).uv(0f, 0f).endVertex()
            builder.vertex(matrix, (x + width).toFloat(), (y + height).toFloat(), 0f).uv(1f, 0f).endVertex()
            builder.vertex(matrix, (x + width).toFloat(), y.toFloat(), 0f).uv(1f, 1f).endVertex()
            builder.vertex(matrix, x.toFloat(), y.toFloat(), 0f).uv(0f, 1f).endVertex()
            BufferUploader.drawWithShader(builder.end())
        } finally {
            VertexBuffer.unbind()
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw)
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead)
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST) else GL11.glDisable(GL11.GL_SCISSOR_TEST)
            if (depth) RenderSystem.enableDepthTest() else RenderSystem.disableDepthTest()
            if (cull) RenderSystem.enableCull() else RenderSystem.disableCull()
            if (blend) RenderSystem.enableBlend() else RenderSystem.disableBlend()
            RenderSystem.depthMask(depthMask)
        }
        return 1f
    }

    override fun close() { target?.destroyBuffers(); target = null; captured = null }
}
