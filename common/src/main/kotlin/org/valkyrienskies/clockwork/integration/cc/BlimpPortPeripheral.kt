@file:Suppress("unused")

package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import org.valkyrienskies.clockwork.content.logistics.gas.pockets.blimp.BlimpPortBlockEntity

class BlimpPortPeripheral(private val be: BlimpPortBlockEntity) : IPeripheral {

    @LuaFunction
    fun getAngle(): Float = (be.computerTarget?.toFloat() ?: (if (be.speed > 0) 1 else 0).toFloat()) * 90f

    @LuaFunction
    fun getActualAngle(): Float = be.pointer.value * 90f

    @LuaFunction
    fun setAngle(angle: Double) {
        if (angle < 0 || angle > 90 || !angle.isFinite()) {
            throw LuaException("Angle must be within range 0..90")
        }
        be.computerTarget = angle / 90.0
        be.updateTarget()
    }

    @LuaFunction
    fun resetAngle() {
        be.computerTarget = null
        be.updateTarget()
    }

    @LuaFunction
    fun hasEnvelope(): Boolean = be.hasPocket

    @LuaFunction
    fun getVolume(): Double = be.balloonVolume

    @LuaFunction
    fun getEffectiveVolume(): Double = be.effectiveVolume

    @LuaFunction
    fun getInflation(): Double = be.inflationRatio

    @LuaFunction
    fun getPressure(): Double = be.balloonPressure

    @LuaFunction
    fun getTemperature(): Double = be.balloonTemperature

    @LuaFunction
    fun getLeaks(): Int = be.leakCount

    @LuaFunction
    fun getGases(): Map<String, Double> = HashMap(be.syncedGases)

    @LuaFunction
    fun rescan() {
        be.requestScan()
    }

    override fun detach(computer: IComputerAccess?) {
        be.computerTarget = null
        be.updateTarget()
    }

    override fun equals(other: IPeripheral?): Boolean = be.blockPos == (other as? BlimpPortPeripheral)?.be?.blockPos
    override fun getType(): String = "cw_blimp_port"
}
