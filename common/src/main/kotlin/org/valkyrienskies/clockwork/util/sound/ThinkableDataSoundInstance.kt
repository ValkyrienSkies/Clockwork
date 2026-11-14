package org.valkyrienskies.clockwork.util.sound

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkSounds

class ThinkableDataSoundInstance(private val position: () -> Vec3?) : AbstractTickableSoundInstance(
    ClockworkSounds.THINKABLE_DATA.mainEvent!!, SoundSource.BLOCKS, RandomSource.create()) {
    init {
        volume = 0.45f
        tick()
    }

    override fun tick() {
        val point = position()
        if (point == null) { stop(); return }
        x = point.x
        y = point.y
        z = point.z
    }
}
