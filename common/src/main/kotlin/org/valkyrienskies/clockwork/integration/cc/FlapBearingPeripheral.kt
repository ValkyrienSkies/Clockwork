@file:Suppress("unused")

package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import net.createmod.catnip.animation.LerpedFloat
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.flap.smart_flap.SmartFlapBearingBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle.GasNozzleBlockEntity
import java.util.Optional

class FlapBearingPeripheral(private val be: FlapBearingBlockEntity): IPeripheral {

    @LuaFunction
    fun isSmart(): Boolean = be is SmartFlapBearingBlockEntity

    @LuaFunction
    fun getAngle(): Float = be.getInterpolatedAngle(0.0f)

    // Note: CC does not accept Float arguments, only Doubles.
    // CC also doesn't understand kotlin optional parameters,
    // so we use a Java Optional instead.
    @LuaFunction
    fun setAngle(angle: Double, setLocked: Optional<Boolean>) {
        if (be !is SmartFlapBearingBlockEntity) {
            throw LuaException("setAngle can only be used on a smart flap bearing")
        }

        if (!(be.angularSpeed > 0) && !ClockworkConfig.SERVER.cheatFlapBearingPeripheral) {
            throw LuaException("Flap bearing is not connected to power")
        }

        if (!be.isRunning) {
            throw LuaException("Flap bearing is not assembled")
        }

        if ((angle < -22.5) || (angle > 22.5)) {
            throw LuaException("Angle must be within range -22.5..22.5")
        }

        if (setLocked.isPresent) {
            be.isLocked = setLocked.get()
        } else {
            be.isLocked = true
        }

        be.setAngle(angle.toFloat())
    }

    @LuaFunction
    fun isLocked(): Boolean = be.isLocked

    @LuaFunction
    fun setLocked(locked: Boolean) {
        be.isLocked = locked
    }

    @LuaFunction(mainThread = true)
    fun isRunning(): Boolean = be.isRunning

    @LuaFunction(mainThread = true)
    fun assemble(): Boolean {
        if (be.isRunning) return false
        be.assemble()
        return true
    }

    @LuaFunction(mainThread = true)
    fun disassemble(): Boolean {
        if (!be.isRunning) return false
        be.disassemble()
        return true
    }

    override fun equals(p0: IPeripheral?): Boolean = be.blockPos == (p0 as? FlapBearingPeripheral)?.be?.blockPos
    override fun getType(): String = "cw_flap_bearing"
}