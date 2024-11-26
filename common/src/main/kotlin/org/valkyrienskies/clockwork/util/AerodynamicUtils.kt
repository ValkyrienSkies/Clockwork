package org.valkyrienskies.clockwork.util

import net.minecraft.util.Mth
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.forces.DragController
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.clockwork.kelvin.impl.DuctNetworkImpl.Companion.idealGasConstant
import java.util.*
import kotlin.math.*

/**
 * Contains useful functions for features that need funny wind maths. Mainly drag and balloons.
 */
object AerodynamicUtils {

    /**
     * Returns the density of air at a Y value adapted to a real life altitude, where Y = 60 is sea level, and Y = 320 is the top of the Troposphere.
     *
     * Where:
     *
     * p = mass density (kg/m^3)
     *
     * pb = base density via b (kg/m^3)
     *
     * Tb = standard temperature via b (K)
     *
     * g0 = standard gravitational acceleration (m/s^2)
     *
     * h = altitude over sea level (m)
     *
     * hb = base altitude via b (m)
     *
     * R = universal gas constant (N-m/(mol-K))
     *
     * M = molar mass of Earth's air (kg/mol)
     *
     * L = temperature lapse rate (K/m)
     * @param y The Y value to get the air density for.
     * @param maxHeight The maximum Y value in the world.
     * @return The air density at the given Y value, in kg/m^3.
     * @see <a href="https://en.wikipedia.org/wiki/Barometric_formula">Wikipedia source for krabber patter formuler</a>
     */
    fun getAirDensityForY(y: Double, maxHeight: Double): Double {
        val worldScale = 11000.0 / (maxHeight - 63.0)

        val realAltitude = if ((y - 63.0) * worldScale >= 0) {
            (y - 63.0) * worldScale
        } else {
            0.0
        }

        val layer = when {
            realAltitude < 11000 -> 0
            realAltitude < 20000 -> 1
            realAltitude < 32000 -> 2
            realAltitude < 47000 -> 3
            realAltitude < 51000 -> 4
            realAltitude < 71000 -> 5
            else -> 6
        }

        val hb = when (layer) {
            0 -> 0.0
            1 -> 11000.0
            2 -> 20000.0
            3 -> 32000.0
            4 -> 47000.0
            5 -> 51000.0
            6 -> 71000.0
            else -> 0.0
        }

        val pb = when (layer) {
            0 -> 1.225
            1 -> 0.36391
            2 -> 0.08803
            3 -> 0.01322
            4 -> 0.00143
            5 -> 0.00086
            6 -> 0.000064
            else -> 0.0
        }

        val Tb = when (layer) {
            0 -> 288.15
            1 -> 216.65
            2 -> 216.65
            3 -> 228.65
            4 -> 270.65
            5 -> 270.65
            6 -> 214.65
            else -> 0.0
        }

        val g0 = GRAVITATIONAL_ACCELERATION

        val R = UNIVERSAL_GAS_CONSTANT

        val M = AIR_MOLAR_MASS

        val L = when (layer) {
            0 -> 0.0065
            1 -> 0.0
            2 -> -0.001
            3 -> -0.0028
            4 -> 0.0
            5 -> 0.0028
            6 -> 0.002
            else -> 0.0
        }

        return when (L != 0.0) {
            true -> pb * Math.pow((Tb - (realAltitude - hb) * L) / Tb, ((g0 * M) / (R * L) -1.0))
            else -> pb * Math.exp((-g0 * M * (realAltitude - hb)) / (R * Tb))
        }
    }

    fun getDensityFromTemperature(volume: Double, mass: Double, temperature: Double, gasType: GasType): Double {
        if (volume == 0.0) return 0.0

        var density = (mass/1000.0) / volume

        if (temperature != 0.0) {
            val molarMass = gasType.density * 22.4
            val pressure = calcPressure(mass/1000.0, volume, temperature, gasType)
            density = (molarMass * pressure) / (UNIVERSAL_GAS_CONSTANT * temperature)
        }
        return density
    }

    /**
     * Calculates pressure using the ideal gas law.
     */
    fun calcPressure(mass: Double, volume: Double, temp: Double, gasType: GasType): Double {
        if (volume == 0.0) return 0.0
        val adjustedTemp = max(temp,0.001)
        val pressure: Double
        val molarMass = gasType.density * 22.4
        val moles = mass / molarMass
        pressure = ((moles) * UNIVERSAL_GAS_CONSTANT * adjustedTemp) / volume
        return pressure
    }

    /**
     * Calculates pressure using the ideal gas law. For use with the average of multiple gas types rather than one.
     */
    fun calcPressure(mass: Double, volume: Double, temp: Double, density: Double): Double {
        if (volume == 0.0 || density == 0.0) return 0.0
        val adjustedTemp = max(temp,0.001)
        val pressure: Double
        val molarMass = density * 22.4
        val moles = mass / molarMass
        pressure = (moles * UNIVERSAL_GAS_CONSTANT * adjustedTemp) / volume
        return pressure
    }

    fun densityAverage(gasMasses: EnumMap<GasType, Double>): Double {
        val totalMass = gasMasses.values.sum()

        if (totalMass == 0.0) {
            return 0.0
        }

        val massPerGas = EnumMap<GasType, Double>(GasType::class.java)

        val gasWeight = EnumMap<GasType, Double>(GasType::class.java)

        gasMasses.keys.forEach {
            if (gasMasses[it] != 0.0 ) {


                massPerGas[it] =  gasMasses[it]!!

            }

        }

        for (gas in massPerGas.keys) {

            gasWeight[gas] = massPerGas[gas]!! / totalMass
        }

        var density = 0.0

        for (gas in gasWeight.keys) {
            density += gasWeight[gas]!! * gas.density
        }


        return density
    }

