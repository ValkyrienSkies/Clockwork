package org.valkyrienskies.clockwork.forge.content.curiosities.sounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class PropStreamSound extends AbstractTickableSoundInstance {

    private float pitch;

    public PropStreamSound(SoundEvent event, float pitch) {
        super(event, SoundSource.BLOCKS);
        this.pitch = pitch;
        looping = true;
        delay = 0;
        volume = 0.01f;
        relative = true;
    }

    @Override
    public void tick() {}

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void fadeIn(float maxVolume) {
        volume = Math.min(maxVolume, volume + 0.05f);
    }

    public void fadeOut() {
        volume = Math.max(0, volume - 0.05f);
    }

    public boolean isFaded() {
        return volume == 0;
    }

    @Override
    public float getPitch() {
        return pitch;
    }

    public void stopSound() {
        stop();
    }

}
