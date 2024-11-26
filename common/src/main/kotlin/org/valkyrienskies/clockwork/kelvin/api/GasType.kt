package org.valkyrienskies.clockwork.kelvin.api

enum class GasType(
    val density: Double,              // Density of gas at STP (kg / m^3)
    val viscosity: Double,            // (kg / (m * s)) (see https://www.sciencedirect.com/topics/engineering/air-viscosity)
    val specificHeatCapacity: Double, // (J / (K * g)
    val thermalConductivity: Double,  // (W / (m * K))
    val sutherlandConstant: Double = 111.0, // (dimensionless) (see https://en.wikipedia.org/wiki/Viscosity#Temperature_dependence)
) {
    AIR(1.293, 1.716e-5, 1.005, 0.026),
    PHLOGISTON(3.0, 2.0e-5, 14.30, 0.240, 150.0),
    HELIUM(0.166, 1.96e-5, 5.1832, 0.151, 79.4),
}
