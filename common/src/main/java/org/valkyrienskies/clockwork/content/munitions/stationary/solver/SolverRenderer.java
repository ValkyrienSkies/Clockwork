package org.valkyrienskies.clockwork.content.munitions.stationary.solver;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.outliner.LineOutline;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SolverRenderer extends KineticBlockEntityRenderer<SolverBlockEntity> {
    public SolverRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
    LineOutline beam = new LineOutline();

    @Override
    protected void renderSafe(SolverBlockEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay);

        if (te.getLevel() == null) {
            return;
        }
        BlockState blockState = te.getBlockState();
        SolverBlockEntity bte = (SolverBlockEntity) te;

        //beam
        Vec3 start = bte.getLinePoints().getFirst();
        Vec3 end = bte.getLinePoints().getSecond();

        double distance = start.distanceTo(end);

        if (distance == 0) {
            return;
        }

        Vec3 beamStart = new Vec3(0.5, 0.5, 0.5);
        Vec3 beamEnd = new Vec3(0.5, distance, 0.5);

        SuperRenderTypeBuffer superbuffer = SuperRenderTypeBuffer.getInstance();

        beam.getParams()
                .colored(0x9400D3)
                .lineWidth(16/16f)
                .lightmap(LightTexture.FULL_BRIGHT)
                .withFaceTexture(AllSpecialTextures.SELECTION);
        beam.set(beamStart, beamEnd)
                .render(ms, superbuffer, Vec3.ZERO, partialTicks);

        

        //beam end



    }
}
