package org.valkyrienskies.clockwork.integration.cc.api

import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.ILuaAPI
import dan200.computercraft.api.lua.LuaFunction
import org.valkyrienskies.clockwork.util.AerodynamicUtils

class AerodynamicAPI: ILuaAPI {
    override fun getNames() = arrayOf("aerodynamic, aero")

    @LuaFunction
    fun getAirMolarMass() = AerodynamicUtils.AIR_MOLAR_MASS

    @LuaFunction
    fun getDragCoefficient() = AerodynamicUtils.DRAG_COEFFICIENT

    @LuaFunction
    fun getGravitationalAcceleration() = AerodynamicUtils.GRAVITATIONAL_ACCELERATION

    @LuaFunction
    fun getUniversalGasConstant() = AerodynamicUtils.UNIVERSAL_GAS_CONSTANT

    @LuaFunction
    fun getAirDensityAtAltitude(altitude: Double, maxAltitude: IArguments) =
        AerodynamicUtils.getAirDensityForY(altitude, maxAltitude.optDouble(0,320.0))
}