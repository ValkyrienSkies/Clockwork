package org.valkyrienskies.clockwork.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
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
        val middle = applyColor(color, CachedBufferer.partial(ClockworkPartials.WING_MIDDLE, state))
        val side = CachedBufferer.partial(ClockworkPartials.WING_SIDE, state)
        val sideVertical = CachedBufferer.partial(ClockworkPartials.WING_SIDE_VERTICAL, state)
        when (facing) {
            Direction.NORTH, Direction.SOUTH -> middle.rotateCentered(Direction.EAST, Math.toRadians(90.0).toFloat())
                .light(light).renderInto(ms, vb)

            Direction.EAST, Direction.WEST -> middle.rotateCentered(Direction.NORTH, Math.toRadians(90.0).toFloat())
                .light(light).renderInto(ms, vb)

            else -> middle.light(light).renderInto(ms, vb)
        }
        if (state.getValue(BlockStateProperties.NORTH)) {
            when (facing) {
                Direction.EAST, Direction.WEST -> applyColor(color, sideVertical)
                    .light(light).renderInto(ms, vb)

                Direction.UP, Direction.DOWN -> applyColor(color, side)
                    .light(light).renderInto(ms, vb)

                else -> {}
            }
        }
        if (state.getValue(BlockStateProperties.SOUTH)) {
            when (facing) {
                Direction.EAST, Direction.WEST -> applyColor(color, sideVertical)
                    .rotateCentered(Direction.UP, Math.toRadians(180.0).toFloat())
                    .light(light).renderInto(ms, vb)

                Direction.UP, Direction.DOWN -> applyColor(color, side)
                    .rotateCentered(Direction.UP, Math.toRadians(180.0).toFloat())
                    .light(light).renderInto(ms, vb)

                else -> {}
            }
        }
        if (state.getValue(BlockStateProperties.EAST)) {
            when (facing) {
                Direction.NORTH, Direction.SOUTH -> applyColor(color, sideVertical)
                    .rotateCentered(Direction.UP, Math.toRadians(270.0).toFloat())
                    .light(light).renderInto(ms, vb)

                Direction.UP, Direction.DOWN -> applyColor(color, side)
                    .rotateCentered(Direction.UP, Math.toRadians(270.0).toFloat())
                    .light(light).renderInto(ms, vb)

                else -> {}
            }
        }
        if (state.getValue(BlockStateProperties.WEST)) {
            when (facing) {
                Direction.NORTH, Direction.SOUTH -> applyColor(color, sideVertical)
                    .rotateCentered(Direction.UP, Math.toRadians(90.0).toFloat())
                    .light(light).renderInto(ms, vb)

                Direction.UP, Direction.DOWN -> applyColor(color, side)
                    .rotateCentered(Direction.UP, Math.toRadians(90.0).toFloat())
                    .light(light).renderInto(ms, vb)

                else -> {}
            }
        }
        if (state.getValue(BlockStateProperties.UP)) {
            when (facing) {
                Direction.NORTH, Direction.SOUTH -> applyColor(color, side)
                    .rotateCentered(Direction.EAST, Math.toRadians(90.0).toFloat())
                    .light(light).renderInto(ms, vb)

                Direction.EAST, Direction.WEST -> applyColor(color, side)
                    .rotateCentered(Direction.EAST, Math.toRadians(90.0).toFloat())
                    .rotateCentered(Direction.NORTH, Math.toRadians(270.0).toFloat())
                    .light(light).renderInto(ms, vb)

                else -> {}
            }
        }
        if (state.getValue(BlockStateProperties.DOWN)) {
            when (facing) {
                Direction.NORTH, Direction.SOUTH -> applyColor(color, side)
                    .rotateCentered(Direction.EAST, Math.toRadians(270.0).toFloat())
                    .light(light).renderInto(ms, vb)

                Direction.EAST, Direction.WEST -> applyColor(color, side)
                    .rotateCentered(Direction.NORTH, Math.toRadians(90.0).toFloat())
                    .rotateCentered(Direction.UP, Math.toRadians(270.0).toFloat())
                    .light(light).renderInto(ms, vb)

                else -> {}
            }
        }
    }

    private fun applyColor(color: Int, buf: SuperByteBuffer): SuperByteBuffer {
        if (color != -1) buf.color(color)
        return buf
    }
}
