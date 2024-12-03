package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.GenericPeripheral
import dan200.computercraft.api.peripheral.PeripheralType
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.ClockworkMod.Kelvin
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.kelvin.api.GasType

object GasHeatSource: GenericPeripheral {
    override fun id() = ResourceLocation(MOD_ID, "gasheat")

    override fun getType(): PeripheralType {
        return PeripheralType.ofType("gasheat")
    }

    @LuaFunction
    @JvmStatic
    fun getGasMass(heatable: IHeatableBlockEntity): Map<Map<String, Any>, Double> =
        Kelvin.getGasMassAt(heatable.getDuctNodePosition()).mapKeys { (gas, _) -> gas.toLua() }

    @LuaFunction
    @JvmStatic
    fun getHeatEnergy(heatable: IHeatableBlockEntity) =
        Kelvin.getHeatEnergy(heatable.getDuctNodePosition())

    @LuaFunction
    @JvmStatic
    fun getPressure(heatable: IHeatableBlockEntity) =
        Kelvin.getPressureAt(heatable.getDuctNodePosition())

    @LuaFunction
    @JvmStatic
    fun getTemperature(heatable: IHeatableBlockEntity) =
        Kelvin.getTemperatureAt(heatable.getDuctNodePosition())

    fun GasType.toLua(): Map<String, Any> {
        return mapOf(
            "name" to this.name,
            "density" to this.density,
            "viscosity" to this.viscosity,
            "specificHeatCapacity" to this.specificHeatCapacity,
            "thermalConductivity" to this.thermalConductivity,
            "sutherlandConstant" to this.sutherlandConstant,
            "adiabaticIndex" to this.adiabaticIndex
        )
    }
}