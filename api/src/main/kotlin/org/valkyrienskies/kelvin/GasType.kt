package org.valkyrienskies.kelvin

enum class GasType(
    val density: Double,              // Density of gas at STP (kg / m^3)
    val viscosity: Double,            // (kg / (m * s)) (see https://www.sciencedirect.com/topics/engineering/air-viscosity)
    val specificHeatCapacity: Double, // (J / (K * g)
) {
    AIR(1.293, 1.81e-5, 1.005),
    PHLOGISTON(3.0, 3.0e-5, 4.0),
}
