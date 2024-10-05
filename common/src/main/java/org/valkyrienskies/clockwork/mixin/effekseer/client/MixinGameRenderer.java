package org.valkyrienskies.clockwork.mixin.effekseer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.effekseer.client.internal.EffekFpvRenderer;
import org.valkyrienskies.clockwork.effekseer.client.internal.RenderContext;
import org.valkyrienskies.clockwork.effekseer.client.internal.RenderStateCapture;
import org.valkyrienskies.clockwork.effekseer.client.render.EffekRenderer;
import org.valkyrienskies.clockwork.effekseer.client.render.RenderUtil;


import static org.lwjgl.opengl.GL11.*;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Shadow @Final public ItemInHandRenderer itemInHandRenderer;
    @Shadow @Final Minecraft minecraft;
    @Shadow private boolean renderHand;

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void renderLevelTail(float partial, long l, PoseStack poseStack, CallbackInfo ci) {
        glDepthMask(true);
        glDepthFunc(GL_LEQUAL);

        if (RenderContext.renderLevelDeferred() && RenderStateCapture.getLEVEL().getHasCapture()) {
            RenderStateCapture.getLEVEL().setHasCapture(false);

            RenderUtil.INSTANCE.pasteToCurrentDepthFrom(RenderStateCapture.getCAPTURED_WORLD_DEPTH_BUFFER());
            EffekRenderer.onRenderWorldLast(partial, RenderStateCapture.getLEVEL().getPose(), RenderStateCapture.getLEVEL().getProjection(), RenderStateCapture.getLEVEL().getCamera());
        }
        if (RenderContext.renderHandDeferred() && renderHand) {
            if (RenderContext.captureHandDepth()) {
                RenderUtil.INSTANCE.pasteToCurrentDepthFrom(RenderStateCapture.getCAPTURED_HAND_DEPTH_BUFFER());
            }
            ((EffekFpvRenderer) itemInHandRenderer).vsclockwork$renderFpvEffek(partial, minecraft.player);
        }
    }
}
