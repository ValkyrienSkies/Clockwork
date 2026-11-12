package org.valkyrienskies.clockwork.content.schematics

import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.world.phys.AABB
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL21
import org.lwjgl.opengl.GL30
import org.lwjgl.system.MemoryStack
import org.valkyrienskies.clockwork.ClockworkShaders
import kotlin.math.max

class VdexSketchRenderer : AutoCloseable {
    private var target: TextureTarget? = null
    private var toneTarget: TextureTarget? = null
    private var toneRange = Vector2f(0f, 1f)
    private var inkBuffer: VertexBuffer? = null
    private var captured: List<Any?>? = null
    var yaw = -35f
    var pitch = 25f
    var zoom = 1f

    fun reset() { yaw = -35f; pitch = 25f; zoom = 1f }

    fun render(graphics: GuiGraphics, data: VdexBlueprintData, x: Int, y: Int, width: Int, height: Int,
               draw: Float = 1f, erase: Float = 0f, frozen: Boolean = false): Float {
        val mesh = if (frozen) null else VdexBlueprintPreview.mesh(data)
        val buffer = mesh?.buffer
        if (buffer == null && !frozen) return mesh?.progress ?: 0f
        if (frozen && target == null) return 0f
        val geometry = ClockworkShaders.blueprintGeometry() ?: return 0f
        val sketch = ClockworkShaders.blueprintSketch() ?: return 0f
        val ink = ClockworkShaders.blueprintInk() ?: return 0f
        val mc = Minecraft.getInstance()
        val pixelScale = mc.window.guiScale.coerceIn(1.0, 3.0)
        val w = (width * pixelScale).toInt().coerceAtMost(1536)
        val h = (height * pixelScale).toInt().coerceAtMost(1024)
        graphics.flush()
        VdexPreviewRenderState().use { state ->
            val key = listOf(data, buffer, geometry, ink, w, h, pixelScale, yaw, pitch, zoom)
            if (!frozen && captured != key) {
                RenderSystem.disableScissor()
                if (target?.width != w || target?.height != h) {
                    target?.destroyBuffers()
                    target = TextureTarget(w, h, true, Minecraft.ON_OSX).also { it.setClearColor(0f, 0f, 0f, 0f) }
                }
                val scene = target!!
                RenderSystem.depthMask(true)
                scene.clear(Minecraft.ON_OSX)
                scene.bindWrite(true)
                RenderSystem.enableDepthTest(); RenderSystem.disableBlend(); RenderSystem.disableCull()
                var bounds = data.bounds
                mesh!!.ink.strokes.forEach { stroke -> stroke.points.forEach { bounds = bounds.minmax(AABB(it, it)) } }
                val center = bounds.center
                val rotation = Matrix4f().rotateX(Math.toRadians(pitch.toDouble()).toFloat()).rotateY(Math.toRadians(yaw.toDouble()).toFloat())
                var extentX = 0f
                var extentY = 0f
                var extentZ = 0f
                for (px in listOf(bounds.minX, bounds.maxX)) for (py in listOf(bounds.minY, bounds.maxY)) for (pz in listOf(bounds.minZ, bounds.maxZ)) {
                    val p = rotation.transformPosition(Vector3f((px - center.x).toFloat(), (py - center.y).toFloat(), (pz - center.z).toFloat()))
                    extentX = max(extentX, kotlin.math.abs(p.x)); extentY = max(extentY, kotlin.math.abs(p.y)); extentZ = max(extentZ, kotlin.math.abs(p.z))
                }
                val aspect = w.toFloat() / h
                val halfY = max(.5f, max(extentY, extentX / aspect)) * 1.15f / zoom
                val projection = Matrix4f().setOrtho(-halfY * aspect, halfY * aspect, -halfY, halfY, -extentZ - 10f, extentZ + 10f)
                val model = rotation.translate(-center.x.toFloat(), -center.y.toFloat(), -center.z.toFloat())
                RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS)
                buffer!!.bind()
                buffer.drawWithShader(model, projection, geometry)
                VertexBuffer.unbind()
                inkBuffer?.close()
                inkBuffer = VdexSketchInk.upload(mesh.ink, model, projection, width.toFloat(), height.toFloat())
                measureTones(scene)
                captured = key
            }
            state.restoreTarget()
            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull()
            RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc()
            RenderSystem.setShader { sketch }
            RenderSystem.setShaderTexture(0, target!!.colorTextureId)
            sketch.safeGetUniform("SketchSize").set(target!!.width.toFloat(), target!!.height.toFloat())
            sketch.safeGetUniform("PixelScale").set(pixelScale.toFloat())
            sketch.safeGetUniform("Reveal").set(draw, erase)
            sketch.safeGetUniform("WorldPreview").set(0f)
            sketch.safeGetUniform("ToneRange").set(toneRange.x, toneRange.y)
            val matrix = graphics.pose().last().pose()
            val builder = Tesselator.getInstance().builder
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
            builder.vertex(matrix, x.toFloat(), (y + height).toFloat(), 0f).uv(0f, 0f).endVertex()
            builder.vertex(matrix, (x + width).toFloat(), (y + height).toFloat(), 0f).uv(1f, 0f).endVertex()
            builder.vertex(matrix, (x + width).toFloat(), y.toFloat(), 0f).uv(1f, 1f).endVertex()
            builder.vertex(matrix, x.toFloat(), y.toFloat(), 0f).uv(0f, 1f).endVertex()
            BufferUploader.drawWithShader(builder.end())
            inkBuffer?.let {
                RenderSystem.setShaderTexture(0, target!!.depthTextureId)
                ink.safeGetUniform("RenderRect").set(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
                ink.safeGetUniform("SketchSize").set(target!!.width.toFloat(), target!!.height.toFloat())
                ink.safeGetUniform("PixelScale").set(pixelScale.toFloat())
                ink.safeGetUniform("Reveal").set(draw, erase)
                ink.safeGetUniform("WorldPreview").set(0f)
                it.bind()
                it.drawWithShader(Matrix4f(RenderSystem.getModelViewMatrix()).mul(matrix), RenderSystem.getProjectionMatrix(), ink)
            }
        }
        return 1f
    }

