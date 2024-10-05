package org.valkyrienskies.clockwork.mixin.effekseer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.effekseer.client.internal.RenderContext;
import org.valkyrienskies.clockwork.effekseer.client.internal.RenderStateCapture;
import org.valkyrienskies.clockwork.effekseer.client.render.EffekRenderer;
import org.valkyrienskies.clockwork.effekseer.client.render.RenderUtil;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void onRenderLevelLast(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, com.mojang.math.Matrix4f projectionMatrix, CallbackInfo ci) {
        var capture = RenderStateCapture.getLEVEL();
        var currentPose = poseStack.last();
        var capturedPose = capture.getPose().last();
        capturedPose.pose().load(currentPose.pose());
        capturedPose.normal().load(currentPose.normal());
        capture.getProjection().load(projectionMatrix);
        capture.setCamera(camera);
        capture.setHasCapture(true);

        if (RenderContext.renderLevelDeferred()) {
            RenderUtil.INSTANCE.copyCurrentDepthTo(RenderStateCapture.getCAPTURED_WORLD_DEPTH_BUFFER());
        } else {
            EffekRenderer.onRenderWorldLast(partialTick, capture.getPose(), capture.getProjection(), capture.getCamera());
        }
    }
}
