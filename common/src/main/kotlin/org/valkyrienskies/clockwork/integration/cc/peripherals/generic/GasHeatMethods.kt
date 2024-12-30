package org.valkyrienskies.clockwork.integration.cc.peripherals.generic

import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.GenericPeripheral
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.PeripheralType
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
import org.valkyrienskies.clockwork.ClockworkMod.getKelvin
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.GasTypeRegistry
import java.util.*
import kotlin.jvm.Throws
import kotlin.jvm.optionals.getOrDefault

object GasHeatMethods: GenericPeripheral {
    override fun id() =
        ResourceLocation(MOD_ID, "kelvin")

    override fun getType(): PeripheralType =
        PeripheralType.ofAdditional("kelvin")

    @LuaFunction
    @JvmStatic
    fun getGasDetails(heatable: IHeatableBlockEntity, gasName: String): Map<String, Any> =
        getGasOrThrow(gasName).toLua()

    @LuaFunction
    @JvmStatic
    fun getGasMass(heatable: IHeatableBlockEntity): Map<String, Double> =
        getKelvin().getGasMassAt(heatable.getDuctNodePosition()).mapKeys { (gas, _) -> gas.name }

    @LuaFunction
    @JvmStatic
    fun getHeatEnergy(heatable: IHeatableBlockEntity) =
        getKelvin().getHeatEnergy(heatable.getDuctNodePosition())


    @LuaFunction
    @JvmStatic
    fun getPressure(heatable: IHeatableBlockEntity) =
        getKelvin().getPressureAt(heatable.getDuctNodePosition())

    @LuaFunction
    @JvmStatic
    fun getTemperature(heatable: IHeatableBlockEntity) =
        getKelvin().getTemperatureAt(heatable.getDuctNodePosition())

    @LuaFunction
    @JvmStatic
    fun pushGas(from: IHeatableBlockEntity, computer: IComputerAccess, toName: String, gasName: String, amount: Optional<Double>) {
        val origin = from.getDuctNodePosition()
        val end = getNodePosFromPeripheral(computer, toName)
        val gas = getGasOrThrow(gasName)

        transferGas(origin, end, gas, amount)
    }

    @LuaFunction
    @JvmStatic
    fun pullGas(to: IHeatableBlockEntity, computer: IComputerAccess, fromName: String, gasName: String, amount: Optional<Double>) {
        val end = to.getDuctNodePosition()
        val origin = getNodePosFromPeripheral(computer, fromName)
        val gas = getGasOrThrow(gasName)

        transferGas(origin, end, gas, amount)
    }

    @LuaFunction
    @JvmStatic
    fun pushTemperature(from: IHeatableBlockEntity, computer: IComputerAccess, toName: String, amount: Optional<Double>) {
        val origin = from.getDuctNodePosition()
        val end = getNodePosFromPeripheral(computer, toName)

        transferTemperature(origin, end, amount)
    }

    @LuaFunction
    @JvmStatic
    fun pullTemperature(to: IHeatableBlockEntity, computer: IComputerAccess, fromName: String, amount: Optional<Double>) {
        val end = to.getDuctNodePosition()
        val origin = getNodePosFromPeripheral(computer, fromName)

        transferTemperature(origin, end, amount)
    }

    @Throws(LuaException::class)
    private fun getNodePosFromPeripheral(computer: IComputerAccess, name: String): DuctNodePos {
        val location = computer.getAvailablePeripheral(name)
            ?: throw LuaException("Target '$name' does not exist!")
        val other = location.target as? IHeatableBlockEntity
            ?: throw LuaException("Target '$name' is not a valid Node!")

        return other.getDuctNodePosition()
    }

    @Throws(LuaException::class)
    fun getGasOrThrow(gasName: String): GasType {
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

        if (actualAmount <= 0)
            throw LuaException("Cannot transfer zero or negative gas...")
        if (currentAmount < actualAmount)
            throw LuaException("Exceeded Amount of `${gas.name}` in origin!")

        kelvin.modGasMass(origin, gas, -actualAmount)

        kelvin.modGasMass(end, gas, 0.0)
        kelvin.modGasMass(end, gas, actualAmount)
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
            "combustible" to this.combustible,
            "calorificValue" to this.calorificValue
        )
    }
}