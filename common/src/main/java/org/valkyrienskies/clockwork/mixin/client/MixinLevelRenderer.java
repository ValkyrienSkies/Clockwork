package org.valkyrienskies.clockwork.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.valkyrienskies.clockwork.util.render.BlockRenderTypeRegistry;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Shadow protected abstract void renderChunkLayer(RenderType layer, PoseStack matrix, double x, double y, double z, Matrix4f frustumMatrix);

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V",
                    ordinal = 2,
                    shift = At.Shift.AFTER
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE_STRING",
                            target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                            args = "ldc=terrain"
                    ),
                    to = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;constantAmbientLight()Z"
                    )
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void renderCustom(PoseStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightmapTextureManager, Matrix4f projectionMatrix, CallbackInfo ci) {
        // Render all the custom ones
        for(RenderType layer : BlockRenderTypeRegistry.INSTANCE.getLayers()) {

            renderChunkLayer(layer, matrices, camera.getPosition().x, camera.getPosition().y, camera.getPosition().z, projectionMatrix);
        }
    }


}