package org.valkyrienskies.clockwork.content.physicalities.reaction_wheel

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.utility.AngleHelper
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkPartials

class ReactionWheelRenderer(context: BlockEntityRendererProvider.Context?) :
    KineticBlockEntityRenderer<ReactionWheelBlockEntity>(context) {
    override fun renderSafe(
        te: ReactionWheelBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay)

        // if (Backend.canUseInstancing(te.getLevel()))
        //     return;
        val blockState: BlockState = te.blockState
        val wte = te
        val speed: Float = wte.rotspeed * 3 / 10f
        val angle: Float = wte.angle + speed * partialTicks
        val vb = buffer.getBuffer(RenderType.solid())
        renderWheels(te, ms, light, blockState, angle, vb)
    }

    private fun renderWheels(
        te: ReactionWheelBlockEntity, ms: PoseStack, light: Int, blockState: BlockState, angle: Float,
        vb: VertexConsumer
    ) {
        val direction = when (blockState.getValue(BlockStateProperties.AXIS)) {
            Direction.Axis.X -> Direction.NORTH
            Direction.Axis.Y -> Direction.UP
            Direction.Axis.Z -> Direction.EAST
        }
        val offset: Float = when (blockState.getValue(BlockStateProperties.AXIS)) {
            Direction.Axis.X, Direction.Axis.Z -> 90f
            Direction.Axis.Y -> 0f
        }
        val wheelBottom = CachedBufferer.partial(ClockworkPartials.WHEEL_BOTTOM, blockState)
        val wheelTop = CachedBufferer.partial(ClockworkPartials.WHEEL_TOP, blockState)
        kineticRotationTransform(
            wheelBottom,
            te,
            blockState.getValue(BlockStateProperties.AXIS),
            AngleHelper.rad(-angle.toDouble()),
            light
        )
        kineticRotationTransform(
            wheelTop,
            te,
            blockState.getValue(BlockStateProperties.AXIS),
            AngleHelper.rad(angle.toDouble()),
            light
        )
        wheelBottom.rotateCentered(direction, Math.toRadians(offset.toDouble()).toFloat())
        wheelTop.rotateCentered(direction, Math.toRadians(offset.toDouble()).toFloat())
        wheelTop.renderInto(ms, vb)
        wheelBottom.renderInto(ms, vb)
    }

    override fun getRenderedBlockState(te: ReactionWheelBlockEntity): BlockState {
        return shaft(getRotationAxisOf(te))
    }
}