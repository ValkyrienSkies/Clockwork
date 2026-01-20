package org.valkyrienskies.clockwork

import com.tterrag.registrate.util.entry.RegistryEntry
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingDisplaySource
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingDisplaySource
import org.valkyrienskies.clockwork.content.contraptions.propeller.copter.CopterBearingDisplaySource
import org.valkyrienskies.clockwork.content.curiosities.altmeter.AltMeterDisplaySource
import org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle.GasNozzleDisplaySource

object ClockworkDisplaySources {
    @JvmField
    val GAS_NOZZLE: RegistryEntry<GasNozzleDisplaySource> =
        REGISTRATE.displaySource("gas_nozzle", ::GasNozzleDisplaySource).register()

    @JvmField
    val FLAP_BEARING: RegistryEntry<FlapBearingDisplaySource> =
        REGISTRATE.displaySource("flap_bearing", ::FlapBearingDisplaySource).register()

    @JvmField
    val COPTER_BEARING: RegistryEntry<CopterBearingDisplaySource> =
        REGISTRATE.displaySource("copter_bearing", ::CopterBearingDisplaySource).register()

    @JvmField
    val ALT_METER: RegistryEntry<AltMeterDisplaySource> =
        REGISTRATE.displaySource("alt_meter", ::AltMeterDisplaySource).register()

    @JvmField
    val PHYS_BEARING: RegistryEntry<PhysBearingDisplaySource> =
        REGISTRATE.displaySource("phys_bearing", ::PhysBearingDisplaySource).register()

    @JvmStatic
    fun register() {

    }
}
