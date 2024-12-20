package org.valkyrienskies.clockwork.util.sound

import com.simibubi.create.foundation.utility.AnimationTickHolder
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import kotlin.math.max

class RepeatingSound(var soundEvent: SoundEvent, val soundSource: SoundSource, var sharedPitch: Float, var repeatDelay: Int, var scape: SoundScape, var relativeVolume: Float) {

    internal constructor(soundEvent: SoundEvent, scape: SoundScape, sharedPitch: Float, relativeVolume: Float, repeatDelay: Int) : this(soundEvent, SoundSource.AMBIENT, sharedPitch, max(1, repeatDelay), scape, relativeVolume)

    fun tick() {
        if (AnimationTickHolder.getTicks() % repeatDelay != 0) return

        val world = Minecraft.getInstance().level ?: return
        val meanPos = scape.getMeanPos() ?: return

        world.playLocalSound(
            meanPos.x, meanPos.y, meanPos.z, soundEvent, SoundSource.AMBIENT,
            scape.getVolume() * relativeVolume, sharedPitch, true
        )
    }
}
