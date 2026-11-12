package org.valkyrienskies.clockwork.content.curiosities

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.Level
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.inAssemblyBlacklist
import org.valkyrienskies.mod.common.isBlockInShipyard

interface IWanderliteBlock {
    fun activate(level: Level, pos: BlockPos): InteractionResult {
        if (level.isBlockInShipyard(pos) || !canAssemble(level, pos)) {
            return InteractionResult.PASS
        }
        if (level is ServerLevel) {
            val positions = linkedSetOf(pos)
            var frontier = listOf(pos)
            repeat(3) {
                val next = mutableListOf<BlockPos>()
                for (current in frontier) {
                    for (direction in Direction.entries) {
                        val neighbor = current.relative(direction)
                        if (neighbor !in positions && canAssemble(level, neighbor)) {
                            positions.add(neighbor)
                            next.add(neighbor)
                        }
                    }
                }
                frontier = next
            }
            ShipAssembler.assembleToShip(level, positions, 1.0)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    private fun canAssemble(level: Level, pos: BlockPos): Boolean {
        if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) return false
        val state = level.getBlockState(pos)
        return state.block is IWanderliteBlock && !state.inAssemblyBlacklist()
    }
}
