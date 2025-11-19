package org.valkyrienskies.clockwork.content.physicalities.reactionwheel

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.render.CachedBuffers
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.level.block.state.BlockState

class ReactionWheelRenderer(context: BlockEntityRendererProvider.Context) : KineticBlockEntityRenderer<ReactionWheelBlockEntity>(
    context
) {
    override fun renderSafe(
        be: ReactionWheelBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)

        //if (Backend.canUseInstancing(be!!.level)) return

        val blockState = be.blockState

        val speed: Float = be.clientSpeed.getValue(partialTicks) * 3 / 10f
        val angle: Float = be.angle.toFloat() + speed * partialTicks

        val vb = buffer.getBuffer(RenderType.solid())
        renderFlywheel(be, ms, light, blockState, angle, vb)
    }

    private fun renderFlywheel(
        be: ReactionWheelBlockEntity, ms: PoseStack, light: Int, blockState: BlockState, angle: Float,
        vb: VertexConsumer
    ) {
        val wheel = CachedBuffers.block(blockState)
        kineticRotationTransform(wheel, be, getRotationAxisOf(be), AngleHelper.rad(angle.toDouble()), light)
        wheel.renderInto(ms, vb)
    }

    override fun getRenderedBlockState(be: ReactionWheelBlockEntity?): BlockState {
        return shaft(getRotationAxisOf(be))
    }
}
