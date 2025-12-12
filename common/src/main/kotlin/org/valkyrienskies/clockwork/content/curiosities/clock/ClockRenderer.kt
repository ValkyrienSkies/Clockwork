package org.valkyrienskies.clockwork.content.curiosities.clock

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.util.EaseHelper
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.util.toMinecraft

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

        val secondHandRotation = AngleHelper.rad(AngleHelper.angleLerp(partialTicks.toDouble(),
            AngleHelper.deg(currentSecondHandRotation).toDouble(),
            AngleHelper.deg(blockEntity.secondHandTargetRotation).toDouble()
        ).toDouble())
        val minuteHandRotation = AngleHelper.rad(AngleHelper.angleLerp(partialTicks.toDouble(),
            AngleHelper.deg(currentMinuteHandRotation).toDouble(),
            AngleHelper.deg(blockEntity.minuteHandTargetRotation).toDouble()
        ).toDouble())
        val hourHandRotation = AngleHelper.rad(AngleHelper.angleLerp(partialTicks.toDouble(),
            AngleHelper.deg(currentHourHandRotation).toDouble(),
            AngleHelper.deg(blockEntity.hourHandTargetRotation).toDouble()
        ).toDouble())

        val clockRing = CachedBuffers.partial(ClockworkPartials.CLOCK_FRAME, blockEntity.blockState)
        val secondHand = CachedBuffers.partial(ClockworkPartials.HAND_SECOND, blockEntity.blockState)
        val minuteHand = CachedBuffers.partial(ClockworkPartials.HAND_MINUTE, blockEntity.blockState)
        val hourHand = CachedBuffers.partial(ClockworkPartials.HAND_HOUR, blockEntity.blockState)

        val vb = buffer.getBuffer(RenderType.solid())
        val facing = blockEntity.blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)

        ms.pushPose()
        clockRing.rotateCentered(
            AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble()),
            Direction.UP
        )
        clockRing.translate(0.5, 0.0, 0.5)
        clockRing.light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay).renderInto(ms, vb)
        ms.popPose()
        ms.pushPose()
        rotateHand(secondHand, secondHandRotation, facing).light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay).renderInto(ms, vb)
        rotateHand(minuteHand, minuteHandRotation, facing).light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay).renderInto(ms, vb)
        rotateHand(hourHand, hourHandRotation, facing).light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay).renderInto(ms, vb)
        ms.popPose()

        currentSecondHandRotation = secondHandRotation.toDouble()
        currentMinuteHandRotation = minuteHandRotation.toDouble()
        currentHourHandRotation = hourHandRotation.toDouble()
    }

    private fun rotateHand(buffer: SuperByteBuffer, angle: Float, facing: Direction): SuperByteBuffer {
        val pivotX = 8 / 16f
        val pivotY = 8 / 16f
        val pivotZ = 14 / 16f
        buffer.rotateCentered(
            AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble()),
            Direction.UP
        )
        buffer.translate(pivotX.toDouble(), pivotY.toDouble(), pivotZ.toDouble())
        buffer.rotate(angle, Direction.SOUTH)
        buffer.translate(-pivotX.toDouble(), -pivotY.toDouble(), -pivotZ.toDouble())
        return buffer
    }
}
