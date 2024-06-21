package org.valkyrienskies.clockwork.kelvin.api

import java.util.EnumMap

data class DuctNodeInfo(val currentTemperature: Double, val currentPressure: Double, val currentGasMasses: EnumMap<GasType, Double>)
