package org.valkyrienskies.clockwork.platform.forge

import net.minecraft.client.resources.sounds.Sound
import net.minecraft.client.sounds.SoundBufferLibrary
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.GasThrusterBlockEntity
import org.valkyrienskies.clockwork.util.sound.GasThrusterSound

object ThrusterSoundFactoryImpl {
    @JvmStatic
    fun create(be: GasThrusterBlockEntity): GasThrusterSound = object : GasThrusterSound(be) {
        override fun getStream(loader: SoundBufferLibrary, sound: Sound, looping: Boolean) = openStream()
    }
}
