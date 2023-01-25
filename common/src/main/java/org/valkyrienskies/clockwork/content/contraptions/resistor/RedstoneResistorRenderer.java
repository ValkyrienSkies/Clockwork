package org.valkyrienskies.clockwork.content.contraptions.resistor;

import com.jozufozu.flywheel.backend.Backend;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.base.IRotate;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.base.KineticTileEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.ClockWorkPartials;

public class RedstoneResistorRenderer extends KineticTileEntityRenderer {

    public RedstoneResistorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(KineticTileEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {

        Block block = te.getBlockState().getBlock();
        final Direction.Axis boxAxis = ((IRotate) block).getRotationAxis(te.getBlockState());
        final BlockPos pos = te.getBlockPos();
        float time = AnimationTickHolder.getRenderTime(te.getLevel());
        if (!(te instanceof RedstoneResistorBlockEntity)) {
            return;
        }
        RedstoneResistorBlockEntity resistor = (RedstoneResistorBlockEntity) te;
        for (Direction direction : Iterate.directions) {
            Direction.Axis axis = direction.getAxis();
            if (boxAxis != axis)
                continue;

            float offset = getRotationOffsetForPosition(te, pos, axis);
            float angle = (time * te.getSpeed() * 3f / 10) % 360;
            float modifier = 1;

            modifier = resistor.getRotationSpeedModifier(direction);

            angle *= modifier;
            angle += offset;
            angle = angle / 180f * (float) Math.PI;

            SuperByteBuffer superByteBuffer =
                    CachedBufferer.partialFacing(AllBlockPartials.SHAFT_HALF, te.getBlockState(), direction);
            kineticRotationTransform(superByteBuffer, te, axis, angle, light);
            superByteBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }
        float state = 0;
        BlockState resistorState = te.getBlockState();
        if (resistor.clientState != null) {
            state = resistor.clientState.getValue(partialTicks);
        }

        VertexConsumer vb = buffer.getBuffer(RenderType.solid());
        int color = Color.mixColors(0x2C0300, 0xCD0000, state / 15f);
        SuperByteBuffer indicator = transform(CachedBufferer.partial(ClockWorkPartials.RESISTOR_INDICATOR, resistorState), resistorState);
        indicator.light(light)
                .color(color)
                .renderInto(ms, vb);
    }


    private SuperByteBuffer transform(SuperByteBuffer buffer, BlockState resistorState) {
        Direction.Axis axis = resistorState.getValue(RedstoneResistorBlock.AXIS);
        return buffer = switch (axis) {
            case X -> buffer.rotateCentered(Direction.NORTH, (float) Math.toRadians(90));
            case Y -> buffer;
            case Z -> buffer.rotateCentered(Direction.EAST, (float) Math.toRadians(90));
        };
    }
}
//    public RedstoneResistorRenderer(BlockEntityRendererProvider.Context context) {
//        super(context);
//    }
//
//    @Override
//    protected void renderSafe(KineticTileEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer,
//                              int light, int overlay) {
//        super.renderSafe(te, partialTicks, ms, buffer, light, overlay);
//
//
//        BlockState resistorState = te.getBlockState();
//        float state = te.clientState.getValue(partialTicks);
//        VertexConsumer vb = buffer.getBuffer(RenderType.solid());
//        int color = Color.mixColors(0x2C0300, 0xCD0000, state / 15f);
//        SuperByteBuffer indicator = transform(CachedBufferer.partial(ClockWorkPartials.RESISTOR_INDICATOR, resistorState), resistorState);
//        indicator.light(light)
//                .color(color)
//                .renderInto(ms, vb);
//    }

