package org.valkyrienskies.clockwork.content.contraptions.phys.slicker

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Quaternion
import com.mojang.math.Vector3f
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.SlickerBlock.Companion.EXTENDED

class SlickerBlockEntityRenderer(context: BlockEntityRendererProvider.Context) :
    SmartBlockEntityRenderer<SlickerBlockEntity>(
        context
    ) {

    var wasAttached = false

    var shouldRenderDoink = false

    var currentDoinkSize = 0.0
    var currentDoinkTransparency = 1.0

    var targetDoinkSize = 0.0
    val targetDoinkTransparency = 0.0

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

        val attached = blockEntity.shipStuck

        matrices.pushPose()
        matrices.translate(0.5, 0.5, 0.5)
        matrices.mulPose(Quaternion.fromXYZ(0.0f, Math.toRadians(-180.0).toFloat(), 0.0f))

        when (facing) {
            Direction.SOUTH -> matrices.mulPose(Vector3f.XP.rotationDegrees(270f))
            Direction.WEST -> {
                matrices.mulPose(Vector3f.ZP.rotationDegrees(270f))
                matrices.mulPose(Vector3f.YP.rotationDegrees(90f))
            }

            Direction.NORTH -> matrices.mulPose(Vector3f.XP.rotationDegrees(90f))
            Direction.EAST -> {
                matrices.mulPose(Vector3f.ZP.rotationDegrees(90f))
                matrices.mulPose(Vector3f.YP.rotationDegrees(90f))
            }

            Direction.UP -> matrices.mulPose(Vector3f.XP.rotationDegrees(0f))
            Direction.DOWN -> matrices.mulPose(Vector3f.XN.rotationDegrees(180f))
        }

        matrices.translate(-0.5, -0.5, -0.5)

        val goo = CachedBufferer.partial(ClockworkPartials.GOO, blockState)

        val gooOffset = blockEntity.piston?.getValue(partialTicks) ?: 0.0f

        goo.light(light).translate(0.0, gooOffset.toDouble() - (2.0 / 16.0), 0.0)
            .renderInto(matrices, buffer.getBuffer(RenderType.translucent()))

        if (attached != wasAttached) {
            shouldRenderDoink = true

            targetDoinkSize = if (attached) 2.0 else 0.0
            currentDoinkTransparency = 1.0
        }

        if (shouldRenderDoink) {
            val doink = CachedBufferer.partial(ClockworkPartials.DOINK, blockState)
            currentDoinkSize = Mth.lerp(partialTicks.toDouble(), currentDoinkSize, targetDoinkSize)
            currentDoinkTransparency =
                Mth.lerp(partialTicks.toDouble() / 2, currentDoinkTransparency, targetDoinkTransparency)

            doink.light(light).scale(currentDoinkSize.toFloat()).color(
                (1.0f * 255).toInt(),
                (1.0f * 255).toInt(), (1.0f * 255).toInt(), (currentDoinkTransparency.toFloat() * 255).toInt()
            ).renderInto(matrices, buffer.getBuffer(RenderType.translucent()))

            if (currentDoinkSize == targetDoinkSize && currentDoinkTransparency == targetDoinkTransparency) {
                shouldRenderDoink = false
            }
        }

        matrices.popPose()

        wasAttached = blockEntity.shipStuck

        super.renderSafe(blockEntity, partialTicks, matrices, buffer, light, overlay)
    }
}