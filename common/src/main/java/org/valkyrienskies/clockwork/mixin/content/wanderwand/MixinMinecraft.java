package org.valkyrienskies.clockwork.mixin.content.wanderwand;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandRopeRidingClient;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void clockwork$rideBindRope(CallbackInfo ci) {
        if (WandRopeRidingClient.onUse()) ci.cancel();
    }
}
