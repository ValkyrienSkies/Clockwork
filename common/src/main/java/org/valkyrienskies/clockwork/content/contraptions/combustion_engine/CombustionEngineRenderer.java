package org.valkyrienskies.clockwork.content.contraptions.combustion_engine;

import com.jozufozu.flywheel.backend.Backend;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.base.KineticTileEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.valkyrienskies.clockwork.ClockWorkPartials;

public class CombustionEngineRenderer extends KineticTileEntityRenderer {
    public CombustionEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(KineticTileEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay);

//        if (Backend.canUseInstancing(te.getLevel()))
//            return;

        BlockState blockState = te.getBlockState();
        float speed = 0;
        if (te.getSpeed() != 0) {
            speed = te.getSpeed();
        }
        SuperByteBuffer engine = CachedBufferer.partialFacing(ClockWorkPartials.ENGINE, blockState);
        SuperByteBuffer pistons = CachedBufferer.partialFacing(ClockWorkPartials.ENGINE_PISTONS, blockState);

        pistons.light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        shakeEngine(engine, speed, partialTicks, te).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    private SuperByteBuffer shakeEngine(SuperByteBuffer buffer, float speed, float partialTicks, KineticTileEntity te) {
        float offsetX = (float) ((((Math.sin(partialTicks) + Math.sin(partialTicks * 2) + Math.sin(partialTicks * -0.5)) * 2)/ 3) / 100) * (speed/32);
        float offsetY = (float) ((((Math.sin(partialTicks) + Math.sin(partialTicks * 0.5) + Math.sin(partialTicks * 3)) * 2)/ 3) / 100) * (speed/32);
        float offsetZ = (float) ((((Math.sin(partialTicks) + Math.sin(partialTicks * 4) + Math.sin(partialTicks * -1)) * 2)/ 3) / 100) * (speed/32);

        Direction.Axis axis = te.getBlockState().getValue(BlockStateProperties.FACING).getAxis();
        if (axis == Direction.Axis.X) {
            offsetX = 0;
        } else if (axis == Direction.Axis.Y) {
            offsetY = 0;
        } else if (axis == Direction.Axis.Z) {
            offsetZ = 0;
        }
        buffer.translate(offsetX, offsetY, offsetZ);
        return buffer;
    }

    @Override
    protected BlockState getRenderedBlockState(KineticTileEntity te) {
        return shaft(getRotationAxisOf(te));
    }
}
