package org.valkyrienskies.clockwork

import com.tterrag.registrate.util.entry.RegistryEntry
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingDisplaySource
import org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle.GasNozzleDisplaySource

object ClockworkDisplaySources {
    @JvmField
    val GAS_NOZZLE: RegistryEntry<GasNozzleDisplaySource> =
        REGISTRATE.displaySource("gas_nozzle", ::GasNozzleDisplaySource).register()

    @JvmField
    val FLAP_BEARING: RegistryEntry<FlapBearingDisplaySource> =
        REGISTRATE.displaySource("flap_bearing", ::FlapBearingDisplaySource).register()

    @JvmStatic
    fun register() {

    }
}
