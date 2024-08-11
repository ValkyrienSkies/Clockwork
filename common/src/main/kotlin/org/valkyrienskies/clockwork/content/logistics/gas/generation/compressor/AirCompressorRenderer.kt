package org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.utility.AnimationTickHolder
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import org.valkyrienskies.clockwork.ClockworkPartials


class AirCompressorRenderer(context: BlockEntityRendererProvider.Context?) : KineticBlockEntityRenderer<AirCompressorBlockEntity>(context) {
    override fun renderSafe(
        be: AirCompressorBlockEntity?,
        partialTicks: Float,
        ms: PoseStack?,
        buffer: MultiBufferSource?,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)

        val vb = buffer!!.getBuffer(RenderType.cutoutMipped())

        val axis = CachedBufferer.partial(ClockworkPartials.COMPRESSOR_AXIS, be!!.blockState)
        val lightBelow = LevelRenderer.getLightColor(be.level, be.blockPos.below())

        standardKineticRotationTransform(axis,be,lightBelow).renderInto(ms, vb);
    }
}