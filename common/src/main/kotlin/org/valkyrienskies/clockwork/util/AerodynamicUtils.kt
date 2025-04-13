package org.valkyrienskies.clockwork.util

import com.google.gson.Gson
import com.google.gson.JsonElement
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.Mth
import net.minecraft.util.profiling.ProfilerFiller
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.AerodynamicUtils.Parameters
import org.valkyrienskies.clockwork.util.AerodynamicUtils.dimensionMap
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.DuctNetwork.Companion.idealGasConstant
import kotlin.collections.HashMap
import kotlin.math.*

object AtmosphereParametersResolver: SimpleJsonResourceReloadListener(Gson(), "atmosphere_parameters") {
    override fun apply(
        objects: Map<ResourceLocation?, JsonElement?>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        val temp = hashMapOf<String, Parameters>()

        objects.forEach { (key, value) ->
            if (key == null || value == null) {return@forEach}
            try {
                if (value.isJsonArray) {
                    value.asJsonArray.forEach { parse(it, temp) }
                } else if (value.isJsonObject) {
                    parse(value, temp)
                } else throw IllegalArgumentException()
            } catch (e: Exception) {
                ClockworkMod.LOGGER.error(e.stackTraceToString())
            }
        }

        dimensionMap = temp
    }

    //TODO add dimensionId verification somehow?
    private fun parse(element: JsonElement, map: MutableMap<String, Parameters>) {
        val maxYPos = element.asJsonObject["maxYPos"]?.asDouble ?: throw NoSuchElementException("Parameter \"maxYPos\" wasn't filled")
        val seaLevel = element.asJsonObject["seaLevel"]?.asDouble ?: throw NoSuchElementException("Parameter \"seaLevel\" wasn't filled")
        val dimensionId = element.asJsonObject["dimensionId"]?.asString ?: throw NoSuchElementException("Parameter \"dimensionId\" wasn't filled")
        val priority = element.asJsonObject["priority"]?.asInt ?: 0

        map.getOrPut(dimensionId) { Parameters(maxYPos, seaLevel, priority) }.also {
            if (it.priority < priority) {
                map[dimensionId] = Parameters(maxYPos, seaLevel, priority)
            }
        }
    }
}

/**
 * Contains useful functions for features that need funny wind maths. Mainly drag and balloons.
 */
object AerodynamicUtils {
    data class Parameters(val maxY: Double, val seaLevel: Double, val priority: Int = -1)

    var dimensionMap: HashMap<DimensionId, Parameters> = HashMap()
    const val DEFAULT_MAX = 562.0
    const val DEFAULT_SEA_LEVEL = 62.0

    val defaultParameters = Parameters(DEFAULT_MAX, DEFAULT_SEA_LEVEL)

