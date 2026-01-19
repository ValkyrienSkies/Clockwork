package org.valkyrienskies.clockwork

import com.tterrag.registrate.util.entry.RegistryEntry
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE
import org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle.GasNozzleDisplaySource

object ClockworkDisplaySources {
    @JvmField
    val GAS_NOZZLE: RegistryEntry<GasNozzleDisplaySource> =
        REGISTRATE.displaySource("gas_nozzle", ::GasNozzleDisplaySource).register()

    @JvmStatic
    fun register() {

    }
}
