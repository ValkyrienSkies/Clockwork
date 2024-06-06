package org.valkyrienskies.clockwork.integration.cc.peripheral

import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import org.joml.Quaterniond
import org.valkyrienskies.clockwork.content.contraptions.phys.gyro.GyroBlockEntity

class GyroscopePeripheral(val gyroscope: GyroBlockEntity): IPeripheral {
    override fun equals(peripheral: IPeripheral?): Boolean = peripheral is GyroscopePeripheral && peripheral.gyroscope == this.gyroscope

    override fun getType() = "gyroscope"

    @LuaFunction
    fun getTargetQuaternion(): Map<String, Double> {
        val q = this.gyroscope.targetQuat
        return mapOf(
            Pair("x", q.x),
            Pair("y", q.y),
            Pair("z", q.z),
            Pair("w", q.w)
        )
    }

    fun setTargetQuaternion(args: IArguments) {
        this.gyroscope.targetQuat = Quaterniond(
            args.optDouble(0, 0.0),
            args.optDouble(1, 0.0),
            args.optDouble(2, 0.0),
            args.optDouble(3, 0.0)
        )
    }

    @LuaFunction
    fun getRedstonePower() = mapOf(
        Pair("x", this.gyroscope.redstonePower.x),
        Pair("y", this.gyroscope.redstonePower.y)
    )

    @LuaFunction
    fun getSpeed() = this.gyroscope.speed

    @LuaFunction
    fun getForceMultiplier() = this.gyroscope.control?.speedToForce(this.gyroscope.control?.speed ?: 0f) ?: 0.0

    @LuaFunction
    fun getGyroCount() = this.gyroscope.control?.gyros ?: 0
}