package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import org.valkyrienskies.clockwork.platform.integration.cc.ComputerCraftUtils

object ClockworkComputerCraftIntegration {
    /**
     * This method exists solely to keep CC classes and imports out of any non-CC classes
     */
    fun integrate() {
        ComputerCraftAPI.registerPeripheralProvider(ComputerCraftUtils.getClockworkPeripheralProvider())
    }

    fun getPerpheral(level: Level, pos: BlockPos, direction: Direction): IPeripheral? {
        val be = level.getBlockEntity(pos)
        return null
    }
}