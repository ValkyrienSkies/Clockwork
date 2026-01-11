@file:Suppress("unused")

package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import net.createmod.catnip.animation.LerpedFloat
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle.GasNozzleBlockEntity

class GasNozzlePeripheral(private val be: GasNozzleBlockEntity): IPeripheral {
    @LuaFunction fun setPointer(value: Double) {
        be.pointer.chase(value.coerceIn(0.0, 1.0), 0.3, LerpedFloat.Chaser.LINEAR)
        be.sendData()
    }

    @LuaFunction fun hasBalloon() = be.hasPocket

    @LuaFunction fun getPointer() = be.pointer.value
    @LuaFunction fun getPointerSpeed() = be.pointerSpeed
    @LuaFunction fun getPocketTemperature() = if (be.hasPocket) be.pocketTemperature else 0.0
    @LuaFunction fun getTargetTemperature() = ClockworkMod.getKelvin().getTemperatureAt(be.getDuctNodePosition()) * be.pointer.value
    @LuaFunction fun getBalloonVolume() = be.balloonVolume
    @LuaFunction fun getLeaks() = if (be.hasPocket) be.currentIdealOutput.toInt() else 0

    override fun equals(p0: IPeripheral?): Boolean = be.level?.getBlockState(be.blockPos)?.`is`(ClockworkBlocks.GAS_NOZZLE.get()) == true
    override fun getType(): String = "cw_gas_nozzle"
}