package org.valkyrienskies.clockwork.mixin.effekseer.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.effekseer.client.internal.RenderStateCapture;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "resizeDisplay", at = @At("RETURN"))
    private void resizeCapturedDepthBuffer(CallbackInfo ci) {
        RenderStateCapture.getCAPTURED_WORLD_DEPTH_BUFFER().resize(window.getWidth(), window.getHeight(), ON_OSX);
        RenderStateCapture.getCAPTURED_HAND_DEPTH_BUFFER().resize(window.getWidth(), window.getHeight(), ON_OSX);
    }

    @Shadow @Final private Window window;

    @Shadow @Final public static boolean ON_OSX;
}
