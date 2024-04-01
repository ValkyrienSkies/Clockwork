package org.valkyrienskies.clockwork.kelvin.impl

import org.valkyrienskies.clockwork.kelvin.api.GasNodeChangesData
import org.valkyrienskies.clockwork.kelvin.api.GasNodeIdentifier
import org.valkyrienskies.clockwork.kelvin.api.GasNodeResultData
import org.valkyrienskies.clockwork.kelvin.api.GasType
import java.util.EnumMap
import kotlin.math.max

data class GasNode(
    val identifier: GasNodeIdentifier,
    val gasMasses: EnumMap<GasType, Double>,
    val volume: Double,
    var temperature: Double,
    val connections: MutableMap<GasNode, GasConnection>,
) {
    fun applyChanges(changes: GasNodeChangesData): GasNodeResultData {
        changes.deltaGasMasses.forEach { (gas, change) ->
            if (gasMasses.containsKey(gas)) {
                gasMasses[gas] = max(gasMasses[gas]!! + change, 0.0)
            } else {
                gasMasses[gas] = max(change, 0.0)
            }
        }

        val averageSpecificHeat = gasMasses.keys.sumOf { it.specificHeatCapacity } / gasMasses.values.size

        val temperatureChange = (changes.deltaThermalEnergy / (gasMasses.values.sum() * averageSpecificHeat))

        temperature += temperatureChange

        return GasNodeResultData(
            gasMasses,
            temperature,
            changes.directionalDeltaMasses,
        )
    }
}
