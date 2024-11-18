package org.valkyrienskies.clockwork.content.curiosities.clock

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.util.EaseHelper

class ClockRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<ClockBlockEntity>(context) {

    var currentSecondHandRotation = 0.0
    var currentMinuteHandRotation = 0.0
    var currentHourHandRotation = 0.0
    override fun renderSafe(
        blockEntity: ClockBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay)

        if (blockEntity.level == null) return
        blockEntity.calcHandRotation(blockEntity.doingTrailerAnim)

        if (blockEntity.doingTrailerAnim && blockEntity.trailerAnimProgress < 1.0) {
            blockEntity.trailerAnimProgress += 0.01
        }

        val secondHandRotation = if (blockEntity.secondHandTargetRotation % 6.0 == 0.0) Mth.rotLerp(EaseHelper.easeOutElastic(partialTicks), currentSecondHandRotation.toFloat(), blockEntity.secondHandTargetRotation.toFloat()) else currentSecondHandRotation.toFloat()
        val minuteHandRotation = Mth.rotLerp(partialTicks, currentMinuteHandRotation.toFloat(), blockEntity.minuteHandTargetRotation.toFloat())
        val hourHandRotation = Mth.rotLerp(partialTicks, currentHourHandRotation.toFloat(), blockEntity.hourHandTargetRotation.toFloat())

        val clockRing = CachedBufferer.partialFacing(ClockworkPartials.CLOCK_FRAME, blockEntity.blockState)
        val secondHand = CachedBufferer.partialFacing(ClockworkPartials.HAND_SECOND, blockEntity.blockState)
        val minuteHand = CachedBufferer.partialFacing(ClockworkPartials.HAND_MINUTE, blockEntity.blockState)
        val hourHand = CachedBufferer.partialFacing(ClockworkPartials.HAND_HOUR, blockEntity.blockState)

        val vb = buffer.getBuffer(RenderType.cutout())
        val facing = blockEntity.blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)

        ms.pushPose()
        clockRing.renderInto(ms, vb)
        secondHand.rotate(facing, secondHandRotation).light(light).overlay(overlay).renderInto(ms, vb)
        minuteHand.rotate(facing, minuteHandRotation).light(light).overlay(overlay).renderInto(ms, vb)
        hourHand.rotate(facing, hourHandRotation).light(light).overlay(overlay).renderInto(ms, vb)
        ms.popPose()

        currentSecondHandRotation = secondHandRotation.toDouble()
        currentMinuteHandRotation = minuteHandRotation.toDouble()
        currentHourHandRotation = hourHandRotation.toDouble()
    }
}