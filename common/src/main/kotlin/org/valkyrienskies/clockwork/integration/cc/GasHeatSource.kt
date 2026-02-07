@file:Suppress("unused")

package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.GenericPeripheral
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.PeripheralType
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
import org.valkyrienskies.clockwork.ClockworkMod.getKelvin
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.INodeBlockEntity
import java.util.*
import kotlin.jvm.Throws
import kotlin.jvm.optionals.getOrDefault

object GasHeatSource: GenericPeripheral {
    override fun id() = "$MOD_ID:kelvin"

    override fun getType(): PeripheralType =
        PeripheralType.ofAdditional("kelvin")

    @LuaFunction
    @JvmStatic
    fun getGasDetails(heatable: INodeBlockEntity, gasName: String): Map<String, Any> =
        getGasOrThrow(gasName).toLua()

    @LuaFunction
    @JvmStatic
    fun getGasMass(heatable: INodeBlockEntity): Map<String, Double> =
        getKelvin().getGasMassAt(heatable.getDuctNodePosition()).mapKeys { (gas, _) -> gas.name }

    @LuaFunction
    @JvmStatic
    fun getHeatEnergy(heatable: INodeBlockEntity) =
        getKelvin().getHeatEnergy(heatable.getDuctNodePosition())


    @LuaFunction
    @JvmStatic
    fun getPressure(heatable: INodeBlockEntity) =
        getKelvin().getPressureAt(heatable.getDuctNodePosition())

    @LuaFunction
    @JvmStatic
    fun getTemperature(heatable: INodeBlockEntity) =
        getKelvin().getTemperatureAt(heatable.getDuctNodePosition())

    @LuaFunction
    @JvmStatic
    fun pushGas(from: INodeBlockEntity, computer: IComputerAccess, toName: String, gasName: String, amount: Optional<Double>) {
        if (!ClockworkConfig.SERVER.cheatKelvinPeripheral) throw LuaException("Cheat kelvin peripheral not enabled in config")
        val origin = from.getDuctNodePosition()
        val end = getNodePosFromPeripheral(computer, toName)
        val gas = getGasOrThrow(gasName)

        transferGas(origin, end, gas, amount)
    }

    @LuaFunction
    @JvmStatic
    fun pullGas(to: INodeBlockEntity, computer: IComputerAccess, fromName: String, gasName: String, amount: Optional<Double>) {
        if (!ClockworkConfig.SERVER.cheatKelvinPeripheral) throw LuaException("Cheat kelvin peripheral not enabled in config")
        val end = to.getDuctNodePosition()
        val origin = getNodePosFromPeripheral(computer, fromName)
        val gas = getGasOrThrow(gasName)

        transferGas(origin, end, gas, amount)
    }

    @LuaFunction
    @JvmStatic
    fun pushTemperature(from: INodeBlockEntity, computer: IComputerAccess, toName: String, amount: Optional<Double>) {
        if (!ClockworkConfig.SERVER.cheatKelvinPeripheral) throw LuaException("Cheat kelvin peripheral not enabled in config")
        val origin = from.getDuctNodePosition()
        val end = getNodePosFromPeripheral(computer, toName)

        transferTemperature(origin, end, amount)
    }

    @LuaFunction
    @JvmStatic
    fun pullTemperature(to: INodeBlockEntity, computer: IComputerAccess, fromName: String, amount: Optional<Double>) {
        if (!ClockworkConfig.SERVER.cheatKelvinPeripheral) throw LuaException("Cheat kelvin peripheral not enabled in config")
        val end = to.getDuctNodePosition()
        val origin = getNodePosFromPeripheral(computer, fromName)

        transferTemperature(origin, end, amount)
    }

    @Throws(LuaException::class)
    private fun getNodePosFromPeripheral(computer: IComputerAccess, name: String): DuctNodePos {
        val location = computer.getAvailablePeripheral(name)
            ?: throw LuaException("Target '$name' does not exist!")
        val other = location.target as? INodeBlockEntity
            ?: throw LuaException("Target '$name' is not a valid Node!")

        return other.getDuctNodePosition()
    }

    @Throws(LuaException::class)
    private fun getGasOrThrow(gasName: String): GasType {
        try {
            return GasTypeRegistry.getGasType(ResourceLocation.tryParse(gasName)!!)!!
        } catch (e: AssertionError) {
            throw LuaException("Gas `$gasName` does not exist!")
        }
    }

    @Throws(LuaException::class)
    private fun transferGas(origin: DuctNodePos, end: DuctNodePos, gas: GasType, amount: Optional<Double>) {
        val kelvin = getKelvin()
        val currentAmount = kelvin.getGasMassAt(origin).getOrDefault(gas, 0.0)
        val actualAmount = amount.getOrDefault(currentAmount)
        val currentTemp = kelvin.getTemperatureAt(origin)

        if (actualAmount <= 0)
            throw LuaException("Cannot transfer zero or negative gas...")
        if (currentAmount < actualAmount)
            throw LuaException("Exceeded Amount of `${gas.name}` in origin!")

        kelvin.removeGas(origin, gas, actualAmount)

        //kelvin.modGasMass(end, gas, 0.0)
        kelvin.addGasAtTemperature(end, gas, actualAmount, currentTemp)
    }

    @Throws(LuaException::class)
    private fun transferTemperature(origin: DuctNodePos, end: DuctNodePos, amount: Optional<Double>) {
        val kelvin = getKelvin()
        val currentAmount = kelvin.getTemperatureAt(origin)
        val actualAmount = amount.getOrDefault(currentAmount)

        if (actualAmount <= 0.0001)
            throw LuaException("Cannot transfer zero or negative temperature...")
        if (currentAmount < actualAmount)
            throw LuaException("Exceeded Amount of temperature in origin!")

        kelvin.modTemperature(origin, -actualAmount)
        kelvin.modTemperature(end, actualAmount)
    }

    private fun GasType.toLua(): Map<String, Any> {
        return mapOf(
            "name" to this.name,
            "density" to this.density,
            "viscosity" to this.viscosity,
            "specificHeatCapacity" to this.specificHeatCapacity,
            "thermalConductivity" to this.thermalConductivity,
            "sutherlandConstant" to this.sutherlandConstant,
            "adiabaticIndex" to this.adiabaticIndex,
        )
    }
}
