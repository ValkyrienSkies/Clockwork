package org.valkyrienskies.clockwork.content.contraptions.flap;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.base.KineticTileEntityRenderer;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.IBearingTileEntity;
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

public class FlapBearingRenderer extends KineticTileEntityRenderer {

    public FlapBearingRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(KineticTileEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {

        if (Backend.canUseInstancing(te.getLevel())) return;

        super.renderSafe(te, partialTicks, ms, buffer, light, overlay);

        IBearingTileEntity bearingTe = (IBearingTileEntity) te;
        final Direction facing = te.getBlockState()
                .getValue(BlockStateProperties.FACING);
        PartialModel top = ClockWorkPartials.BEARING_TOP_FLAP;
        SuperByteBuffer superBuffer = CachedBufferer.partial(top, te.getBlockState());

        float interpolatedAngle = bearingTe.getInterpolatedAngle(partialTicks - 1);
        kineticRotationTransform(superBuffer, te, facing.getAxis(), (float) (interpolatedAngle / 180 * Math.PI), light);

        if (facing.getAxis()
                .isHorizontal())
            superBuffer.rotateCentered(Direction.UP,
                    AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())));
        superBuffer.rotateCentered(Direction.EAST, AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)));
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(KineticTileEntity te, BlockState state) {
        return CachedBufferer.partialFacing(AllBlockPartials.SHAFT_HALF, state, state
                .getValue(FlapBearingBlock.FACING)
                .getOpposite());
    }

}