package org.valkyrienskies.clockwork.util.render

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.valkyrienskies.clockwork.ClockworkRenderTypes

/** Encodes either our native shader's metadata or ordinary, fully populated entity vertices. */
class SurfaceEffectPass(private val buffers: MultiBufferSource, private val time: Float) {
    val compatible = ShaderPackCompat.enabled()
    private lateinit var consumer: VertexConsumer
    private lateinit var sprite: TextureAtlasSprite
    private var slot: SurfaceEffectAtlas.Slot? = null
    private var mode = 0
    private val color = Vector4f()
    private val normal = Vector3f()
    private val normalMatrix = Matrix3f()

    fun quad(quad: BakedQuad, matrix: Matrix4f, mode: Int) {
        this.mode = mode
        sprite = quad.sprite
        slot = if (compatible) SurfaceEffectAtlas.get(sprite) else null
        consumer = buffers.getBuffer(if (!compatible) ClockworkRenderTypes.GRAVITRON_SURFACE
            else if (mode == 1) ClockworkRenderTypes.TOOL_FROZEN_COMPAT else ClockworkRenderTypes.TOOL_SURFACE_COMPAT)
        normal.set(quad.direction.stepX.toFloat(), quad.direction.stepY.toFloat(), quad.direction.stepZ.toFloat())
        if (compatible) matrix.normal(normalMatrix).transform(normal).normalize()
    }

    fun vertex(matrix: Matrix4f, x: Float, y: Float, z: Float, r: Float, g: Float, b: Float,
               alpha: Float, u: Float, v: Float, distance: Float, light: Int) {
        val vc = consumer.vertex(matrix, x, y, z)
        val tile = slot
        if (tile == null) {
            vc.color(r, g, b, alpha).uv(u, v).overlayCoords((distance.coerceIn(-127f, 127f) * 256).toInt(), mode)
        } else {
            SurfaceEffectColor.shade(mode, distance, time, r, g, b, alpha, color)
            vc.color(color.x, color.y, color.z, color.w)
                .uv(tile.u((u - sprite.u0) / (sprite.u1 - sprite.u0), mode == 1),
                    tile.v((v - sprite.v0) / (sprite.v1 - sprite.v0)))
                .overlayCoords(OverlayTexture.NO_OVERLAY)
        }
        vc.uv2(light).normal(normal.x, normal.y, normal.z).endVertex()
    }

    companion object {
        fun endBatch(buffers: MultiBufferSource.BufferSource) {
            buffers.endBatch(ClockworkRenderTypes.GRAVITRON_SURFACE)
            buffers.endBatch(ClockworkRenderTypes.TOOL_SURFACE_COMPAT)
            buffers.endBatch(ClockworkRenderTypes.TOOL_FROZEN_COMPAT)
        }
    }
}
