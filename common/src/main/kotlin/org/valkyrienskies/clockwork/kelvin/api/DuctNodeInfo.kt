package org.valkyrienskies.clockwork.kelvin.api

import java.util.EnumMap

data class DuctNodeInfo(var nodeType: NodeBehaviorType, var currentTemperature: Double, var currentPressure: Double, val currentGasMasses: EnumMap<GasType, Double>, var previousTemperatureLevel: Int = 0, var previousPressure: Double = 0.0)
