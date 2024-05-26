package org.valkyrienskies.clockwork.integration.cc.peripheral

import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.platform.api.ContraptionController

class PhysBearingPeripheral(val bearing: PhysBearingBlockEntity): IPeripheral {
    override fun equals(peripheral: IPeripheral?): Boolean = peripheral is PhysBearingPeripheral && peripheral.bearing == this.bearing

    override fun getType() = "phys_bearing"

    @LuaFunction
    fun getAngle() = this.bearing.getAngle().toDouble()

    @LuaFunction
    fun setAngle(angle: Double) {
        this.bearing.setAngle(angle.toFloat())
    }

    @LuaFunction
    fun getSpeed() = this.bearing.speed.toDouble()

    @LuaFunction
    fun setSpeed(speed: Double) {
        this.bearing.speed = speed.toFloat()
    }

    @LuaFunction
    fun getMode() = this.bearing.movementMode?.get()?.name

    @LuaFunction
    fun setMode(locked: Boolean) {
        this.bearing.movementMode?.setValue(
            if (locked) ContraptionController.LockedMode.LOCKED.ordinal else ContraptionController.LockedMode.UNLOCKED.ordinal
        )
    }
}