    fun getAtmosphereForDimension(id: DimensionId) = dimensionMap[id] ?: defaultParameters


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
    fun getAirDensityForY(y: Double, dimension: DimensionId): Double {
        val (maxHeight, seaLevel) = getAtmosphereForDimension(dimension)
        if (maxHeight <= 0.0) {
            return 0.0
        }
        val worldScale = 71000.0 / (maxHeight - seaLevel)

        val realAltitude = if ((y - seaLevel) * worldScale >= 0) {
            (y - seaLevel) * worldScale
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
        //println("Height: $y, Real Altitude: $realAltitude, Layer: $layer")

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

    fun getAirPressureForY(y: Double, dimension: DimensionId): Double {
        val (maxHeight, seaLevel) = getAtmosphereForDimension(dimension)
        if (maxHeight <= 0.0) {
            return 0.0
        }
        val worldScale = 71000.0 / (maxHeight - seaLevel)

        val realAltitude = if ((y - seaLevel) * worldScale >= 0) {
            (y - seaLevel) * worldScale
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
            0 -> 101325.0
            1 -> 22632.1
            2 -> 5474.89
            3 -> 868.02
            4 -> 110.91
            5 -> 66.94
            6 -> 3.96
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
            true -> pb * Math.pow(1.0 - (L / Tb) * (realAltitude - hb), ((g0 * M) / (R * L)))
            else -> pb * Math.exp((-g0 * M * (realAltitude - hb)) / (R * Tb))
        }
    }

    fun getAirTemperatureForY(y: Double, dimension: DimensionId): Double {
        val (maxHeight, seaLevel) = getAtmosphereForDimension(dimension)
        if (maxHeight <= 0.0) {
            return 0.0
        }
        val worldScale = 71000.0 / (maxHeight - seaLevel)

        val realAltitude = if ((y - seaLevel) * worldScale >= 0) {
            (y - seaLevel) * worldScale
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
            true -> Tb - ((realAltitude - hb) * L)
            else -> Tb
        }
    }

    fun getDensityFromTemperature(volume: Double, mass: Double, temperature: Double, gasType: GasType): Double {
        if (volume == 0.0) return 0.0

        var density = (mass) / volume

        if (temperature != 0.0) {
            val molarMass = gasType.density * 22.4
            val pressure = calcPressure(mass, volume, temperature, gasType)
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
    fun calcPressure(mass: Double, volume: Double, temp: Double, standardDensity: Double): Double {
        if (volume == 0.0 || mass == 0.0) return 0.0
        val adjustedTemp = max(temp,0.0001)
        val pressure: Double
        val density: Double = mass / volume
        val molarMass = standardDensity * 22.4
        val specificGasConstant = (idealGasConstant / molarMass) * 1000.0
        val moles = mass / molarMass
        pressure = (density * specificGasConstant * adjustedTemp)

        return pressure
    }

    fun densityAverage(gasMasses: HashMap<GasType, Double>): Double {
        val totalMass = gasMasses.values.sum()

        if (totalMass == 0.0) {
            return 0.0
        }

        val massPerGas = HashMap<GasType, Double>()

        val gasWeight = HashMap<GasType, Double>()

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

    fun densityFromPressureAverage(gasMasses: HashMap<GasType, Double>, temp: Double, pressure: Double): Double {
        val totalMass = gasMasses.values.sum()
        if (totalMass == 0.0) {
            return 0.0
        }

        val massPerGas = HashMap<GasType, Double>()

        val gasWeight = HashMap<GasType, Double>()

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

    fun dynamicViscosityAverage(gasMasses: HashMap<GasType, Double>, temp: Double): Double {
        val totalMass = gasMasses.values.sum()
        if (totalMass == 0.0) {
            return 0.0
        }

        val massPerGas = HashMap<GasType, Double>()

        val gasWeight = HashMap<GasType, Double>()

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

    fun specificHeatAverage(gasMasses: HashMap<GasType, Double>): Double {
        val totalMass = gasMasses.values.sum()
        if (totalMass == 0.0) {
            return 0.0
        }

        val massPerGas = HashMap<GasType, Double>()

        val gasWeight = HashMap<GasType, Double>()

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

    //Returns an average Specific Gas Constant, Sutherland Constant, and Adiabatic Index for a given gas mixture
    fun extraHeatInfoAverage(gasMasses: HashMap<GasType, Double>): Triple<Double,Double,Double> {
        val totalMass = gasMasses.values.sum()
        if (totalMass == 0.0) {
            return Triple(0.0,0.0,0.0)
        }

        val massPerGas = HashMap<GasType, Double>()

        val gasWeight = HashMap<GasType, Double>()

        gasMasses.keys.forEach {
            if (gasMasses[it] != 0.0 ) {
                massPerGas[it] =  gasMasses[it]!!
            }

        }

        for (gas in massPerGas.keys) {
            gasWeight[gas] = massPerGas[gas]!! / totalMass
        }

        var specificGasConstant = 0.0
        var sutherlandConstant = 0.0
        var adiabaticIndex = 0.0

        for (gas in gasWeight.keys) {
            specificGasConstant += gasWeight[gas]!! * (UNIVERSAL_GAS_CONSTANT / (gas.density * 22.4))
            sutherlandConstant += gasWeight[gas]!! * gas.sutherlandConstant
            adiabaticIndex += gasWeight[gas]!! * gas.adiabaticIndex
        }

        return Triple(specificGasConstant, sutherlandConstant, adiabaticIndex)
    }


    // useful values

    const val DRAG_COEFFICIENT = 2.18
    const val GRAVITATIONAL_ACCELERATION = 9.80665
    const val UNIVERSAL_GAS_CONSTANT = 8.314
    const val AIR_MOLAR_MASS = 0.0289644
    const val DUCT_RADIUS = 0.1875
    const val DUCT_AREA = 0.11045
}