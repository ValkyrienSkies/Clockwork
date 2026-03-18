package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.clockwork.util.VS2AssemblyBridge
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.inAssemblyBlacklist
import org.valkyrienskies.mod.common.util.toBlockPos
import java.util.concurrent.CompletableFuture

//TODO should be in ShipAssembler itself
object PhysBearingAssembler {
    fun collectLoadedShipBlocks(level: ServerLevel, ship: ServerShip): List<BlockPos>? {
        val result = ArrayList<BlockPos>()
        var hasMissingChunk = false

        ship.activeChunksSet.forEach { chunkX, chunkZ ->
            val chunk = level.chunkSource.getChunkNow(chunkX, chunkZ)
            if (chunk == null) {
                hasMissingChunk = true
                return@forEach
            }

            for (sectionIndex in 0 until chunk.sections.size) {
                val section = chunk.sections[sectionIndex]
                if (section == null || section.hasOnlyAir()) continue

                val bottomY = (sectionIndex shl 4) + level.minBuildHeight

                for (x in 0..15) {
                    for (y in 0..15) {
                        for (z in 0..15) {
                            val state = section.getBlockState(x, y, z)
                            if (state.isAir || state.inAssemblyBlacklist()) continue

                            result.add(BlockPos((chunkX shl 4) + x, bottomY + y, (chunkZ shl 4) + z))
                        }
                    }
                }
            }
        }

        return if (hasMissingChunk) null else result
    }

    private fun getLoadedBlockState(level: ServerLevel, pos: BlockPos) =
        level.chunkSource.getChunkNow(pos.x shr 4, pos.z shr 4)?.getBlockState(pos)

    private fun prepareBlocks(
        level: ServerLevel,
        blocks: Collection<BlockPos>,
        originCenter: BlockPos,
        toCenter: BlockPos,
        requireLoadedSources: Boolean
    ): List<BlockPos>? {
        val filteredBlocks = ArrayList<BlockPos>(blocks.size)

        for (sourcePos in blocks) {
            val sourceState = getLoadedBlockState(level, sourcePos)
                ?: if (requireLoadedSources) return null else continue

            if (sourceState.isAir || sourceState.inAssemblyBlacklist()) continue

            val relative = sourcePos.subtract(originCenter)
            val destPos = toCenter.offset(relative)
            val destState = getLoadedBlockState(level, destPos) ?: return null
            if (!destState.isAir) return emptyList()

            filteredBlocks.add(sourcePos)
        }

        return filteredBlocks
    }

    @JvmStatic
    fun moveBlocksFromTo(level: ServerLevel, blocks: DenseBlockPosSet, removeOriginal: Boolean, originCenter: BlockPos, toCenter: BlockPos, originShip: ServerShip?, toShip: ServerShip?): Boolean {
        val filteredBlocks = prepareBlocks(
            level,
            blocks.map { it.toBlockPos() },
            originCenter,
            toCenter,
            requireLoadedSources = true
        ) ?: return false

        if (filteredBlocks.isEmpty()) return false

        val result = VS2AssemblyBridge.moveBlocksFromTo(
            level,
            filteredBlocks,
            removeOriginal,
            originCenter,
            toCenter,
            originShip,
            toShip
        )
        return result.wasSuccessful
    }

    @JvmStatic
    fun queueMoveBlocksFromTo(level: ServerLevel, blocks: Collection<BlockPos>, removeOriginal: Boolean, originCenter: BlockPos, toCenter: BlockPos, originShip: ServerShip?, toShip: ServerShip?): CompletableFuture<Boolean> {
        val filteredBlocks = prepareBlocks(
            level,
            blocks,
            originCenter,
            toCenter,
            requireLoadedSources = true
        ) ?: return CompletableFuture.completedFuture(false)

        if (filteredBlocks.isEmpty()) {
            return CompletableFuture.completedFuture(false)
        }

        return VS2AssemblyBridge.queueMoveBlocksFromTo(
            level,
            filteredBlocks,
            removeOriginal,
            originCenter,
            toCenter,
            originShip,
            toShip
        ).thenApply { it.wasSuccessful }
    }

    @JvmStatic
    fun queueMoveBlocksFromTo(level: ServerLevel, blocks: DenseBlockPosSet, removeOriginal: Boolean, originCenter: BlockPos, toCenter: BlockPos, originShip: ServerShip?, toShip: ServerShip?): CompletableFuture<Boolean> {
        return queueMoveBlocksFromTo(
            level,
            blocks.map { it.toBlockPos() },
            removeOriginal,
            originCenter,
            toCenter,
            originShip,
            toShip
        )
    }
}
