package org.valkyrienskies.clockwork.effekseer.client.render

import com.mojang.blaze3d.pipeline.RenderTarget
import net.minecraft.client.Minecraft
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30


object RenderUtil {
    fun copyDepthSafely(from: RenderTarget?, to: RenderTarget) {
        val read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        val draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        to.copyDepthFrom(from)
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read)
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw)
    }

    fun copyCurrentDepthTo(target: RenderTarget) {
        val frameBuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        val window = MC.window
        copyDepthSafely(frameBuffer, window.width, window.height, target)
    }

    fun pasteToCurrentDepthFrom(source: RenderTarget) {
        val frameBuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        val window = MC.window
        copyDepthSafely(source, frameBuffer, window.width, window.height)
    }


    fun copyDepthSafely(src: Int, srcWidth: Int, srcHeight: Int, target: RenderTarget) {
        val readBackup = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        val drawBackup = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src)
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId)
        GL30.glBlitFramebuffer(0, 0, srcWidth, srcHeight, 0, 0, target.width, target.height, 256, 9728)
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readBackup)
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawBackup)
    }

    fun copyDepthSafely(src: RenderTarget, target: Int, targetWidth: Int, targetHeight: Int) {
        val readBackup = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        val drawBackup = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src.frameBufferId)
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target)
        GL30.glBlitFramebuffer(0, 0, src.width, src.height, 0, 0, targetWidth, targetHeight, 256, 9728)
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readBackup)
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawBackup)
    }

    private val MC: Minecraft = Minecraft.getInstance()
}