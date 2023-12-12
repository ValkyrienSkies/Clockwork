package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import com.jozufozu.flywheel.backend.Backend
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.utility.AngleHelper
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.level.block.state.BlockState


class GyroBlockEntityRenderer(context: BlockEntityRendererProvider.Context?) : KineticBlockEntityRenderer<GyroBlockEntity>(context) {

    override fun renderSafe(
        be: GyroBlockEntity?,
        partialTicks: Float,
        ms: PoseStack?,
        buffer: MultiBufferSource?,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)

        if (Backend.canUseInstancing(be!!.level)) {
            return
        }

        val blockState: BlockState = be.getBlockState()
        val speed: Float = be.visualSpeed.getValue(partialTicks) * 3 / 10f
        val angle: Float = be.angle + speed * partialTicks

        val vb = buffer!!.getBuffer(RenderType.solid())
        renderGyro(be, ms, light, blockState, angle, vb)
    }

    private fun renderGyro(be: GyroBlockEntity, ms: PoseStack?, light: Int, blockState: BlockState, angle: Float, vb: VertexConsumer) {
        val wheel = CachedBufferer.block(blockState)
        kineticRotationTransform(wheel, be, getRotationAxisOf(be), AngleHelper.rad(angle.toDouble()), light)
        wheel.renderInto(ms, vb)
    }
}