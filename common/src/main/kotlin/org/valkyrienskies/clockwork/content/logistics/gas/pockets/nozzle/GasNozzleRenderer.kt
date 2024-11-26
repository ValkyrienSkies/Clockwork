package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import com.jozufozu.flywheel.backend.Backend
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.AnimationTickHolder
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
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
        var pointer = CachedBufferer.partial(ClockworkPartials.NOZZLE_DIAL, blockState)
        val facing = blockState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING)

        val pointerRotation = Mth.DEG_TO_RAD * Mth.lerp(be.pointer.getValue(partialTicks), 225f, 135f)

        val dialOffset = Vec3(0.0,-0.1,0.0)

        rotateBufferTowards(pointer, facing.clockWise)
            .translate(dialOffset)
            .rotateCentered(Direction.NORTH, pointerRotation)
            .translate(dialOffset.reverse())
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

    fun rotateBufferTowards(buffer: SuperByteBuffer, target: Direction): SuperByteBuffer {
        return buffer.rotateCentered(Direction.UP, ((-target.toYRot() - 90) / 180 * Math.PI).toFloat())
    }
}