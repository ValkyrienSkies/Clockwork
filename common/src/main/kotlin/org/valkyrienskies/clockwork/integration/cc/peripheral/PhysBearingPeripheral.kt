package org.valkyrienskies.clockwork.integration.cc.peripheral

import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingData
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingUpdateData
import org.valkyrienskies.clockwork.content.forces.contraption.BearingController
import org.valkyrienskies.clockwork.platform.api.ContraptionController
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

class PhysBearingPeripheral(val bearing: PhysBearingBlockEntity): IPeripheral {
    override fun equals(peripheral: IPeripheral?): Boolean = peripheral is PhysBearingPeripheral && peripheral.bearing == this.bearing

    override fun getType() = "phys_bearing"

    @LuaFunction
    fun getAngle() = this.bearing.getAngle().toDouble()

    @LuaFunction
    fun setAngle(angle: Double) {
        if (this.bearing.movementMode?.get() == ContraptionController.LockedMode.LOCKED)
            throw LuaException("Cannot set angle, Phys Bearing is locked!")
        val ship = (this.bearing.level as ServerLevel).shipObjectWorld.loadedShips.getById(this.bearing.connectedShip?.id
            ?: throw LuaException("Has no connected Ship!")) ?: throw LuaException("Connected Ship does not exist? How did you do this?")
        val control = BearingController.getOrCreate(ship)!!
        val prevData = control.bearingData[this.bearing.bearingID]
        val data = PhysBearingUpdateData(angle, 0f, false, prevData?.hingeConstraint, prevData?.angleConstraint)
        control.updatePhysBearing(this.bearing.bearingID!!, data)
        this.bearing.bearingAngle = angle.toFloat()
    }

    @LuaFunction
    fun getSpeed() = this.bearing.speed.toDouble()

    @LuaFunction
    fun getMode() = this.bearing.movementMode?.get()?.name

    @LuaFunction
    fun setMode(locked: Boolean) {
        this.bearing.movementMode?.setValue(
            if (locked) ContraptionController.LockedMode.LOCKED.ordinal else ContraptionController.LockedMode.UNLOCKED.ordinal
        )
    }
}