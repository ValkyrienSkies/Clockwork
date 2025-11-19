package org.valkyrienskies.clockwork.content.kinetics.resistor

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.animation.AnimationTickHolder
import net.createmod.catnip.data.Iterate
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.createmod.catnip.theme.Color
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkPartials

class RedstoneResistorRenderer(context: BlockEntityRendererProvider.Context?) :
    KineticBlockEntityRenderer<RedstoneResistorBlockEntity>(context) {
    protected override fun renderSafe(
        te: RedstoneResistorBlockEntity, partialTicks: Float, ms: PoseStack, buffer: MultiBufferSource,
        light: Int, overlay: Int
    ) {
        val block = te.blockState.block
        val boxAxis = (block as IRotate).getRotationAxis(te.blockState)
        val pos = te.blockPos
        val time = AnimationTickHolder.getRenderTime(te.level)
        if (te !is RedstoneResistorBlockEntity) {
            return
        }
        val resistor = te
        for (direction in Iterate.directions) {
            val axis = direction.axis
            if (boxAxis !== axis) continue
            val offset = getRotationOffsetForPosition(te, pos, axis)
            var angle = time * te.speed * 3f / 10 % 360
            var modifier = 1f
            modifier = resistor.getRotationSpeedModifier(direction)
            angle *= modifier
            angle += offset
            angle = angle / 180f * Math.PI.toFloat()
            val superByteBuffer = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, te.blockState, direction)
            kineticRotationTransform(superByteBuffer, te, axis, angle, light)
            superByteBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()))
        }
        val resistorState = te.blockState
        val vb = buffer.getBuffer(RenderType.solid())
        val color = Color.mixColors(0x2C0300, 0xCD0000, resistor.state / 15f)
        val indicator =
            transform(CachedBuffers.partial(ClockworkPartials.RESISTOR_INDICATOR, resistorState), resistorState)
        indicator.light<SuperByteBuffer>(light)
            .color<SuperByteBuffer>(color)
            .renderInto(ms, vb)
    }

    private fun transform(buffer: SuperByteBuffer, resistorState: BlockState): SuperByteBuffer {
        var buffer = buffer
        val axis = resistorState.getValue<Direction.Axis>(BlockStateProperties.AXIS)
        return when (axis) {
            Direction.Axis.X -> buffer.rotateCentered(Math.toRadians(90.0).toFloat(), Direction.NORTH)
            Direction.Axis.Y -> buffer
            Direction.Axis.Z -> buffer.rotateCentered(Math.toRadians(90.0).toFloat(), Direction.EAST)
        }.also {
            buffer = it
        }
    }
}
