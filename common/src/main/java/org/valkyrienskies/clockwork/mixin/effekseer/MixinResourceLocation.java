package org.valkyrienskies.clockwork.mixin.effekseer;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ResourceLocation.class, priority = Integer.MAX_VALUE)
public class MixinResourceLocation {
    @Inject(method = "validPathChar", at = @At("HEAD"), cancellable = true)
    private static void modernfixCompat(char c, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method = "isValidPath", at = @At("HEAD"), cancellable = true)
    private static void fixDfuCrash(String string, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!"DUMMY".equals(string));
    }
}
