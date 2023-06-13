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

public class WingBlockEntityRenderer extends SmartTileEntityRenderer<ColorBlockEntity> {
    public WingBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(ColorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        VertexConsumer vb = buffer.getBuffer(RenderType.cutout());
        BlockState state = be.getBlockState();

        SuperByteBuffer middle = CachedBufferer.partial(ClockWorkPartials.WING_MIDDLE, state);

        int color = be.getColor();
        if (color != -1)
            middle.color(color);

        switch (state.getValue(BlockStateProperties.FACING)) {
            case NORTH, SOUTH -> middle.rotateCentered(Direction.EAST, (float) Math.toRadians(90)).light().renderInto(ms, vb);
            case EAST, WEST -> middle.rotateCentered(Direction.NORTH, (float) Math.toRadians(90)).light().renderInto(ms, vb);
            default -> middle.light().renderInto(ms, vb);
        }
    }
}
