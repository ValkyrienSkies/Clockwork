package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import net.minecraft.client.Minecraft;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrClient;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "useShaderTransparency", at = @At("HEAD"), cancellable = true)
    private static void clockwork$gateTransparency(CallbackInfoReturnable<Boolean> cir) {
        if (GatemakyrView.target != null) cir.setReturnValue(false);
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void clockwork$gatemakyrAttack(CallbackInfoReturnable<Boolean> cir) {
        if (GatemakyrClient.holding()) cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void clockwork$gatemakyrMining(boolean attacking, CallbackInfo ci) {
        if (GatemakyrClient.holding()) ci.cancel();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void clockwork$gatemakyrUse(CallbackInfo ci) {
        if (GatemakyrClient.holding()) ci.cancel();
    }
}
