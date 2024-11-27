package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import com.jozufozu.flywheel.backend.Backend
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.AnimationTickHolder
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.core.Direction.Axis
import net.minecraft.util.Mth
import org.valkyrienskies.clockwork.ClockworkPartials

class GasNozzleRenderer(context: BlockEntityRendererProvider.Context?) : KineticBlockEntityRenderer<GasNozzleBlockEntity>(
    context
) {
    override fun renderSafe(
        be: GasNozzleBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)

        val blockState = be.blockState
        val pointer = CachedBufferer.partial(ClockworkPartials.NOZZLE_DIAL, blockState)
        val facing = blockState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING)

        val pointerRotation = Mth.lerp(be.pointer.getValue(partialTicks), 0f, -90f)

        pointer.centre()
            .rotateCentered(Direction.UP, AngleHelper.rad(AngleHelper.horizontalAngle(facing).toDouble()))
            .rotateCentered(facing, AngleHelper.rad(pointerRotation.toDouble()))
            .unCentre()
            .light(light)
            .renderInto(ms, buffer.getBuffer(RenderType.solid()))

        if (Backend.canUseInstancing(be.level)) return
        val time = AnimationTickHolder.getRenderTime(be.level)
        val rotdir = blockState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING).clockWise
        val rotaxis = rotdir.axis
        val offset = getRotationOffsetForPosition(be, be.blockPos, rotaxis)
        var angle = time * be.speed * 3f / 10 % 360
        angle += offset
        angle = angle / 180f * Math.PI.toFloat()
        val axis = CachedBufferer.partialFacing(ClockworkPartials.NOZZLE_AXIS, blockState, rotdir)
        kineticRotationTransform(axis, be, rotaxis, angle, light)
        axis.renderInto(ms, buffer.getBuffer(RenderType.solid()))
    }
}