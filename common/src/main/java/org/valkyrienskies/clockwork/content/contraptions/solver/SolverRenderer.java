package org.valkyrienskies.clockwork.content.contraptions.solver;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.base.KineticTileEntityRenderer;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.outliner.LineOutline;
import com.simibubi.create.foundation.utility.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.List;

public class SolverRenderer extends KineticTileEntityRenderer {
    public SolverRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
    LineOutline beam = new LineOutline();

    @Override
    protected void renderSafe(KineticTileEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay);
        if (!(te instanceof SolverBlockEntity)) {
            return;
        }
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
                .lightMap(LightTexture.FULL_BRIGHT)
                .withFaceTexture(AllSpecialTextures.SELECTION);
        beam.set(beamStart, beamEnd)
                .render(ms, superbuffer, partialTicks);

        

        //beam end



    }
}
