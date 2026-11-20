package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrPlayerCrossing;

@Mixin(LocalPlayer.class)
public abstract class MixinGateLocalPlayer {
    @Inject(method = "tick", at = @At("HEAD"))
    private void clockwork$finishPreviousMovement(CallbackInfo ci) {
        GatemakyrPlayerCrossing.finishTick();
    }
}
