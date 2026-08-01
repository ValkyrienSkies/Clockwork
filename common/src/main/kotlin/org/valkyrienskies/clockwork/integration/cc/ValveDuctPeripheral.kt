@file:Suppress("unused")

package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import org.valkyrienskies.clockwork.content.logistics.gas.valve.ValveDuctBlockEntity

class ValveDuctPeripheral(private val be: ValveDuctBlockEntity): IPeripheral {

    @LuaFunction
    fun getAngle(): Float = (be.computerTarget?.toFloat() ?: (if (be.speed > 0) 1 else 0).toFloat())*90

    @LuaFunction
    fun getActualAngle(): Float = be.pointer.value*90

    @LuaFunction
    fun setAngle(angle: Double) {
        if ((angle < 0) || (angle > 90) || !angle.isFinite()) {
            throw LuaException("Angle must be within range 0..90")
        }

        be.computerTarget = angle/90
        be.updateTarget()
    }

    @LuaFunction
    fun resetAngle() {
        be.computerTarget = null
        be.updateTarget()
    }

    override fun detach(computer: IComputerAccess?) {
        be.computerTarget = null
        be.updateTarget()
    }

    override fun equals(p0: IPeripheral?): Boolean = be.blockPos == (p0 as? ValveDuctPeripheral)?.be?.blockPos
    override fun getType(): String = "cw_valve_duct"
}