package org.valkyrienskies.clockwork.content.contraptions.intake;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.base.KineticTileEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class IntakeRenderer extends KineticTileEntityRenderer {
    public IntakeRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(KineticTileEntity te, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        super.renderSafe(te, partialTicks, ms, bufferSource, light, overlay);
        if (!(te instanceof IntakeBlockEntity)) {
            return;
        }
    }

    protected BlockState getRenderedBlockState(KineticTileEntity te) {
        return KineticTileEntityRenderer.shaft(KineticTileEntityRenderer.getRotationAxisOf(te));
    }
}