    internal fun sampleToneRange(scene: TextureTarget): Vector2f {
        measureTones(scene)
        return Vector2f(toneRange)
    }

    private fun measureTones(scene: TextureTarget) {
        val sample = toneTarget ?: TextureTarget(64, 64, false, Minecraft.ON_OSX).also { toneTarget = it }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, scene.frameBufferId)
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sample.frameBufferId)
        GL30.glBlitFramebuffer(0, 0, scene.width, scene.height, 0, 0, 64, 64, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST)
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sample.frameBufferId)
        val packBuffer = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING)
        val settings = intArrayOf(GL11.GL_PACK_ALIGNMENT, GL11.GL_PACK_ROW_LENGTH, GL11.GL_PACK_SKIP_ROWS, GL11.GL_PACK_SKIP_PIXELS)
        val previous = settings.map { GL11.glGetInteger(it) }
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0)
            settings.forEach { GL11.glPixelStorei(it, if (it == GL11.GL_PACK_ALIGNMENT) 1 else 0) }
            MemoryStack.stackPush().use { stack ->
                val pixels = stack.malloc(64 * 64 * 4)
                GL11.glReadPixels(0, 0, 64, 64, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels)
                toneRange = VdexSketchTones.measure(pixels)
            }
        } finally {
            settings.forEachIndexed { i, setting -> GL11.glPixelStorei(setting, previous[i]) }
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, packBuffer)
        }
    }

    override fun close() {
        if (target == null && toneTarget == null && inkBuffer == null) return
        VdexPreviewRenderState().use {
            target?.destroyBuffers()
            toneTarget?.destroyBuffers()
            inkBuffer?.close()
            target = null
            toneTarget = null
            inkBuffer = null
            captured = null
        }
    }
}
