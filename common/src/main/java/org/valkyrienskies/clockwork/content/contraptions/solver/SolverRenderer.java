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
        VertexConsumer consumer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.LINES);

//        SuperRenderTypeBuffer superbuffer = SuperRenderTypeBuffer.getInstance();

        Vec3 diff = end.subtract(start);
        float hAngle = AngleHelper.deg(Mth.atan2(diff.x, diff.z));
        float hDistance = (float) diff.multiply(1, 0, 1)
                .length();
        float vAngle = AngleHelper.deg(Mth.atan2(hDistance, diff.y)) - 90;
        ms.pushPose();
        //temp


        Vec3 camVec = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
        TransformStack.cast(ms)
                .translate(start)
                .rotateY(hAngle).rotateX(vAngle);
        RenderSystem.depthMask(false);

//        beam.getParams()
//                .colored(0x9400D3)
//                .lineWidth(16/16f)
//                .lightMap(LightTexture.FULL_BRIGHT)
//                .withFaceTexture(AllSpecialTextures.SELECTION);
//        beam.set(start, end)
//                .render(ms, superbuffer, partialTicks);
//        consumer.vertex(start.x(), start.y(), start.z()).color(255, 0, 255, 192).endVertex();
//        consumer.vertex(end.x(), end.y(), end.z()).color(255, 0, 255, 192).endVertex();
        LevelRenderer.renderLineBox(ms, consumer, 8/16d, 0d, 8/16d, 8/16d, diff.length(), 8/16d, 1, 1, 1, 1);

        ms.popPose();



    }
}
