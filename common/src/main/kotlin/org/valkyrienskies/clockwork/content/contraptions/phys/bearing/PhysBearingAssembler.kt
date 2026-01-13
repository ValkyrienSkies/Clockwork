package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.ticks.ScheduledTick
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.inAssemblyBlacklist
import org.valkyrienskies.mod.common.util.toBlockPos

//TODO this is dumb but i'm not going to use ShipAssembler cuz it sucks
//Why does it suck? Mainly this because of this (https://github.com/ValkyrienSkies/Valkyrien-Skies-2/blob/93cc755c6325585ddf3fd90cfd414e8293474ecc/common/src/main/kotlin/org/valkyrienskies/mod/common/assembly/ShipAssembler.kt#L84)
// it gets worldspace position of the new ship, then transforms it back to shipspace, why???????????????????????
// doing some math to calculate center is simple and can be easily calculated outside of the function when you need it (and you do need it)
// you can't get the center pos without knowing what value positionInShip has
object PhysBearingAssembler {

    @JvmStatic
    fun copyBlock(level: Level, from: BlockPos, to: BlockPos) {
        val state = level.getBlockState(from)
        val blockentity = level.getBlockEntity(from)
        level.getChunk(to).setBlockState(to, state, false)

        // Transfer pending schedule-ticks
        if (level.blockTicks.hasScheduledTick(from, state.block)) {
            level.blockTicks.schedule(ScheduledTick<Block?>(state.block, to, 0, 0))
        }

        // Transfer block-entity data
        if (state.hasBlockEntity() && blockentity != null) {
            val data: CompoundTag = blockentity.saveWithId()
            level.setBlockEntity(blockentity)
            val newBlockentity = level.getBlockEntity(to)
            newBlockentity?.load(data)
        }
    }

    @JvmStatic
    fun moveBlocksFromTo(level: ServerLevel, blocks: DenseBlockPosSet, removeOriginal: Boolean, originCenter: BlockPos, toCenter: BlockPos): Boolean {
        val blocks = blocks.filter { level.getBlockState(it.toBlockPos()).let{!it.isAir && !it.inAssemblyBlacklist()} }.map {it.toBlockPos()}
        for (itPos in blocks) {
            val relative: BlockPos = itPos.subtract(BlockPos(originCenter.x, originCenter.y, originCenter.z))
            val shipPos: BlockPos = toCenter.offset(relative)
            if (!level.getBlockState(shipPos).isAir) {return false}
        }

        for (itPos in blocks) {
            val relative: BlockPos = itPos.subtract(BlockPos(originCenter.x, originCenter.y, originCenter.z))
            val shipPos: BlockPos = toCenter.offset(relative)
            copyBlock(level, itPos, shipPos)
        }

        if (removeOriginal) {
            for (itPos in blocks) {
                //AssemblyUtil.removeBlock has isMoving set to false which updates blocks on removal
                level.removeBlockEntity(itPos)
                level.getChunk(itPos).setBlockState(itPos, Blocks.AIR.defaultBlockState(), true)
            }
        }

        for (itPos in blocks) {
            val relative: BlockPos = itPos.subtract(BlockPos(originCenter.x, originCenter.y, originCenter.z))
            val shipPos: BlockPos = toCenter.offset(relative)
            level.chunkSource.blockChanged(shipPos)
        }

        return true
    }
}
