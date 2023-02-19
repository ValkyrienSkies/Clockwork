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
import net.minecraft.world.level.block.state.BlockState;
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

        pistons.light(light).renderInto(ms, buffer.getBuffer(RenderType.cutout()));
        shakeEngine(engine, speed).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutout()));
    }

    private SuperByteBuffer shakeEngine(SuperByteBuffer buffer, float speed) {
        float offsetX = (float) (((Math.sin(speed) + Math.sin(speed * 2) + Math.sin(speed * -0.5)) * 2)/ 3);
        float offsetY = (float) (((Math.sin(speed) + Math.sin(speed * 0.5) + Math.sin(speed * 3)) * 2)/ 3);
        float offsetZ = (float) (((Math.sin(speed) + Math.sin(speed * 4) + Math.sin(speed * -1)) * 2)/ 3);

        buffer.translate(offsetX, offsetY, offsetZ);
        return buffer;
    }

    @Override
    protected BlockState getRenderedBlockState(KineticTileEntity te) {
        return shaft(getRotationAxisOf(te));
    }
}
