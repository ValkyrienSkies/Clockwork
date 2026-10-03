package org.valkyrienskies.clockwork.content.forces

import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.GasType
import kotlin.math.expm1
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/** All balloon heat capacities are in J/K, masses in kg, temperatures in K. */
internal object BalloonThermodynamics {
    fun specificCapacity(gas: GasType) = gas.specificHeatCapacity * 1000.0 / gas.adiabaticIndex
    fun capacity(masses: Map<GasType, Double>) = masses.entries.sumOf { (gas, mass) -> mass * specificCapacity(gas) }

    fun step(
        masses: MutableMap<GasType, Double>, energy: Double, volume: Double,
        air: GasType, ambientPressure: Double, ambientTemperature: Double,
        permeability: Double, heatTransfer: Double, leakCooling: Double, holes: Int
    ): Double {
        val mass = masses.values.sum()
        val capacity = capacity(masses)
        if (mass <= 0.0 || capacity <= 0.0 || volume <= 0.0) return 0.0
        val temperature = energy / capacity
        val moles = masses.entries.sumOf { (gas, amount) -> gas.massToMoles(amount) }
        val r = DuctNetwork.idealGasConstant
        val pressure = moles * r * temperature / volume
        val molarMass = mass / moles
        val area = 4.84 * volume.pow(2.0 / 3.0)
        // Limit flow to pressure equilibrium.
        val exitRate = max(0.0, pressure - ambientPressure) * area * permeability /
            sqrt(temperature * r / molarMass) * (holes + 1.0)
        val exitFraction = if (pressure > 0.0) {
            minOf((exitRate * 0.01 / mass).coerceIn(0.0, 1.0), (1.0 - ambientPressure / pressure).coerceIn(0.0, 1.0))
        } else 0.0
        val remainingFraction = 1.0 - exitFraction
        masses.replaceAll { _, amount -> amount * remainingFraction }
        var result = energy * remainingFraction
        val remainingCapacity = capacity * remainingFraction
        // Exponential cooling avoids overshooting ambient.
        if (remainingCapacity > 0.0) {
            val conductance = heatTransfer * area * (holes * 2.0 + 1.0) * (1.0 + exitFraction * leakCooling)
            val fraction = -expm1(-conductance / remainingCapacity)
            result += (ambientTemperature * remainingCapacity - result) * fraction
        }
        val remainingMoles = moles * remainingFraction
        val newTemperature = if (remainingCapacity > 0.0) result / remainingCapacity else ambientTemperature
        val newPressure = remainingMoles * r * newTemperature / volume
        if (ambientPressure > newPressure && ambientTemperature > 0.0) {
            val airMolarMass = air.molesToMass(1.0)
            val airCapacity = specificCapacity(air)
            val rate = (ambientPressure - newPressure) * area * permeability /
                sqrt(ambientTemperature * r / airMolarMass) * (holes * 2.0 + 1.0)
            // Solve P = (n + x/M) R (E + x cv Ta) / ((C + x cv) V).
            val a = r / volume * airCapacity * ambientTemperature / airMolarMass
            val b = r / volume * (remainingMoles * airCapacity * ambientTemperature + result / airMolarMass) - ambientPressure * airCapacity
            val c = r / volume * remainingMoles * result - ambientPressure * remainingCapacity
            val discriminant = sqrt(max(0.0, b * b - 4.0 * a * c))
            val equilibriumMass = if (b >= 0.0 && b + discriminant > 0.0) -2.0 * c / (b + discriminant)
                else (-b + discriminant) / (2.0 * a)
            val incoming = minOf(rate * 0.01, max(0.0, equilibriumMass))
            masses[air] = (masses[air] ?: 0.0) + incoming
            result += incoming * ambientTemperature * airCapacity
        }
        return result
    }
}
