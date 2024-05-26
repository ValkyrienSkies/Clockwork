package org.valkyrienskies.clockwork.integration.cc.peripheral

import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlockEntity
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD

class AltMeterPeripheral(val altmeter: AltMeterBlockEntity): IPeripheral {
    override fun equals(peripheral: IPeripheral?) = peripheral is AltMeterPeripheral && peripheral.altmeter == this.altmeter

    override fun getType() = "altimeter"

    @LuaFunction
    fun getAltitude(): Double {
        val posInWorld = this.altmeter.blockPos.toJOMLD().add(0.5, 0.5, 0.5)
        val shipOn = this.altmeter.level.getShipManagingPos(this.altmeter.blockPos)
        shipOn?.transform?.shipToWorld?.transformPosition(posInWorld)
        return posInWorld.y
    }

    @LuaFunction
    fun getTriggerAltitude() = this.altmeter.triggerHeight

    @LuaFunction
    fun setTriggerAltitude(altitude: Double) {
        this.altmeter.triggerHeight = altitude
    }

    override fun attach(computer: IComputerAccess) {
        this.altmeter.computerHandler.attachComputer(computer)
        super.attach(computer)
    }

    override fun detach(computer: IComputerAccess) {
        this.altmeter.computerHandler.detachComputer(computer)
        super.detach(computer)
    }
}