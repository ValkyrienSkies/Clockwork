package org.valkyrienskies.clockwork.content.logistics.gas.thermal

import kotlin.math.expm1
import kotlin.math.max
import kotlin.math.min

/** Exact Newton cooling over a time step. Capacities are J/K, conductance is W/K. */
object ThermalTransfer {
    // Kelvin clamps node energy to this floor when applying a heat delta.
    private const val MIN_ENERGY = 0.001

    fun cooling(energy: Double, capacity: Double, ambient: Double, coefficient: Double, seconds: Double): Double {
        if (!validNode(energy, capacity) || !ambient.isFinite() || ambient < 0.0) return 0.0
        val excess = energy - max(capacity * ambient, MIN_ENERGY)
        if (excess <= 0.0) return 0.0
        return excess * fraction(coefficient, seconds, capacity)
    }

    /** Signed joules from A to B, limited to their heat-capacity-weighted equilibrium. */
    fun exchange(
        energyA: Double, capacityA: Double,
        energyB: Double, capacityB: Double,
        coefficient: Double, seconds: Double
    ): Double {
        if (!validNode(energyA, capacityA) || !validNode(energyB, capacityB)) return 0.0
        val smaller = min(capacityA, capacityB)
        val effectiveCapacity = smaller / (1.0 + smaller / max(capacityA, capacityB))
        val equilibriumEnergy = (energyA / capacityA - energyB / capacityB) * effectiveCapacity
        val transfer = equilibriumEnergy * fraction(coefficient, seconds, effectiveCapacity)
        if (!transfer.isFinite()) return 0.0
        return transfer.coerceIn(-max(energyB - MIN_ENERGY, 0.0), max(energyA - MIN_ENERGY, 0.0))
    }

    private fun validNode(energy: Double, capacity: Double) =
        energy.isFinite() && energy >= 0.0 && capacity.isFinite() && capacity > 0.0

    private fun fraction(coefficient: Double, seconds: Double, capacity: Double): Double {
        if (!coefficient.isFinite() || coefficient <= 0.0 || !seconds.isFinite() || seconds <= 0.0) return 0.0
        return -expm1(-coefficient * seconds / capacity)
    }
}
