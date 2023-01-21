package org.valkyrienskies.clockwork.content.contraptions.sequenced_seat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlockPartials;
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

public class SequencedSeatRenderer extends KineticTileEntityRenderer {

    private SequencedSeatBlockEntity te;

    public SequencedSeatRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(KineticTileEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay);
        if (!(te instanceof SequencedSeatBlockEntity))
            return;

        this.te = (SequencedSeatBlockEntity) te;
        SequencedSeatBlockEntity seat = this.te;
        final Direction facing = te.getBlockState()
                .getValue(BlockStateProperties.HORIZONTAL_FACING);

        SuperByteBuffer joystick = CachedBufferer.partialFacing(ClockWorkPartials.JOYSTICK, te.getBlockState(), facing.getOpposite());
        SuperByteBuffer buttonone = CachedBufferer.partialFacing(ClockWorkPartials.BUTTON_ONE, te.getBlockState(), facing.getOpposite());
        SuperByteBuffer buttontwo = CachedBufferer.partialFacing(ClockWorkPartials.BUTTON_TWO, te.getBlockState(), facing.getOpposite());

//        superBuffer.rotateCentered(Direction.EAST, AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)));
//        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        joystick.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
        buttonone.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
        buttontwo.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }
}