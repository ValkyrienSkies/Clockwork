package org.valkyrienskies.clockwork.util.sound

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.rope.LinkStressWarning

class LinkStressSoundInstance(spring: Boolean, private val state: () -> State?) : AbstractTickableSoundInstance(
    (if (spring) ClockworkSounds.SPRING_OVERLOAD else ClockworkSounds.ROPE_OVERLOAD).mainEvent!!,
    SoundSource.BLOCKS, RandomSource.create()) {
    data class State(val position: Vec3, val stress: Float)

    init {
        looping = true
        delay = 0
        volume = 0f
        state()?.let { x = it.position.x; y = it.position.y; z = it.position.z }
    }

    override fun canStartSilent() = true

    fun stopNow() = stop()

    override fun tick() {
        val current = state()
        if (current == null) { stop(); return }
        val target = LinkStressWarning.volume(current.stress)
        volume += (target - volume) * 0.25f
        x = current.position.x; y = current.position.y; z = current.position.z
        if (target == 0f && volume < 0.005f) stop()
    }
}
