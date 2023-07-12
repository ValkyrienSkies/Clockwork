package org.valkyrienskies.clockwork.content.physicalities.reaction_wheel;

import com.jozufozu.flywheel.backend.Backend;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.ClockWorkPartials;

public class ReactionWheelRenderer extends KineticBlockEntityRenderer<ReactionWheelBlockEntity> {

    public ReactionWheelRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }


    @Override
    protected void renderSafe(ReactionWheelBlockEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay);

//        if (Backend.canUseInstancing(te.getLevel()))
//            return;

        BlockState blockState = te.getBlockState();
        ReactionWheelBlockEntity wte = (ReactionWheelBlockEntity) te;

        float speed = wte.rotspeed * 3 / 10f;
        float angle = wte.angle + speed * partialTicks;

        VertexConsumer vb = buffer.getBuffer(RenderType.solid());
        renderWheels(te, ms, light, blockState, angle, vb);
    }

    private void renderWheels(ReactionWheelBlockEntity te, PoseStack ms, int light, BlockState blockState, float angle,
                                VertexConsumer vb) {

        Direction direction = switch (blockState.getValue(BlockStateProperties.AXIS)) {
            case X -> Direction.NORTH;
            case Y -> Direction.UP;
            case Z -> Direction.EAST;
        };

        float offset = switch (blockState.getValue(BlockStateProperties.AXIS)) {
            case X, Z -> 90f;
            case Y -> 0;
        };
        SuperByteBuffer wheelBottom = CachedBufferer.partial(ClockWorkPartials.WHEEL_BOTTOM, blockState);
        SuperByteBuffer wheelTop = CachedBufferer.partial(ClockWorkPartials.WHEEL_TOP, blockState);

        kineticRotationTransform(wheelBottom, te, blockState.getValue(BlockStateProperties.AXIS), AngleHelper.rad(-angle), light);
        kineticRotationTransform(wheelTop, te, blockState.getValue(BlockStateProperties.AXIS), AngleHelper.rad(angle), light);
        wheelBottom.rotateCentered(direction, (float) Math.toRadians(offset));
        wheelTop.rotateCentered(direction, (float) Math.toRadians(offset));
        wheelTop.renderInto(ms, vb);
        wheelBottom.renderInto(ms, vb);
    }

    @Override
    protected BlockState getRenderedBlockState(ReactionWheelBlockEntity te) {
        return shaft(getRotationAxisOf(te));
    }

}
