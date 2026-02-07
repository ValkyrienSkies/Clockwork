@file:Suppress("unused")

package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import java.util.*

class PropellerBearingPeripheral(private val be: PropellerBearingBlockEntity): IPeripheral {

    @LuaFunction
    fun isBrass(): Boolean = be.blockState.block == ClockworkBlocks.BRASS_PROPELLER_BEARING

    @LuaFunction
    fun getAngle(): Float = be.getInterpolatedAngle(0.0f)

    @LuaFunction
    fun getBladeAngle(): Double = be.blades.first().angle

    // Note: CC does not accept Float arguments, only Doubles.
    // CC also doesn't understand kotlin optional parameters,
    // so we use a Java Optional instead.
    @LuaFunction
    fun setBladeAngle(angle: Double, setLocked: Optional<Boolean>) {
        if (be.blockState.block == ClockworkBlocks.BRASS_PROPELLER_BEARING) {
            throw LuaException("setBladeAngle can only be used on a brass propeller bearing")
        }

        if (!be.running) {
            throw LuaException("Propeller bearing is not assembled")
        }

        if ((angle < -180) || (angle > 180)) {
            throw LuaException("Angle must be within range -180..180")
        }

        if (setLocked.isPresent) {
            be.isLocked = setLocked.get()
        } else {
            be.isLocked = true
        }

        be.setNewBladeAngle(angle)
        be.blades.clear()
        be.getBlades()
    }

    @LuaFunction
    fun isLocked(): Boolean = be.isLocked

    @LuaFunction
    fun setLocked(locked: Boolean) {
        be.isLocked = locked
    }

    @LuaFunction(mainThread = true)
    fun isRunning(): Boolean = be.running

    @LuaFunction(mainThread = true)
    fun assemble(): Boolean {
        if (be.running) return false
        be.assemble()
        return true
    }

    @LuaFunction(mainThread = true)
    fun disassemble(): Boolean {
        if (!be.running) return false
        be.disassemble()
        return true
    }

    override fun equals(p0: IPeripheral?): Boolean = be.level?.getBlockState(be.blockPos)?.`is`(ClockworkBlocks.BRASS_PROPELLER_BEARING.get()) == true
    override fun getType(): String = "cw_prop_bearing"
}