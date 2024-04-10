package org.valkyrienskies.clockwork.kelvin.api

import java.util.EnumMap

data class GasNodeChangesDataMutable(
    override val identifier: GasNodeIdentifier,
    override val deltaGasMasses: EnumMap<GasType, Double>,
    /**
     * Change in thermal energy, in joules
     */
    override var deltaThermalEnergy: Double,
    override val directionalDeltaMasses: MutableMap<GasNodeIdentifier, Double>,
): GasNodeChangesData

interface GasNodeChangesData {
    val identifier: GasNodeIdentifier
    val deltaGasMasses: EnumMap<GasType, Double>
    /**
     * Change in thermal energy, in joules
     */
    val deltaThermalEnergy: Double
    val directionalDeltaMasses: Map<GasNodeIdentifier, Double>
}

data class GasNodeChangeFromGame(
    val identifier: GasNodeIdentifier,
    val deltaGasMasses: EnumMap<GasType, Double>,
    /**
     * Change in thermal energy, in joules
     */
    val deltaThermalEnergy: Double,
)
