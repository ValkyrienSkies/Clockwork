package org.valkyrienskies.clockwork.kelvin.api

enum class GasType(
    val density: Double,              // Density of gas at STP (kg / m^3)
    val viscosity: Double,            // (kg / (m * s)) (see https://www.sciencedirect.com/topics/engineering/air-viscosity)
    val specificHeatCapacity: Double, // (J / (K * g)
) {
    AIR(1.293, 1.81e-5, 1.005),
    PHLOGISTON(3.0, 0.75e-5, 14.30),
    HELIUM(0.166, 1.81e-5, 5.1832),
    EXAMPLE_01(0.1,1.81e-5,1.0),
    EXAMPLE_02(0.1,1.81e-5,1.0),
    EXAMPLE_03(0.1,1.81e-5,1.0)
}
