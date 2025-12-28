@file:Suppress("unused")

package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.platform.api.ContraptionController

class PhysBearingPeripheral(private val level: ServerLevel, private val bpos: BlockPos): IPeripheral {
    val be = level.getBlockEntity(bpos) as PhysBearingBlockEntity

    @LuaFunction fun assemble() {be.assembleNextTick = true}
    @LuaFunction fun disassemble() {be.disassemble()}

    @LuaFunction fun setLockedMode() {be.movementMode!!.setValue(ContraptionController.LockedMode.LOCKED.ordinal)}
    @LuaFunction fun setUnlockedMode() {be.movementMode!!.setValue(ContraptionController.LockedMode.UNLOCKED.ordinal)}
    @LuaFunction fun setAngle(angle: Double) {if (be.stopTargetAngleChange) {be.setAngle(angle.toFloat())} else {throw LuaException("Can't be changed until target angle change is stopped")}}

    @LuaFunction fun isBeingDisassembled() = be.disassembleWhenPossible
    @LuaFunction fun isActive() = be.isRunning
    @LuaFunction fun isInLockedMode() = be.movementMode!!.get() == ContraptionController.LockedMode.LOCKED
    @LuaFunction fun targetAngleIsFrozen() = be.stopTargetAngleChange

    @LuaFunction fun getConnectedToShip() = be.shiptraptionID
    @LuaFunction fun getTargetAngle() = be.targetAngle
    @LuaFunction fun getActualAngle() = be.getActualAngle()
    @LuaFunction fun getTargetAngleChangeSpeed() = be.getActualAngularSpeed()
    @LuaFunction fun getRPM() = be.speed
    @LuaFunction fun getFacingDirection() = be.blockState.getValue(BlockStateProperties.FACING).getName()

    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(bpos).`is`(ClockworkBlocks.PHYS_BEARING.get())
    override fun getType(): String = "cw_phys_bearing"
}