    fun densityFromPressureAverage(gasMasses: EnumMap<GasType, Double>, temp: Double, pressure: Double): Double {
        val totalMass = gasMasses.values.sum()
        if (totalMass == 0.0) {
            return 0.0
        }

        val massPerGas = EnumMap<GasType, Double>(GasType::class.java)

        val gasWeight = EnumMap<GasType, Double>(GasType::class.java)

        gasMasses.keys.forEach {
            if (gasMasses[it] != 0.0 ) {
                massPerGas[it] =  gasMasses[it]!!
            }

        }

        for (gas in massPerGas.keys) {
            gasWeight[gas] = massPerGas[gas]!! / totalMass
        }

        var density = 0.0

        for (gas in gasWeight.keys) {
            val molarMass = gas.density * 22.4
            val specificGasConstant = idealGasConstant / molarMass
            density += gasWeight[gas]!! * (pressure / (specificGasConstant * temp))
        }

        return density
    }

    fun dynamicViscosityAverage(gasMasses: EnumMap<GasType, Double>, temp: Double): Double {
        val totalMass = gasMasses.values.sum()
        if (totalMass == 0.0) {
            return 0.0
        }

        val massPerGas = EnumMap<GasType, Double>(GasType::class.java)

        val gasWeight = EnumMap<GasType, Double>(GasType::class.java)

        gasMasses.keys.forEach {
            if (gasMasses[it] != 0.0 ) {
                massPerGas[it] =  gasMasses[it]!!
            }

        }

        for (gas in massPerGas.keys) {
            gasWeight[gas] = massPerGas[gas]!! / totalMass
        }

        var viscosity = 0.0

        for (gas in gasWeight.keys) {
            viscosity += gasWeight[gas]!! * (gas.viscosity * (temp / 273.15) * ((273.15 + gas.sutherlandConstant) / (temp + gas.sutherlandConstant)))
        }

        return viscosity
    }

    fun calculateFlow(pressureOne: Double, pressureTwo: Double, radius: Double, length: Double, densityA: Double, densityB: Double, viscosity: Double, pumpPressure: Double = 0.0, previousFlowRate: Double = 0.0): Double {
        var flowRate = 0.0
        if (densityA <= 0 && densityB <= 0) {
            return flowRate
        }
        val density = if (pressureOne > pressureTwo) densityA else densityB
        // -- constants
        // (meters)
        val pipeRoughness = 0.00012
        val pipeDiameter = radius * 2.0

        var pressureDrop = (pressureOne - pressureTwo + pumpPressure)

        if (pressureOne <= 0.0001 && pumpPressure.absoluteValue > 0.0) {
            pressureDrop = min(pressureDrop, 0.0)
        }

        if (pressureTwo <= 0.0001 && pumpPressure < 0.0) {
            pressureDrop = max(pressureDrop, 0.0)
        }

        val finalPressureDrop = pressureDrop

        val Re = max((density * previousFlowRate * pipeDiameter) / viscosity, 0.0001)

        var f: Double = if (Re < 2000) {
            64.0/Re
        } else if (Re > 4000) {
            0.25 / (Math.pow(Math.log10(((pipeRoughness / pipeDiameter) / 3.7) + (5.74 / Math.pow(Re, 0.9))), 2.0))
        } else {
            Mth.clampedLerp(64.0/Re, 0.25 / (Math.pow(Math.log10(((pipeRoughness / pipeDiameter) / 3.7) + (5.74 / Math.pow(Re, 0.9))), 2.0)),(Re-2000.0)/(4000.0-2000.0))
        }

        val flowSpeed = (2.0*finalPressureDrop.absoluteValue)/(f * (length/pipeDiameter) * density)
        val sqrtFlowSpeed = sign(finalPressureDrop) * sqrt(flowSpeed)
        val volumetricFlowRate = sqrtFlowSpeed * (Math.pow(Math.PI * radius, 2.0) / 4.0)

        flowRate = volumetricFlowRate * density

        return flowRate
    }

    fun specificHeatAverage(gasMasses: EnumMap<GasType, Double>): Double {
        val totalMass = gasMasses.values.sum()
        if (totalMass == 0.0) {
            return 0.0
        }

        val massPerGas = EnumMap<GasType, Double>(GasType::class.java)

        val gasWeight = EnumMap<GasType, Double>(GasType::class.java)

        gasMasses.keys.forEach {
            if (gasMasses[it] != 0.0 ) {
                massPerGas[it] =  gasMasses[it]!!
            }

        }

        for (gas in massPerGas.keys) {
            gasWeight[gas] = massPerGas[gas]!! / totalMass
        }

        var specificHeat = 0.0

        for (gas in gasWeight.keys) {
            specificHeat += gasWeight[gas]!! * gas.specificHeatCapacity
        }

        return specificHeat
    }


    // useful values

    const val DRAG_COEFFICIENT = 3.15
    const val GRAVITATIONAL_ACCELERATION = 9.80665
    const val UNIVERSAL_GAS_CONSTANT = 8.3144598
    const val AIR_MOLAR_MASS = 0.0289644
}