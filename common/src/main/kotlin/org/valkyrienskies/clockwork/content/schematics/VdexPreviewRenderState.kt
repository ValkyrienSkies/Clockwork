package org.valkyrienskies.clockwork.content.schematics

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexBuffer
import net.minecraft.client.Minecraft
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL14
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30

internal class VdexPreviewRenderState : AutoCloseable {
    private val drawTarget = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
    private val readTarget = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
    private val mainTarget = Minecraft.getInstance()?.mainRenderTarget?.takeIf { it.frameBufferId == drawTarget }
    private val viewport = IntArray(4).also { GL11.glGetIntegerv(GL11.GL_VIEWPORT, it) }
    private val scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
    private val scissorBox = IntArray(4).also { GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, it) }
    private val depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
    private val cull = GL11.glIsEnabled(GL11.GL_CULL_FACE)
    private val blend = GL11.glIsEnabled(GL11.GL_BLEND)
    private val depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)
    private val depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC)
    private val clearColor = FloatArray(4).also { GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, it) }
    private val clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE)
    private val sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB)
    private val destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB)
    private val sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA)
    private val destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA)
    private val equationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB)
    private val equationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA)
    private val activeTexture = GlStateManager._getActiveTexture()
    private val textures = IntArray(2) { RenderSystem.getShaderTexture(it) }
    private val shader = RenderSystem.getShader()
    private val bindings: IntArray

    init {
        bindings = IntArray(2) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + it)
            GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
        }
        RenderSystem.activeTexture(GL13.GL_TEXTURE0)
    }

    fun restoreTarget() {
        if (mainTarget != null) mainTarget.bindWrite(false)
        else GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawTarget)
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readTarget)
        RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
        if (scissor) RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]) else RenderSystem.disableScissor()
    }

    override fun close() {
        VertexBuffer.unbind()
        restoreTarget()
        RenderSystem.setShader { shader }
        bindings.forEachIndexed { unit, binding ->
            RenderSystem.setShaderTexture(unit, textures[unit])
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit)
            RenderSystem.bindTexture(binding)
        }
        RenderSystem.activeTexture(activeTexture)
        RenderSystem.blendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha)
        GL20.glBlendEquationSeparate(equationRgb, equationAlpha)
        if (depth) RenderSystem.enableDepthTest() else RenderSystem.disableDepthTest()
        if (cull) RenderSystem.enableCull() else RenderSystem.disableCull()
        if (blend) RenderSystem.enableBlend() else RenderSystem.disableBlend()
        RenderSystem.depthMask(depthMask)
        RenderSystem.depthFunc(depthFunction)
        RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3])
        RenderSystem.clearDepth(clearDepth)
    }
}
