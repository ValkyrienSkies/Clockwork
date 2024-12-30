package org.valkyrienskies.clockwork.integration.cc.apis

import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.ILuaAPI
import dan200.computercraft.api.lua.LuaFunction
import org.valkyrienskies.clockwork.integration.cc.peripherals.generic.GasHeatMethods
import org.valkyrienskies.clockwork.util.AerodynamicUtils
import java.util.HashMap

object AerodynamicsAPI: ILuaAPI {
    override fun getNames() =
        arrayOf("aerodynamics", "aero")

    val dragCoefficient: Double
        @LuaFunction
        get() = AerodynamicUtils.DRAG_COEFFICIENT

    val gravitationalAcceleration: Double
        @LuaFunction
        get() = AerodynamicUtils.GRAVITATIONAL_ACCELERATION

    val universalGasConstant: Double
        @LuaFunction
        get() = AerodynamicUtils.UNIVERSAL_GAS_CONSTANT

    val airMolarMass: Double
        @LuaFunction
        get() = AerodynamicUtils.AIR_MOLAR_MASS

    val ductRadius: Double
        @LuaFunction
        get() = AerodynamicUtils.DUCT_RADIUS

    val ductArea: Double
        @LuaFunction
        get() = AerodynamicUtils.DUCT_AREA

    @LuaFunction
    fun getAtmosphereForDimension(dimensionId: String): Map<String, Double> {
        val (max, sea) = AerodynamicUtils.getAtmosphereForDimension(dimensionId.toDimensionId())
        return mapOf(
            "max" to max,
            "seaLevel" to sea
        )
    }

    @LuaFunction
    fun getAirDensityForY(y: Double, dimensionId: String) =
        AerodynamicUtils.getAirDensityForY(y, dimensionId.toDimensionId())

    @LuaFunction
    fun getAirPressureForY(y: Double, dimensionId: String) =
        AerodynamicUtils.getAirPressureForY(y, dimensionId.toDimensionId())

    @LuaFunction
    fun getAirTemperatureForY(y: Double, dimensionId: String) =
        AerodynamicUtils.getAirTemperatureForY(y, dimensionId.toDimensionId())

    @LuaFunction
    fun getDensityFromTemperature(volume: Double, mass: Double, temperature: Double, gasType: String) =
        AerodynamicUtils.getDensityFromTemperature(volume, mass, temperature, GasHeatMethods.getGasOrThrow(gasType))

    @LuaFunction
    fun calculatePressure(mass: Double, volume: Double, temperature: Double, gasType: String) =
        AerodynamicUtils.calcPressure(mass, volume, temperature, GasHeatMethods.getGasOrThrow(gasType))

    @LuaFunction
    fun calculatePressureWithAverageDensity(mass: Double, volume: Double, temperature: Double, standardDensity: Double) =
        AerodynamicUtils.calcPressure(mass, volume, temperature, standardDensity)

    @LuaFunction
    fun getAverageDensity(gasMasses: Map<String, Double>) =
        AerodynamicUtils.densityAverage(HashMap(gasMasses.mapKeys { (gas, _) ->
            GasHeatMethods.getGasOrThrow(gas) }))

    @LuaFunction
    fun getDensityFromPressureAverage(gasMasses: Map<String, Double>, temperature: Double, pressure: Double) =
        AerodynamicUtils.densityFromPressureAverage(HashMap(gasMasses.mapKeys { (gas, _) ->
            GasHeatMethods.getGasOrThrow(gas) }), temperature, pressure)

    @LuaFunction
    fun getDynamicViscosityAverage(gasMasses: Map<String, Double>, temperature: Double) =
        AerodynamicUtils.dynamicViscosityAverage(HashMap(gasMasses.mapKeys { (gas, _) ->
            GasHeatMethods.getGasOrThrow(gas) }), temperature)

    @LuaFunction
    fun calculateFlow(pressureOne: Double, pressureTwo: Double, radius: Double, length: Double, densityA: Double, densityB: Double, viscosity: Double, args: IArguments): Double {
        val pumpPressure = args.optDouble(0)
        val previousFlowRate = args.optDouble(1)

        return if (pumpPressure.isEmpty && previousFlowRate.isEmpty)
            AerodynamicUtils.calculateFlow(pressureOne, pressureTwo, radius, length, densityA, densityB, viscosity)
        else if (pumpPressure.isPresent && previousFlowRate.isEmpty)
            AerodynamicUtils.calculateFlow(pressureOne, pressureTwo, radius, length, densityA, densityB, viscosity, pumpPressure.get())
        else if (pumpPressure.isEmpty && previousFlowRate.isPresent)
            AerodynamicUtils.calculateFlow(pressureOne, pressureTwo, radius, length, densityA, densityB, viscosity, previousFlowRate = previousFlowRate.get())
        else
            AerodynamicUtils.calculateFlow(pressureOne, pressureTwo, radius, length, densityA, densityB, viscosity, pumpPressure.get(), previousFlowRate.get())
    }

    @LuaFunction
    fun getSpecificHeatAverage(gasMasses: Map<String, Double>) =
        AerodynamicUtils.specificHeatAverage(HashMap(gasMasses.mapKeys { (gas, _) ->
            GasHeatMethods.getGasOrThrow(gas) }))

    @LuaFunction
    fun getExtraHeatInfoAverage(gasMasses: Map<String, Double>): Map<String, Double> {
        val (specificGasConstant, sutherlandConstant, adiabaticIndex) =
            AerodynamicUtils.extraHeatInfoAverage(HashMap(gasMasses.mapKeys { (gas, _) ->
                GasHeatMethods.getGasOrThrow(gas) }))

        return mapOf(
            "specificGasConstant" to specificGasConstant,
            "sutherlandConstant" to sutherlandConstant,
            "adiabaticIndex" to adiabaticIndex
        )
    }

    private fun String.toDimensionId(): String {
        var dimId = this
        if (!this.contains("minecraft:dimension:"))
            dimId = "minecraft:dimension:$this"
        return dimId
    }
}