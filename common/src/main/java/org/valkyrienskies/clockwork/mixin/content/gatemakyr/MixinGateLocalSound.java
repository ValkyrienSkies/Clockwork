package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrSounds;

@Mixin(ClientLevel.class)
public abstract class MixinGateLocalSound {
    @Inject(method = "playSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZJ)V", at = @At("HEAD"))
    private void clockwork$pointSound(double x, double y, double z, SoundEvent event, SoundSource category, float volume,
                                      float pitch, boolean delayed, long seed, CallbackInfo ci) {
        GatemakyrSounds.relayLocal((ClientLevel) (Object) this, new Vec3(x, y, z), event, category, volume, pitch, delayed, seed);
    }

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"))
    private void clockwork$entitySound(Player except, Entity entity, Holder<SoundEvent> event, SoundSource category, float volume,
                                       float pitch, long seed, CallbackInfo ci) {
        GatemakyrSounds.relayLocal((ClientLevel) (Object) this, entity.position(), event.value(), category, volume, pitch, false, seed);
    }
}
