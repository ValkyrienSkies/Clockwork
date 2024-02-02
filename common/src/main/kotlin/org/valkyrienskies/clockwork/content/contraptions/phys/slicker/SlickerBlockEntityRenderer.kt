package org.valkyrienskies.clockwork.content.contraptions.phys.slicker

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPartials

class SlickerBlockEntityRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<SlickerBlockEntity>(
    context
) {
    override fun renderSafe(
        blockEntity: SlickerBlockEntity?,
        partialTicks: Float,
        matrices: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {


        if (blockEntity !is SlickerBlockEntity) return

        val blockState = blockEntity.blockState
        val facing = blockState.getValue(BlockStateProperties.FACING)

        matrices.pushPose()
        matrices.translate(0.5, 0.5, 0.5)
        matrices.mulPose(Quaternionf().rotationXYZ(0.0f, Math.toRadians(-180.0).toFloat(), 0.0f))

        when (facing) {
            Direction.SOUTH -> matrices.mulPose(Axis.XP.rotationDegrees(270f))
            Direction.WEST -> {
                matrices.mulPose(Axis.ZP.rotationDegrees(270f))
                matrices.mulPose(Axis.YP.rotationDegrees(90f))
            }
            Direction.NORTH -> matrices.mulPose(Axis.XP.rotationDegrees(90f))
            Direction.EAST -> {
                matrices.mulPose(Axis.ZP.rotationDegrees(90f))
                matrices.mulPose(Axis.YP.rotationDegrees(90f))
            }
            Direction.UP -> matrices.mulPose(Axis.XP.rotationDegrees(0f))
            Direction.DOWN -> matrices.mulPose(Axis.XN.rotationDegrees(180f))
        }

        matrices.translate(-0.5, -0.5, -0.5)

        val goo = CachedBufferer.partial(ClockworkPartials.GOO, blockState)

        goo.light(light).renderInto(matrices, buffer.getBuffer(RenderType.translucent()))

        matrices.popPose()

        super.renderSafe(blockEntity, partialTicks, matrices, buffer, light, overlay)
    }
}