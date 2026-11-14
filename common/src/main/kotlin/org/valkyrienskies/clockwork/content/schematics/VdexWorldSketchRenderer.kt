package org.valkyrienskies.clockwork.content.schematics

import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureAtlas
import org.joml.Matrix4f
import org.joml.Vector2f
import org.lwjgl.opengl.GL11
import org.valkyrienskies.clockwork.ClockworkShaders
import kotlin.math.min
import kotlin.math.sqrt

internal class VdexWorldSketchRenderer : AutoCloseable {
    private var target: TextureTarget? = null
    private var quad: VertexBuffer? = null
    private var inkBuffer: VertexBuffer? = null
    private var captured: List<Any>? = null
    private var toned: VdexBlueprintData? = null
    private var tones = Vector2f(0f, 1f)
    private val toneSampler = VdexSketchRenderer()

    fun render(mesh: VdexBlueprintPreview.Mesh, model: Matrix4f, projection: Matrix4f) {
        val buffer = mesh.buffer ?: return
        val geometry = ClockworkShaders.blueprintGeometry() ?: return
        val sketch = ClockworkShaders.blueprintSketch() ?: return
        val ink = ClockworkShaders.blueprintInk() ?: return
        val viewport = IntArray(4).also { GL11.glGetIntegerv(GL11.GL_VIEWPORT, it) }
        if (viewport[2] <= 0 || viewport[3] <= 0) return
        val downsample = min(1.0, sqrt(1048576.0 / (viewport[2].toDouble() * viewport[3])))
        val width = (viewport[2] * downsample).toInt().coerceAtLeast(1)
        val height = (viewport[3] * downsample).toInt().coerceAtLeast(1)
        val pixelScale = (Minecraft.getInstance().window.guiScale * downsample).toFloat().coerceAtLeast(1f)
        VdexPreviewRenderState().use { state ->
            val key = listOf(mesh, geometry, ink, Matrix4f(model), Matrix4f(projection), width, height, pixelScale)
            if (captured != key) {
                RenderSystem.disableScissor()
                if (target?.width != width || target?.height != height) {
                    target?.destroyBuffers()
                    target = TextureTarget(width, height, true, Minecraft.ON_OSX).also { it.setClearColor(0f, 0f, 0f, 0f) }
                }
                val scene = target!!
                RenderSystem.depthMask(true); RenderSystem.clearDepth(1.0)
                scene.clear(Minecraft.ON_OSX); scene.bindWrite(true)
                RenderSystem.enableDepthTest(); RenderSystem.depthFunc(GL11.GL_LEQUAL)
                RenderSystem.disableCull(); RenderSystem.disableBlend()
                RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS)
                buffer.bind(); buffer.drawWithShader(model, projection, geometry); VertexBuffer.unbind()
                inkBuffer?.close()
                inkBuffer = VdexSketchInk.upload(mesh.ink, model, projection, width / pixelScale, height / pixelScale)
                if (toned !== mesh.data) { tones = toneSampler.sampleToneRange(scene); toned = mesh.data }
                captured = key
            }
            state.restoreTarget()
            RenderSystem.enableDepthTest(); RenderSystem.depthFunc(GL11.GL_LEQUAL); RenderSystem.depthMask(false)
            RenderSystem.disableCull(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc()
            val scene = target!!
            RenderSystem.setShaderTexture(0, scene.colorTextureId)
            RenderSystem.setShaderTexture(1, scene.depthTextureId)
            sketch.safeGetUniform("SketchSize").set(width.toFloat(), height.toFloat())
            sketch.safeGetUniform("PixelScale").set(pixelScale)
            sketch.safeGetUniform("ToneRange").set(tones.x, tones.y)
            sketch.safeGetUniform("Reveal").set(1f, 0f)
            sketch.safeGetUniform("WorldPreview").set(1f)
            val screen = quad ?: createQuad().also { quad = it }
            screen.bind(); screen.drawWithShader(Matrix4f(), Matrix4f(), sketch); VertexBuffer.unbind()
            inkBuffer?.let {
                RenderSystem.setShaderTexture(0, scene.depthTextureId)
                ink.safeGetUniform("SketchSize").set(width.toFloat(), height.toFloat())
                ink.safeGetUniform("PixelScale").set(pixelScale)
                ink.safeGetUniform("Reveal").set(1f, 0f)
                ink.safeGetUniform("WorldPreview").set(1f)
                ink.safeGetUniform("RenderRect").set(-1f, 1f, 2f, -2f)
                it.bind(); it.drawWithShader(Matrix4f(), Matrix4f(), ink); VertexBuffer.unbind()
            }
        }
    }

    private fun createQuad(): VertexBuffer {
        val builder = BufferBuilder(256)
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        builder.vertex(-1.0, -1.0, 0.0).uv(0f, 0f).endVertex()
        builder.vertex(1.0, -1.0, 0.0).uv(1f, 0f).endVertex()
        builder.vertex(1.0, 1.0, 0.0).uv(1f, 1f).endVertex()
        builder.vertex(-1.0, 1.0, 0.0).uv(0f, 1f).endVertex()
        return VertexBuffer(VertexBuffer.Usage.STATIC).also { it.bind(); it.upload(builder.end()); VertexBuffer.unbind() }
    }

    override fun close() {
        VdexPreviewRenderState().use {
            target?.destroyBuffers(); quad?.close(); inkBuffer?.close(); toneSampler.close()
            target = null; quad = null; inkBuffer = null; captured = null; toned = null
        }
    }
}
