package org.valkyrienskies.clockwork.mixin.content.lubricant;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.curiosities.lubricant.LubricantClient;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void clockwork$removeLubricant(CallbackInfoReturnable<Boolean> cir) {
        if (LubricantClient.attack()) cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void clockwork$preventLubricantMining(boolean attacking, CallbackInfo ci) {
        if (LubricantClient.holding()) ci.cancel();
    }
}
