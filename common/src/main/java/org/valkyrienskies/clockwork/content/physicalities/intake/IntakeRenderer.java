package org.valkyrienskies.clockwork.content.physicalities.intake;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class IntakeRenderer extends KineticBlockEntityRenderer<IntakeBlockEntity> {
    public IntakeRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(IntakeBlockEntity te, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        super.renderSafe(te, partialTicks, ms, bufferSource, light, overlay);
        if (te == null) {
            return;
        }
    }

    protected BlockState getRenderedBlockState(IntakeBlockEntity te) {
        return KineticBlockEntityRenderer.shaft(KineticBlockEntityRenderer.getRotationAxisOf(te));
    }
}
