package org.valkyrienskies.clockwork.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.generic.ColorBlockEntity

class WingBlockEntityRenderer(context: BlockEntityRendererProvider.Context?) :
    SmartBlockEntityRenderer<ColorBlockEntity>(context) {
    override fun renderSafe(
        be: ColorBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)
        val vb = buffer.getBuffer(RenderType.cutout())
        val state: BlockState = be.blockState
        val color: Int = be.getColor()
        val facing = state.getValue(BlockStateProperties.FACING)
        val middle = applyColor(color, CachedBuffers.partial(ClockworkPartials.WING_MIDDLE, state))
        val side = CachedBuffers.partial(ClockworkPartials.WING_SIDE, state)
        val sideVertical = CachedBuffers.partial(ClockworkPartials.WING_SIDE_VERTICAL, state)
        when (facing) {
            Direction.NORTH, Direction.SOUTH -> middle.rotateCentered(Math.toRadians(90.0).toFloat(), Direction.EAST)
                .light<SuperByteBuffer>(light).renderInto(ms, vb)

            Direction.EAST, Direction.WEST -> middle.rotateCentered(Math.toRadians(90.0).toFloat(), Direction.NORTH)
                .light<SuperByteBuffer>(light).renderInto(ms, vb)

            else -> middle.light<SuperByteBuffer>(light).renderInto(ms, vb)
        }
        if (state.getValue(BlockStateProperties.NORTH)) {
            when (facing) {
                Direction.EAST, Direction.WEST -> applyColor(color, sideVertical)
                    .light<SuperByteBuffer>(light).renderInto(ms, vb)

                Direction.UP, Direction.DOWN -> applyColor(color, side)
                    .light<SuperByteBuffer>(light).renderInto(ms, vb)

                else -> {}
            }
        }
        if (state.getValue(BlockStateProperties.SOUTH)) {
            when (facing) {
                Direction.EAST, Direction.WEST -> applyColor(color, sideVertical)
                    .rotateCentered(Math.toRadians(180.0).toFloat(), Direction.UP)
                    .light<SuperByteBuffer>(light).renderInto(ms, vb)

                Direction.UP, Direction.DOWN -> applyColor(color, side)
                    .rotateCentered(Math.toRadians(180.0).toFloat(), Direction.UP)
                    .light<SuperByteBuffer>(light).renderInto(ms, vb)

                else -> {}
            }
        }
        if (state.getValue(BlockStateProperties.EAST)) {
            when (facing) {
                Direction.NORTH, Direction.SOUTH -> applyColor(color, sideVertical)
                    .rotateCentered(Math.toRadians(270.0).toFloat(), Direction.UP)
                    .light<SuperByteBuffer>(light).renderInto(ms, vb)

                Direction.UP, Direction.DOWN -> applyColor(color, side)
                    .rotateCentered(Math.toRadians(270.0).toFloat(), Direction.UP)
                    .light<SuperByteBuffer>(light).renderInto(ms, vb)

                else -> {}
            }
        }
        if (state.getValue(BlockStateProperties.WEST)) {
            when (facing) {
                Direction.NORTH, Direction.SOUTH -> applyColor(color, sideVertical)
                    .rotateCentered(Math.toRadians(90.0).toFloat(), Direction.UP)
                    .light<SuperByteBuffer>(light).renderInto(ms, vb)

                Direction.UP, Direction.DOWN -> applyColor(color, side)
                    .rotateCentered(Math.toRadians(90.0).toFloat(), Direction.UP)
                    .light<SuperByteBuffer>(light).renderInto(ms, vb)

                else -> {}
            }
        }
        if (state.getValue(BlockStateProperties.UP)) {
            when (facing) {
                Direction.NORTH, Direction.SOUTH -> applyColor(color, side)
                    .rotateCentered(Math.toRadians(90.0).toFloat(), Direction.EAST)
                    .light<SuperByteBuffer>(light).renderInto(ms, vb)

                Direction.EAST, Direction.WEST -> applyColor(color, side)
                    .rotateCentered(Math.toRadians(90.0).toFloat(), Direction.EAST)
                    .rotateCentered(Math.toRadians(270.0).toFloat(), Direction.NORTH)
                    .light<SuperByteBuffer>(light).renderInto(ms, vb)

                else -> {}
            }
        }
        if (state.getValue(BlockStateProperties.DOWN)) {
            when (facing) {
                Direction.NORTH, Direction.SOUTH -> applyColor(color, side)
                    .rotateCentered(Math.toRadians(270.0).toFloat(), Direction.EAST)
                    .light<SuperByteBuffer>(light).renderInto(ms, vb)

                Direction.EAST, Direction.WEST -> applyColor(color, side)
                    .rotateCentered(Math.toRadians(90.0).toFloat(), Direction.NORTH)
                    .rotateCentered(Math.toRadians(270.0).toFloat(), Direction.UP)
                    .light<SuperByteBuffer>(light).renderInto(ms, vb)

                else -> {}
            }
        }
    }

    private fun applyColor(color: Int, buf: SuperByteBuffer): SuperByteBuffer {
        if (color != -1) buf.color<SuperByteBuffer>(color)
        return buf
    }
}
