package org.valkyrienskies.clockwork.content.curiosities.clampotron

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

class ClampotronRenderer(context: BlockEntityRendererProvider.Context) : KineticBlockEntityRenderer<ClampotronBlockEntity>(context) {
    override fun renderSafe(be: ClampotronBlockEntity, partialTicks: Float, ms: PoseStack, buffers: MultiBufferSource,
                            light: Int, overlay: Int) {
        val shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.blockState, be.facing.opposite)
        standardKineticRotationTransform(shaft, be, light).renderInto(ms, buffers.getBuffer(RenderType.solid()))
    }
}
