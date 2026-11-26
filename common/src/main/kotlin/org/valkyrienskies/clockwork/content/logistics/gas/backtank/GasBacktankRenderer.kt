package org.valkyrienskies.clockwork.content.logistics.gas.backtank

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkPartials

class GasBacktankRenderer(@Suppress("UNUSED_PARAMETER") context: BlockEntityRendererProvider.Context?) : SafeBlockEntityRenderer<GasBacktankBlockEntity>() {

    override fun renderSafe(
        be: GasBacktankBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        renderNeedle(be.blockState, be.needle.getValue(partialTicks), light, ms, buffer.getBuffer(RenderType.cutout()))
    }

    companion object {
        private const val SWEEP_DEGREES = 180f

        private const val PIVOT_Y = 7.0 / 16.0
        private const val PIVOT_Z = 8.0 / 16.0

        fun renderNeedle(
            state: BlockState,
            fraction: Float,
            light: Int,
            ms: PoseStack,
            consumer: VertexConsumer
        ) {
            val facing = if (state.hasProperty(HorizontalDirectionalBlock.FACING))
                state.getValue(HorizontalDirectionalBlock.FACING) else Direction.SOUTH
            CachedBuffers.partial(ClockworkPartials.GAS_BACKTANK_NEEDLE, state)
                .center()
                .rotateYDegrees(-modelYRotation(facing))
                .uncenter()
                .translate(0.0, PIVOT_Y, PIVOT_Z)
                .rotateXDegrees(fraction.coerceIn(0f, 1f) * SWEEP_DEGREES)
                .translate(0.0, -PIVOT_Y, -PIVOT_Z)
                .light<SuperByteBuffer>(light)
                .renderInto(ms, consumer)
        }

        private fun modelYRotation(facing: Direction): Float = (facing.toYRot() + 90f) % 360f
    }
}
