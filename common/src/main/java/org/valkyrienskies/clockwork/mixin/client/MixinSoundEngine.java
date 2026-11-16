package org.valkyrienskies.clockwork.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;

@Mixin(SoundEngine.class)
public class MixinSoundEngine {
    @Shadow private int tickCount;
    @Unique private final ArrayDeque<Integer> clockwork$pulseSounds = new ArrayDeque<>();

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void clockwork$limitPulseSounds(SoundInstance sound, CallbackInfo ci) {
        var id = sound.getLocation();
        if (!id.getNamespace().equals("vs_clockwork") ||
            !(id.getPath().equals("thinkable_data_1") || id.getPath().equals("combobulator_data_1"))) return;
        var minecraft = Minecraft.getInstance();
        if (sound.getVolume() <= 0 || minecraft.options.getSoundSourceVolume(SoundSource.MASTER) <= 0 ||
            minecraft.options.getSoundSourceVolume(sound.getSource()) <= 0) return;
        double range = Math.max(16, sound.getVolume() * 16);
        if (!sound.isRelative() && minecraft.gameRenderer.getMainCamera().getPosition()
            .distanceToSqr(sound.getX(), sound.getY(), sound.getZ()) > range * range) return;
        while (!clockwork$pulseSounds.isEmpty() && tickCount - clockwork$pulseSounds.peekFirst() >= 8)
            clockwork$pulseSounds.removeFirst();
        if (clockwork$pulseSounds.size() >= 2) {
            ci.cancel();
            return;
        }
        clockwork$pulseSounds.addLast(tickCount);
    }

    @Inject(method = "stopAll", at = @At("HEAD"))
    private void clockwork$clearPulseSounds(CallbackInfo ci) {
        clockwork$pulseSounds.clear();
    }
}
