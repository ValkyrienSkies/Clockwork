package org.valkyrienskies.clockwork.integration.cc

import com.tterrag.registrate.util.entry.BlockEntry
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlocks

private inline fun <T: Block> c(arg1: BlockState, arg2: BlockEntry<T>) = arg1.`is`(arg2.get())

fun getPeripheralCommon(level: Level, blockPos: BlockPos, direction: Direction): IPeripheral? {
    level as ServerLevel
    val s = level.getBlockState(blockPos)

    return when {
        c(s, ClockworkBlocks.PHYS_BEARING) -> PhysBearingPeripheral(level, blockPos)
        else -> null
    }
}