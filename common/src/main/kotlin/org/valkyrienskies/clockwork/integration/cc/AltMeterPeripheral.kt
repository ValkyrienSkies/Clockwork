@file:Suppress("unused")

package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import org.valkyrienskies.clockwork.content.curiosities.altmeter.AltMeterBlockEntity
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.common.getLoadedShipManagingPos

class AltMeterPeripheral(private val be: AltMeterBlockEntity): IPeripheral {
    @LuaFunction
    fun getHeight(): Double {
        val ship = be.level.getLoadedShipManagingPos(be.blockPos)
        val pos = be.blockPos.center
        val height = ship?.transform?.shipToWorld?.transformPosition(pos.toJOML())?.y ?: pos.y
        return height
    }

    @LuaFunction
    fun getOutput(): Int = be.signalStrength

    @LuaFunction
    fun getTargetHeight(): Int = be.triggerHeight

    @LuaFunction
    fun setTargetHeight(height: Int) {
        be.triggerHeight = height
        be.notifyUpdate()
    }

    @LuaFunction
    fun getSensitivity(): Int = be.triggerSensitivity

    @LuaFunction
    fun setSensitivity(sensitivity: Int) {
        be.triggerSensitivity = sensitivity
        be.notifyUpdate()
    }

    @LuaFunction
    fun getDirection(): String = be.triggerDirection.name

    @LuaFunction
    fun setDirection(direction: String) {
        try {
            be.triggerDirection = AltMeterBlockEntity.AltMeterDirection.valueOf(direction.uppercase())
            be.notifyUpdate()
        } catch (e: IllegalArgumentException) {
            throw LuaException(
                "Direction must be one of: " +
                        AltMeterBlockEntity.AltMeterDirection.entries.joinToString(", ") { it.name }
            )
        }
    }

    override fun equals(p0: IPeripheral?): Boolean = be.blockPos == (p0 as? AltMeterPeripheral)?.be?.blockPos
    override fun getType(): String = "cw_alt_meter"
}