@file:Suppress("unused")

package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.platform.api.ContraptionController
import org.valkyrienskies.mod.common.getShipManagingPos

class PhysBearingPeripheral(private val be: PhysBearingBlockEntity): IPeripheral {
    @LuaFunction fun assemble() = be.assemble()
    @LuaFunction fun disassemble() = be.disassemble()

    @LuaFunction fun setFollowAngleMode() {
        be.movementMode!!.setValue(ContraptionController.LockedMode.FOLLOW_ANGLE.ordinal)
    }
    @LuaFunction fun setUnlockedMode() {
        be.movementMode!!.setValue(ContraptionController.LockedMode.UNLOCKED.ordinal)
    }
    @LuaFunction fun setAngle(angle: Double) {
        be.setAngle(angle.toFloat())
    }

    @LuaFunction fun isBeingDisassembled() = be.aligning
    @LuaFunction fun isActive() = be.isRunning
    @LuaFunction fun isInFollowAngleMode() = be.movementMode!!.get() == ContraptionController.LockedMode.FOLLOW_ANGLE

    @LuaFunction fun getConnectedToShip() = be.level.getShipManagingPos(be.partnerPos ?: BlockPos(0, 0, 0))?.id ?: -1
    @LuaFunction fun getTargetAngle() = be.targetAngle
    @LuaFunction fun getActualAngle() = be.getActualAngle()

    @LuaFunction fun getRPM() = be.speed
    @LuaFunction fun getFacingDirection() = be.blockState.getValue(BlockStateProperties.FACING).getName()

    override fun equals(p0: IPeripheral?): Boolean = be.level?.getBlockState(be.blockPos)?.`is`(ClockworkBlocks.PHYS_BEARING.get()) == true
    override fun getType(): String = "cw_phys_bearing"
}