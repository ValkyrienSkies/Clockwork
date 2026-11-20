package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrPlayerCrossing;

@Mixin(ClientLevel.class)
public abstract class MixinGateClientLevel {
    @Inject(method = "entitiesForRendering", at = @At("HEAD"))
    private void clockwork$crossBeforeRiderInterpolation(CallbackInfoReturnable<Iterable<Entity>> cir) {
        GatemakyrPlayerCrossing.beforeRenderingEntities();
    }
}
