package org.valkyrienskies.clockwork.integration.cc.peripheral

import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import org.valkyrienskies.clockwork.content.forces.PropellerController

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

    @LuaFunction
    fun getAirPressureAtPosition(args: IArguments): Double {
        return this.controller?.airPressure(Vector3d(
            args.getDouble(0),
            args.getDouble(1),
            args.getDouble(2)
        )) ?: 0.0
    }

    @LuaFunction
    fun getExhaustVelocityAtPosition(args: IArguments): Double {
        return this.controller?.exhaustVelocity(
            Vector3d(
                args.getDouble(0),
                args.getDouble(1),
                args.getDouble(2)
            ),
            Vector3d(
                args.getDouble(3),
                args.getDouble(4),
                args.getDouble(5)
            )
        ) ?: 0.0
    }

    override fun attach(computer: IComputerAccess) {
        this.bearing.computerHandler.attachComputer(computer)
        super.attach(computer)
    }

    override fun detach(computer: IComputerAccess) {
        this.bearing.computerHandler.detachComputer(computer)
        super.detach(computer)
    }
}