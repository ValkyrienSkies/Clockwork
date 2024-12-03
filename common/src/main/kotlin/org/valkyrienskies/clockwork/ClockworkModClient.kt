package org.valkyrienskies.clockwork

import com.simibubi.create.foundation.outliner.Outliner
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientTickEvent
import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.event.events.common.TickEvent
import net.minecraft.client.Minecraft
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotGlobals
import org.valkyrienskies.clockwork.kelvin.impl.client.ClientKelvinInfo
import org.valkyrienskies.clockwork.kelvin.impl.client.DuctNetworkClient


object ClockworkModClient {

    @JvmStatic
    val OUTLINER: Outliner = Outliner()

    @JvmStatic
    val WANDER_OUTLINER: Outliner = Outliner()

    val ClientKelvin: DuctNetworkClient = DuctNetworkClient()

    @JvmStatic
    fun initClient() {
        ClockworkPonders.init()

        // This is really stupid, but it's how create does it, so ¯\_(ツ)_/¯
        TickEvent.PLAYER_POST.register() {
            FrequencySlotGlobals.tick()
        }

        PlayerEvent.PLAYER_JOIN.register() {
            ClientKelvin.disabled = false
        }

        PlayerEvent.PLAYER_QUIT.register() {
            ClientKelvin.disabled = true
        }

        ClientTickEvent.CLIENT_POST.register() { mc: Minecraft,  ->
            if (mc.level != null) {
                ClientKelvin.tick(mc.level!!, 0)
            }
        }
    }

    @JvmStatic
    fun getKelvin(): DuctNetworkClient {
        if (ClientKelvin.disabled) {
            throw IllegalStateException("Client Kelvin is disabled!")
        }
        return ClientKelvin
    }
}