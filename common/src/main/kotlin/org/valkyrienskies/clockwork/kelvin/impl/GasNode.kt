package org.valkyrienskies.clockwork.kelvin.impl

import org.valkyrienskies.clockwork.kelvin.api.GasNodeChangeFromGame
import org.valkyrienskies.clockwork.kelvin.api.GasNodeChangesData
import org.valkyrienskies.clockwork.kelvin.api.GasNodeIdentifier
import org.valkyrienskies.clockwork.kelvin.api.GasType
import java.util.EnumMap
import kotlin.math.max

class GasNode(
    val identifier: GasNodeIdentifier,
    val gasMasses: EnumMap<GasType, Double>,
    val volume: Double,
    var temperature: Double,
    val connections: MutableMap<GasNode, GasConnection>,
) {
    fun applyChanges(changes: GasNodeChangeFromGame) {
        // Apply the changes to gas masses
        changes.deltaGasMasses.forEach { (gas, change) ->
            if (!change.isFinite()) {
                throw IllegalArgumentException("Change in gas mass must be finite")
            }
            if (gasMasses.containsKey(gas)) {
                gasMasses[gas] = max(gasMasses[gas]!! + change, 0.0)
            } else {
                gasMasses[gas] = max(change, 0.0)
            }
        }

        val totalGasMass = gasMasses.values.sum()
        if (!totalGasMass.isFinite()) {
            throw IllegalStateException("Total gas mass must be finite")
        }

        // If there is no gas, we don't need to do anything
        if (Epsilon.isEffectivelyZero(totalGasMass)) {
            return
        }

        // Calculate the weighted specific heat capacity
        val weightedSpecificHeat = gasMasses.map { (gas, mass) -> gas.specificHeatCapacity * mass }.sum() / totalGasMass

        // Apply the thermal energy change
        val deltaTemperature = changes.deltaThermalEnergy / (totalGasMass * weightedSpecificHeat)
        if (!deltaTemperature.isFinite()) {
            throw IllegalStateException("Delta temperature must be finite")
        }

        // Do not let temperature go below 0
        temperature = max(temperature + deltaTemperature, 0.0)
    }

    fun applyChanges2(changes: GasNodeChangesData) {
        val fromGame = GasNodeChangeFromGame(
            identifier = changes.identifier,
            deltaGasMasses = changes.deltaGasMasses,
            deltaThermalEnergy = changes.deltaThermalEnergy,
        )
        applyChanges(fromGame)
    }
}
