package org.valkyrienskies.clockwork.integration.cc.peripheral

import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import org.valkyrienskies.clockwork.content.contraptions.phys.gyro.GyroBlockEntity

class GyroscopePeripheral(val gyroscope: GyroBlockEntity): IPeripheral {
    override fun equals(peripheral: IPeripheral?): Boolean = peripheral is GyroscopePeripheral && peripheral.gyroscope == this.gyroscope

    override fun getType() = "gyroscope"

    //TODO: MrSterner!!!! (Dinkleberg!!!!)
}