package org.valkyrienskies.clockwork.util.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.tileEntity.renderer.SmartTileEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.valkyrienskies.clockwork.ClockWorkPartials;
import org.valkyrienskies.clockwork.content.materials.solids.colorblock.ColorBlockEntity;
import org.valkyrienskies.clockwork.util.blocktype.ConnectedWingAlike;

public class WingBlockEntityRenderer extends SmartTileEntityRenderer<ColorBlockEntity> {
    public WingBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(ColorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        VertexConsumer vb = buffer.getBuffer(RenderType.cutout());
        BlockState state = be.getBlockState();
        int color = be.getColor();
        Direction facing = state.getValue(BlockStateProperties.FACING);

        SuperByteBuffer middle = applyColor(color, CachedBufferer.partial(ClockWorkPartials.WING_MIDDLE, state));
        SuperByteBuffer side = CachedBufferer.partial(ClockWorkPartials.WING_SIDE, state);
        SuperByteBuffer side_vertical = CachedBufferer.partial(ClockWorkPartials.WING_SIDE_VERTICAL, state);

        switch (facing) {
            case NORTH, SOUTH -> middle.rotateCentered(Direction.EAST, (float) Math.toRadians(90)).light(light).renderInto(ms, vb);
            case EAST, WEST -> middle.rotateCentered(Direction.NORTH, (float) Math.toRadians(90)).light(light).renderInto(ms, vb);
            default -> middle.light(light).renderInto(ms, vb);
        }

        if (state.getValue(BlockStateProperties.NORTH)) {
            switch (facing) {
                case EAST, WEST -> applyColor(color, side_vertical)
                        .light(light).renderInto(ms, vb);
                case UP, DOWN -> applyColor(color, side)
                        .light(light).renderInto(ms, vb);
            }
        }

        if (state.getValue(BlockStateProperties.SOUTH)) {
            switch (facing) {
                case EAST, WEST -> applyColor(color, side_vertical)
                        .rotateCentered(Direction.UP, (float) Math.toRadians(180))
                        .light(light).renderInto(ms, vb);
                case UP, DOWN -> applyColor(color, side)
                        .rotateCentered(Direction.UP, (float) Math.toRadians(180))
                        .light(light).renderInto(ms, vb);
            }
        }

        if (state.getValue(BlockStateProperties.EAST)) {
            switch (facing) {
                case NORTH, SOUTH -> applyColor(color, side_vertical)
                        .rotateCentered(Direction.UP, (float) Math.toRadians(270))
                        .light(light).renderInto(ms, vb);
                case UP, DOWN -> applyColor(color, side)
                        .rotateCentered(Direction.UP, (float) Math.toRadians(270))
                        .light(light).renderInto(ms, vb);
            }
        }

        if (state.getValue(BlockStateProperties.WEST)) {
            switch (facing) {
                case NORTH, SOUTH -> applyColor(color, side_vertical)
                        .rotateCentered(Direction.UP, (float) Math.toRadians(90))
                        .light(light).renderInto(ms, vb);
                case UP, DOWN -> applyColor(color, side)
                        .rotateCentered(Direction.UP, (float) Math.toRadians(90))
                        .light(light).renderInto(ms, vb);
            }
        }

        if (state.getValue(BlockStateProperties.UP)) {
            switch (facing) {
                case NORTH, SOUTH -> applyColor(color, side)
                        .rotateCentered(Direction.EAST, (float) Math.toRadians(90))
                        .light(light).renderInto(ms, vb);
                case EAST, WEST -> applyColor(color, side)
                        .rotateCentered(Direction.EAST, (float) Math.toRadians(90))
                        .rotateCentered(Direction.NORTH, (float) Math.toRadians(270))
                        .light(light).renderInto(ms, vb);
            }
        }

        if (state.getValue(BlockStateProperties.DOWN)) {
            switch (facing) {
                case NORTH, SOUTH -> applyColor(color, side)
                        .rotateCentered(Direction.EAST, (float) Math.toRadians(270))
                        .light(light).renderInto(ms, vb);
                case EAST, WEST -> applyColor(color, side)
                        .rotateCentered(Direction.NORTH, (float) Math.toRadians(90))
                        .rotateCentered(Direction.UP, (float) Math.toRadians(270))
                        .light(light).renderInto(ms, vb);
            }
        }
    }

    private SuperByteBuffer applyColor(int color, SuperByteBuffer buf) {
        if (color != -1)
            buf.color(color);
        return buf;
    }
}
