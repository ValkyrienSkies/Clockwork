package org.valkyrienskies.clockwork.platform.fabric

import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.client.sounds.SoundBufferLibrary
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.GasThrusterBlockEntity
import org.valkyrienskies.clockwork.util.sound.GasThrusterSound

object ThrusterSoundFactoryImpl {
    @JvmStatic
    fun create(be: GasThrusterBlockEntity): GasThrusterSound = object : GasThrusterSound(be), FabricSoundInstance {
        override fun getAudioStream(loader: SoundBufferLibrary, id: ResourceLocation, repeatInstantly: Boolean) = openStream()
    }
}
