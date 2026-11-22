package org.valkyrienskies.clockwork.content.curiosities.harpoon

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkPartials

class HarpoonRenderer(context: BlockEntityRendererProvider.Context) : KineticBlockEntityRenderer<HarpoonBlockEntity>(context) {
    override fun renderSafe(be: HarpoonBlockEntity, partialTicks: Float, ms: PoseStack, buffers: MultiBufferSource,
                            light: Int, overlay: Int) {
        val facing = be.blockState.getValue(BlockStateProperties.FACING)
        val shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.blockState, facing.opposite)
        standardKineticRotationTransform(shaft, be, light)
            .overlay<SuperByteBuffer>(overlay).renderInto(ms, buffers.getBuffer(RenderType.solid()))

        // The supplied models face up: keep rotation and recoil in their local coordinates.
        ms.pushPose()
        ms.translate(0.5, 0.5, 0.5)
        // Match the JSON blockstate rotations exactly, including texture roll.
        when (facing) {
            Direction.UP -> Unit
            Direction.DOWN -> ms.mulPose(Axis.XP.rotationDegrees(180f))
            else -> {
                ms.mulPose(Axis.YP.rotationDegrees(-(facing.toYRot() + 180f)))
                ms.mulPose(Axis.XP.rotationDegrees(-90f))
            }
        }
        ms.translate(-0.5, -0.5, -0.5)
        val buffer = buffers.getBuffer(RenderType.cutoutMipped())
        CachedBuffers.partial(ClockworkPartials.HARPOON_COG, be.blockState)
            .rotateCentered(be.getCogAngle(partialTicks), Direction.UP)
            .light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay).renderInto(ms, buffer)
        CachedBuffers.partial(ClockworkPartials.HARPOON_CHUTE, be.blockState)
            .translate(0.0, -be.getRecoil(partialTicks).toDouble(), 0.0)
            .light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay).renderInto(ms, buffer)
        if (!be.loadedHarpoon.isEmpty && !be.hasDeployedHarpoon) {
            // Pull the harpoon into the launcher as it charges
            CachedBuffers.partial(ClockworkPartials.HARPOON, be.blockState)
                .translate(0.0, 0.5 - HarpoonBlockEntity.sanitizeCharge(be.charge) * 4.0 / 16.0, 0.0)
                .light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay).renderInto(ms, buffer)
        }
        ms.popPose()
    }
}
