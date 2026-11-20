package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrPlayerCrossing;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrView;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrEffects;

@Mixin(GameRenderer.class)
public class MixinGateGameRenderer {
    @WrapMethod(method = "render")
    private void clockwork$trackPlayerView(float pt, long deadline, boolean renderLevel, Operation<Void> render) {
        if (renderLevel) GatemakyrPlayerCrossing.beginRender(pt);
        try {
            render.call(pt, deadline, renderLevel);
        } finally {
            GatemakyrPlayerCrossing.endRender();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE_STRING",
        target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = "ldc=level"))
    private void clockwork$beginPlayerView(float pt, long deadline, boolean renderLevel, CallbackInfo ci) {
        GatemakyrPlayerCrossing.beforeRenderingEntities();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getXRot()F"))
    private void clockwork$orientGateExit(float pt, long deadline, PoseStack pose, CallbackInfo ci) {
        GatemakyrPlayerCrossing.applyCameraRoll(pose, pt);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;prepareCullFrustum(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;Lorg/joml/Matrix4f;)V"))
    private void clockwork$captureGateViews(float partialTick, long deadline, PoseStack pose, CallbackInfo ci) {
        GatemakyrView.capture(pose, partialTick, deadline);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V", shift = At.Shift.AFTER))
    private void clockwork$compositeGates(float partialTick, long deadline, PoseStack pose, CallbackInfo ci) {
        GatemakyrEffects.composite(pose, partialTick);
    }

    @Inject(method = "getMainCamera", at = @At("HEAD"), cancellable = true)
    private void clockwork$gateCamera(CallbackInfoReturnable<Camera> cir) {
        if (GatemakyrView.camera != null) cir.setReturnValue(GatemakyrView.camera);
    }
}
