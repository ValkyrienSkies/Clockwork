package org.valkyrienskies.clockwork

import com.jozufozu.flywheel.backend.Loader.ResourceReloadListener
import com.simibubi.create.foundation.outliner.Outliner
import dev.architectury.event.events.client.ClientTickEvent
import dev.architectury.event.events.common.TickEvent
import dev.architectury.registry.ReloadListenerRegistry
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.SecondScrollValueRenderer
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotGlobals
import org.valkyrienskies.clockwork.util.render.DualLinkRenderer
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.impl.client.DuctNetworkClient


object ClockworkModClient {

    @JvmStatic
    val OUTLINER: Outliner = Outliner()

    @JvmStatic
    val WANDER_OUTLINER: Outliner = Outliner()

    @JvmStatic
    val RESOURCE_RELOAD_LISTENER: ResourceManagerReloadListener = ClockworkReloadListener()

    @JvmStatic
    fun initClient() {
        ClockworkPonders.init()
        ClockworkSoundScapes.init()
        SecondScrollValueRenderer.init()

        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, RESOURCE_RELOAD_LISTENER)

        // This is really stupid, but it's how create does it, so ¯\_(ツ)_/¯
        TickEvent.PLAYER_POST.register() {
            FrequencySlotGlobals.tick()
        }

        ClientTickEvent.CLIENT_POST.register(ClientTickEvent.Client {
            DualLinkRenderer.tick()
            ClockworkSoundScapes.tick()
            SecondScrollValueRenderer.tickSecond()
        })
    }

    @JvmStatic
    fun getKelvin(): DuctNetworkClient {
        return KelvinMod.getClientKelvin() as DuctNetworkClient
    }

    class ClockworkReloadListener : ResourceManagerReloadListener {
        override fun onResourceManagerReload(resourceManager: ResourceManager) {
            ClockworkSoundScapes.invalidateAll()
        }
    }
}