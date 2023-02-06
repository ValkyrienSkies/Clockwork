package org.valkyrienskies.clockwork.content.contraptions.reaction_wheel;

import com.jozufozu.flywheel.backend.Backend;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.base.KineticTileEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.valkyrienskies.clockwork.ClockWorkPartials;

public class ReactionWheelRenderer extends KineticTileEntityRenderer {

    public ReactionWheelRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }


    @Override
    protected void renderSafe(KineticTileEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer,
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

    private void renderWheels(KineticTileEntity te, PoseStack ms, int light, BlockState blockState, float angle,
                                VertexConsumer vb) {

        Direction direction = switch (blockState.getValue(BlockStateProperties.AXIS)) {
            case X -> Direction.EAST;
            case Y -> Direction.UP;
            case Z -> Direction.NORTH;
        };
        SuperByteBuffer wheelBottom = CachedBufferer.partial(ClockWorkPartials.WHEEL_BOTTOM, blockState);
        SuperByteBuffer wheelTop = CachedBufferer.partial(ClockWorkPartials.WHEEL_TOP, blockState);
        kineticRotationTransform(wheelBottom, te, getRotationAxisOf(te), AngleHelper.rad(angle), light);
        kineticRotationTransform(wheelTop, te, getRotationAxisOf(te), AngleHelper.rad(angle), light);
        wheelTop.renderInto(ms, vb);
        wheelBottom.renderInto(ms, vb);
    }

    @Override
    protected BlockState getRenderedBlockState(KineticTileEntity te) {
        return shaft(getRotationAxisOf(te));
    }

}
