package org.valkyrienskies.clockwork.mixin.content.pulse;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.client.render.scanner.ScannerRenderer;
import org.valkyrienskies.clockwork.util.render.AreaDesignatorClusterRenderer;
import org.valkyrienskies.clockwork.util.render.BluperClusterRenderer;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Shadow
    @Nullable
    private ClientLevel level;

    @Shadow
    @Final
    private Minecraft minecraft;

    /**
     * @deprecated Will be replaced with different shader soon, only here for temporary reference.
     */
    @Deprecated
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V", shift = At.Shift.BEFORE))
    private void renderScanner(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        ScannerRenderer.Companion.getINSTANCE().doRender(poseStack);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V", shift = At.Shift.BEFORE))
    private void renderAreaDesignator(final PoseStack poseStack, final float tickDelta, final long nanos, final boolean shouldRenderBlockOutline, final Camera camera, final GameRenderer gameRenderer, final LightTexture lightTexture, final Matrix4f projectionMatrix, final CallbackInfo ci) {
        ClockworkMod.INSTANCE.getOUTLINER().renderOutlines(poseStack, SuperRenderTypeBuffer.getInstance(), camera.getPosition(), tickDelta);
        AreaDesignatorClusterRenderer.Companion.getINSTANCE().renderDesignator(level, minecraft, poseStack);
        BluperClusterRenderer.Companion.getINSTANCE().renderDesignator(level, minecraft, poseStack);
    }
}