package org.valkyrienskies.clockwork.content.kinetics.plotting

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.resources.ResourceLocation
import org.lwjgl.opengl.GL11

internal object PlottingGridRenderer {
    private val CONCRETE = ResourceLocation("minecraft", "textures/block/black_concrete.png")

    fun draw(g: GuiGraphics, left: Int, top: Int, right: Int, bottom: Int, panX: Double, panY: Double, cell: Double) {
        g.flush()
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader)
        RenderSystem.setShaderTexture(0, CONCRETE)
        RenderSystem.bindTexture(RenderSystem.getShaderTexture(0))
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT)
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)
        val u = -panX % cell / cell
        val v = -panY % cell / cell
        val builder = Tesselator.getInstance().builder
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
        fun vertex(x: Int, y: Int) {
            builder.vertex(g.pose().last().pose(), x.toFloat(), y.toFloat(), 0f)
                .uv((u + (x - left) / cell).toFloat(), (v + (y - top) / cell).toFloat())
                .color(255, 255, 255, 255).endVertex()
        }
        vertex(left, top)
        vertex(left, bottom)
        vertex(right, bottom)
        vertex(right, top)
        BufferUploader.drawWithShader(builder.end())
        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
        fun line(x1: Int, y1: Int, x2: Int, y2: Int) {
            fun corner(x: Int, y: Int) {
                builder.vertex(g.pose().last().pose(), x.toFloat(), y.toFloat(), 0f)
                    .color(48, 50, 56, 255).endVertex()
            }
            corner(x1, y1); corner(x1, y2); corner(x2, y2); corner(x2, y1)
        }
        var x = left + panX % cell
        while (x < right) { line(x.toInt(), top, x.toInt() + 1, bottom); x += cell }
        var y = top + panY % cell
        while (y < bottom) { line(left, y.toInt(), right, y.toInt() + 1); y += cell }
        BufferUploader.drawWithShader(builder.end())
    }
}
