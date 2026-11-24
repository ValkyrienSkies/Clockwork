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

    fun isValidState(masses: Map<GasType, Double>, energy: Double): Boolean {
        if (!energy.isFinite() || energy <= 0.0 || masses.values.any { !it.isFinite() || it < 0.0 }) return false
        val mass = masses.values.sum()
        val capacity = capacity(masses)
        if (!mass.isFinite() || mass < 1e-9 || !capacity.isFinite() || capacity <= 1e-9) return false
        val temperature = energy / capacity
        return temperature.isFinite() && temperature > 0.0
    }

    /** Sealed blimps are allowed to be completely empty (0 kg gas, 0 J energy). */
    fun isValidSealedState(masses: Map<GasType, Double>, energy: Double): Boolean {
        if (!energy.isFinite() || energy < 0.0 || masses.values.any { !it.isFinite() || it < 0.0 }) return false
        val mass = masses.values.sum()
        if (!mass.isFinite()) return false
        if (mass < 1e-9) return true
        return isValidState(masses, energy)
    }

    fun pressure(masses: Map<GasType, Double>, energy: Double, volume: Double): Double {
        if (!volume.isFinite() || volume <= 0.0 || !isValidState(masses, energy)) return 0.0
        val capacity = capacity(masses)
        val temperature = energy / capacity
        val moles = masses.entries.sumOf { (gas, amount) -> gas.massToMoles(amount) }
        if (!moles.isFinite() || moles <= 0.0) return 0.0
        val p = moles * DuctNetwork.idealGasConstant * temperature / volume
        return if (p.isFinite() && p > 0.0) p else 0.0
    }

    /** Effective displaced volume of a flexible/ballonet sealed gasbag, capped at hull volume. */
    fun effectiveVolume(volume: Double, internalPressure: Double, ambientPressure: Double): Double {
        if (!volume.isFinite() || volume <= 0.0 || !internalPressure.isFinite() || internalPressure <= 0.0) return 0.0
        if (!ambientPressure.isFinite() || ambientPressure <= 1e-9) return volume
        return volume * (internalPressure / ambientPressure).coerceIn(0.0, 1.0)
    }

    /**
     * Sealed blimp thermodynamic step: outward-only gas permeation (tiny when intact, rapid when punctured)
     * plus thermal equalization with ambient air. Never draws ambient air inward.
     */
    fun stepSealed(
        masses: MutableMap<GasType, Double>, energy: Double, volume: Double,
        ambientPressure: Double, ambientTemperature: Double,
        blimpPermeability: Double, leakPermeability: Double,
        heatTransfer: Double, leakCooling: Double, holes: Int
    ): Double {
        val mass = masses.values.sum()
        val capacity = capacity(masses)
        if (!mass.isFinite() || !capacity.isFinite() || mass < 1e-9 || capacity <= 1e-9 || !volume.isFinite() || volume <= 0.0) {
            masses.clear()
            return 0.0
        }
        val temperature = energy / capacity
        val moles = masses.entries.sumOf { (gas, amount) -> gas.massToMoles(amount) }
        if (!moles.isFinite() || moles <= 1e-12 || !temperature.isFinite() || temperature <= 0.0) {
            masses.clear()
            return 0.0
        }
        val r = DuctNetwork.idealGasConstant
        val pressure = moles * r * temperature / volume
        val molarMass = mass / moles
        val area = 4.84 * volume.pow(2.0 / 3.0)
        val effectivePermeability = max(0.0, blimpPermeability) + (if (holes > 0) max(0.0, leakPermeability) * holes else 0.0)
        val thermalSpeed = sqrt(temperature * r / molarMass)
        val exitRate = if (thermalSpeed > 1e-9 && pressure.isFinite() && pressure > 0.0) {
            pressure * area * effectivePermeability / thermalSpeed
        } else 0.0
        val exitFraction = (exitRate * 0.01 / mass).coerceIn(0.0, 1.0)
        val remainingFraction = 1.0 - exitFraction
        if (remainingFraction <= 1e-9) {
            masses.clear()
            return 0.0
        }
        masses.replaceAll { _, amount -> amount * remainingFraction }
        var result = energy * remainingFraction
        val remainingCapacity = capacity * remainingFraction
        if (remainingCapacity > 0.0 && ambientTemperature.isFinite() && ambientTemperature > 0.0) {
            val conductance = heatTransfer * area * (holes * 2.0 + 1.0) * (1.0 + exitFraction * leakCooling)
            val fraction = -expm1(-conductance / remainingCapacity)
            result += (ambientTemperature * remainingCapacity - result) * fraction
        }
        return if (result.isFinite() && result > 0.0) result else 0.0
    }

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
