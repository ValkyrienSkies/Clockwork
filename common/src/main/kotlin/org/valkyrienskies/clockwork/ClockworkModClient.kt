package org.valkyrienskies.clockwork

import com.simibubi.create.foundation.outliner.Outliner
import dev.architectury.event.events.common.TickEvent
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotGlobals
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.impl.client.DuctNetworkClient


object ClockworkModClient {

    @JvmStatic
    val OUTLINER: Outliner = Outliner()

    @JvmStatic
    val WANDER_OUTLINER: Outliner = Outliner()

    @JvmStatic
    fun initClient() {
        ClockworkPonders.init()

        // This is really stupid, but it's how create does it, so ¯\_(ツ)_/¯
        TickEvent.PLAYER_POST.register() {
            FrequencySlotGlobals.tick()
        }
    }

    @JvmStatic
    fun getKelvin(): DuctNetworkClient {
        return KelvinMod.getClientKelvin() as DuctNetworkClient
    }
}