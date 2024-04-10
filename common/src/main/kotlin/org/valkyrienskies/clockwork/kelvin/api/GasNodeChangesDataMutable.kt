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
): GasNodeChangesData {
    init {
        require(deltaGasMasses.values.all{ it.isFinite() }) { "Delta gas masses must be finite" }
        require(deltaThermalEnergy.isFinite()) { "Delta thermal energy must be finite" }
        require(directionalDeltaMasses.values.all{ it.isFinite() }) { "Directional delta gas masses must be finite" }
    }
}

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
