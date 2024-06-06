package org.valkyrienskies.clockwork.integration.cc.peripheral

import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import org.valkyrienskies.clockwork.content.forces.PropellerController
import org.valkyrienskies.mod.common.util.toJOMLD

class PropellerBearingPeripheral(val bearing: PropellerBearingBlockEntity, private val controller: PropellerController?): IPeripheral {
    override fun equals(peripheral: IPeripheral?) = peripheral is PropellerBearingPeripheral &&
            peripheral.bearing == this.bearing && peripheral.controller == this.controller

    override fun getType() = "propeller"

    @LuaFunction
    fun getDirection() = this.bearing.directonFromBlock.serializedName

    @LuaFunction
    fun isInverted() = this.bearing.isInverted

    @LuaFunction
    fun isRunning() = this.bearing.running

    @LuaFunction
    fun isSlowingDown() = this.bearing.slowingDown

    @LuaFunction
    fun isSpinningUp() = this.bearing.spinningUp

    @LuaFunction
    fun getRotSpeed() = this.bearing.rotspeed.toDouble()

    @LuaFunction
    fun getSpeed() = this.bearing.speed.toDouble()

    @LuaFunction
    fun isOverStressed() = this.bearing.isOverStressed

    @LuaFunction
    fun getSailCount() = this.bearing.sails

    @LuaFunction
    fun getSailPositions(): List<Map<String, Int>> {
        val list = mutableListOf<Map<String, Int>>()
        this.bearing.sailPositions.forEach {
            list.add(mapOf(
                Pair("x", it.x),
                Pair("y", it.y),
                Pair("z", it.z)
            ))
        }

        return list.toList()
    }

    fun getAirPressure(): Double {
        return this.controller?.airPressure(this.bearing.blockPos.toJOMLD()) ?: throw LuaException("Not on a Ship!")
    }

    @LuaFunction
    fun getExhaustVelocityWithOmega(xOmega: Double, yOmega: Double, zOmega: Double): Double =
        this.controller?.exhaustVelocity(this.bearing.blockPos.toJOMLD(),
            Vector3d(xOmega, yOmega, zOmega)) ?: throw LuaException("Not on a Ship!")

    override fun attach(computer: IComputerAccess) {
        this.bearing.computerHandler.attachComputer(computer)
        super.attach(computer)
    }

    override fun detach(computer: IComputerAccess) {
        this.bearing.computerHandler.detachComputer(computer)
        super.detach(computer)
    }
}