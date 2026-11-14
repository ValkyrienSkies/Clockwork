package org.valkyrienskies.clockwork.content.schematics

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import org.valkyrienskies.clockwork.ClockworkSounds

internal class VdexSketchSound(drawing: Boolean, private val gain: () -> Float?) : AbstractTickableSoundInstance(
    (if (drawing) ClockworkSounds.VDEX_SCRIBBLE else ClockworkSounds.VDEX_ERASE).mainEvent!!,
    SoundSource.MASTER, RandomSource.create()) {
    init {
        relative = true
        attenuation = SoundInstance.Attenuation.NONE
        looping = drawing
        delay = 0
        volume = 0f
    }

    override fun canStartSilent() = true
    override fun tick() {
        val strength = gain()
        if (strength == null) stop() else volume = .55f * strength.coerceIn(0f, 1f)
    }
}
