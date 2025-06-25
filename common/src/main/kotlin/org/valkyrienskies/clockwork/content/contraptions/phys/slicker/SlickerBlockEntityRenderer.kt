package org.valkyrienskies.clockwork.content.contraptions.phys.slicker

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.utility.AngleHelper
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.SlickerBlock.Companion.EXTENDED

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
        matrices.mulPose(Quaternionf().rotateXYZ(0.0f, Math.toRadians(-180.0).toFloat(), 0.0f))

        when (facing) {
            Direction.SOUTH -> matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(270.0), 1f, 0f, 0f)))
            Direction.WEST -> {
                matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(270.0), 0f, 0f, 1f)))
                matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(90.0), 0f, 1f, 0f)))
            }
            Direction.NORTH -> matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(90.0), 1f, 0f, 0f)))
            Direction.EAST -> {
                matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(90.0), 0f, 0f, 1f)))
                matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(90.0), 0f, 1f, 0f)))
            }
            Direction.UP -> matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(0.0), 1f, 0f, 0f)))
            Direction.DOWN -> matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(180.0), -1f, 0f, 0f)))
        }

        matrices.translate(-0.5, -0.5, -0.5)

        val goo = CachedBufferer.partial(ClockworkPartials.GOO, blockState)

        val gooOffset = blockEntity.piston?.getValue(partialTicks) ?: 0.0f

        goo.light(light).translate(0.0, gooOffset.toDouble()-(2.0/16.0), 0.0).renderInto(matrices, buffer.getBuffer(RenderType.translucent()))

        if (blockEntity.shouldRenderDoink) {
            val doink = CachedBufferer.partial(ClockworkPartials.DOINK, blockState)
            blockEntity.currentDoinkSize = Mth.lerp(partialTicks.toDouble(), blockEntity.currentDoinkSize, blockEntity.targetDoinkSize)

            doink.light(light).scale(blockEntity.currentDoinkSize.toFloat()).renderInto(matrices, buffer.getBuffer(RenderType.translucent()))

            if (blockEntity.currentDoinkSize == blockEntity.targetDoinkSize) {
                blockEntity.shouldRenderDoink = false
            }
        }

        matrices.popPose()

        super.renderSafe(blockEntity, partialTicks, matrices, buffer, light, overlay)
    }
}