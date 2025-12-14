package org.valkyrienskies.clockwork.content.logistics.gas.crafter

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.util.Mth
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.util.EaseHelper
import org.valkyrienskies.mod.common.util.toJOMLF
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random

class GasCrafterBlockEntityRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<GasCrafterBlockEntity>(context) {

    override fun renderSafe(be: GasCrafterBlockEntity, partialTicks: Float, ms: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
        val frame = CachedBuffers.partial(ClockworkPartials.GAS_CRAFTER_FRAME, be.blockState)
        val tube = CachedBuffers.partial(ClockworkPartials.GAS_CRAFTER_TUBE, be.blockState)
        val mesh = CachedBuffers.partial(ClockworkPartials.GAS_CRAFTER_MESH, be.blockState)
        val glow_lamp = CachedBuffers.partial(ClockworkPartials.GAS_CRAFTER_GLOW, be.blockState)

        var glow: Float = 2-be.glow.getValue(partialTicks)
        glow = (1 - (2 * (glow - .75f).toDouble().pow(2.0))).toFloat()
        glow = Mth.clamp(glow, -1f, 1f)
        val color = (200 * glow).toInt()



        glow_lamp.color<SuperByteBuffer>(color,color,color,255).disableDiffuse<SuperByteBuffer>()



        val cutout = buffer.getBuffer(RenderType.cutout())
        val translucent = buffer.getBuffer(RenderType.translucent())

        val facing = be.blockState.getValue(BlockStateProperties.FACING) ?: return
        be.clientProcessingTicks = max(be.clientProcessingTicks-partialTicks,0f)


        mesh.translate(facing.normal.toJOMLF().mul(EaseHelper.easeInQuad(cos(be.clientProcessingTicks * 0.1f))*0.3f-0.3f))

        if (be.clientProcessingTicks > 0) {
            val randomOffset =Vector3f(Random.nextFloat(),Random.nextFloat(),Random.nextFloat()).mul(0.01f)
            frame.translate(randomOffset)
            glow_lamp.translate(randomOffset)
            tube.translate(randomOffset)
        }



        fun rotateAndRender(buffer: SuperByteBuffer, light: Int, consumer: VertexConsumer) {
            buffer.translate(0.5,0.5,0.5).rotateToFace(facing).translateBack(0.5,0.5,0.5).light<SuperByteBuffer>(light).renderInto(ms, consumer)

        }

        rotateAndRender(frame, light, cutout)
        rotateAndRender(mesh, light, translucent)
        rotateAndRender(tube, LightTexture.FULL_BRIGHT, cutout)
        rotateAndRender(glow_lamp, LightTexture.FULL_BRIGHT, translucent)

    }


}