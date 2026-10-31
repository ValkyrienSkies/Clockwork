package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import net.minecraft.core.BlockPos
import com.simibubi.create.content.contraptions.StructureTransform
import org.valkyrienskies.mod.util.updateBlock
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.ticks.ScheduledTick
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.assembly.ICopyableBlock
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.assembly.VSAssemblyEvents
import org.valkyrienskies.mod.common.inAssemblyBlacklist
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOMLD

//TODO should be in ShipAssembler itself
object PhysBearingAssembler {
    @JvmStatic
    @JvmOverloads
    fun copyBlock(level: ServerLevel, from: BlockPos, to: BlockPos, originShip: ServerShip?, toShip: ServerShip?, centerPositions: Pair<Vector3dc, Vector3dc>, transforms: List<StructureTransform> = emptyList()) {
        val state = level.getBlockState(from)
        val block = state.block
        val be = level.getBlockEntity(from)

        var tag = (if (block is ICopyableBlock) block.onCopy(level, from, state, be,
            mutableListOf<ServerShip>().also { if (originShip != null) it.add(originShip) }.also { if (toShip != null) it.add(toShip) },
            ShipAssembler.SingleItemMap(originShip?.id ?: -1L, centerPositions.first.get(Vector3d()), Vector3d()) //return actual center position only for origin ship
        ) else null) ?: be?.saveWithId()
        val rotatedState = transforms.fold(state) { current, transform -> transform.apply(current) }
        level.getChunk(to).setBlockState(to, rotatedState, false)
        tag = (if (block is ICopyableBlock) block.onPaste(level, to, rotatedState,
            ShipAssembler.SingleItemMap(originShip?.id ?: -1L, toShip?.id ?: -1L, -1L) {it}, // return actual change (originShip -> toShip), otherwise identity
            ShipAssembler.SingleItemMap(originShip?.id ?: -1L, Vector3d(centerPositions.first) to Vector3d(centerPositions.second), Vector3d() to Vector3d()),
            tag
        ) else tag) ?: tag

        // Transfer pending schedule-ticks
        if (level.blockTicks.hasScheduledTick(from, state.block)) {
            level.blockTicks.schedule(ScheduledTick<Block?>(state.block, to, 0, 0))
        }

        // Transfer block-entity data
        if (state.hasBlockEntity() && be != null && tag != null) {
            val newBlockentity = checkNotNull(level.getBlockEntity(to)) { "Missing destination block entity at $to" }
            tag.putInt("x", to.x); tag.putInt("y", to.y); tag.putInt("z", to.z)
            newBlockentity.load(tag)
            transforms.forEach { it.apply(newBlockentity) }
        }
    }

    /** Rotated welds use the same copy/paste hooks and assembly events as other ship moves. */
    fun moveBlocksWithTransform(level: ServerLevel, moves: Map<BlockPos, BlockPos>, originShip: ServerShip,
                               toShip: ServerShip?, origin: BlockPos, destination: BlockPos,
                               transforms: List<StructureTransform>): Boolean {
        if (moves.isEmpty() || moves.values.toSet().size != moves.size || moves.values.any { !level.getBlockState(it).isAir }) return false
        val centers = origin.toJOMLD().add(0.5, 0.5, 0.5) to destination.toJOMLD().add(0.5, 0.5, 0.5)
        val eventData = mutableMapOf<String, CompoundTag>()
        val (min, max) = ShipAssembler.findMinAndMax(moves.keys)
        VSAssemblyEvents.beforeCopy.emit(VSAssemblyEvents.BeforeCopy(level, min.toJOMLD(), max.toJOMLD(),
            centers.first, originShip, moves.keys, eventData))
        VSAssemblyEvents.onPasteBeforeBlocksAreLoaded.emit(VSAssemblyEvents.OnPasteBeforeBlocksAreLoaded(
            level, originShip, toShip, centers, eventData))
        try {
            // Keep every original intact until all destination block entities have been copied successfully.
            for ((from, to) in moves) copyBlock(level, from, to, originShip, toShip, centers, transforms)
        } catch (exception: Exception) {
            org.valkyrienskies.clockwork.ClockworkMod.LOGGER.error("Unable to copy blocks for a weld", exception)
            for (to in moves.values) {
                level.removeBlockEntity(to)
                level.getChunk(to).setBlockState(to, Blocks.AIR.defaultBlockState(), false)
                level.chunkSource.blockChanged(to)
            }
            return false
        }
        for (from in moves.keys) {
            level.removeBlockEntity(from)
            level.getChunk(from).setBlockState(from, Blocks.AIR.defaultBlockState(), false)
        }
        for ((from, to) in moves) {
            updateBlock(level, from, to, level.getBlockState(to))
            level.chunkSource.blockChanged(from)
            level.chunkSource.blockChanged(to)
        }
        VSAssemblyEvents.onPasteAfterBlocksAreLoaded.emit(VSAssemblyEvents.OnPasteAfterBlocksAreLoaded(
            level, originShip, toShip, centers, eventData))
        return true
    }

    @JvmStatic
    fun moveBlocksFromTo(level: ServerLevel, blocks: DenseBlockPosSet, removeOriginal: Boolean, originCenter: BlockPos, toCenter: BlockPos, originShip: ServerShip?, toShip: ServerShip?): Boolean {
        val blocks = blocks.filter { level.getBlockState(it.toBlockPos()).let{!it.isAir && !it.inAssemblyBlacklist()} }.map {it.toBlockPos()}
        if (blocks.isEmpty()) return false
        for (itPos in blocks) {
            val relative: BlockPos = itPos.subtract(BlockPos(originCenter.x, originCenter.y, originCenter.z))
            val shipPos: BlockPos = toCenter.offset(relative)
            if (!level.getBlockState(shipPos).isAir) {return false}
        }

        val eventData = mutableMapOf<String, CompoundTag>()

        val (minB, maxB) = ShipAssembler.findMinAndMax(blocks)
        val oldMin = minB.toJOMLD()
        val oldMax = maxB.toJOMLD()
        val oldCenter = originCenter.toJOMLD()
        val newCenter = toCenter.toJOMLD()

        VSAssemblyEvents.beforeCopy.emit(VSAssemblyEvents.BeforeCopy(level, oldMin, oldMax, originCenter.toJOMLD(), originShip, blocks.toSet(), eventData))
        //TODO
//        VSAssemblyEvents.onPasteBeforeBlocksAreLoaded.emit(VSAssemblyEvents.OnPasteBeforeBlocksAreLoaded(level, originShip, toShip, oldCenter to newCenter, eventData))

        for (itPos in blocks) {
            val relative: BlockPos = itPos.subtract(BlockPos(originCenter.x, originCenter.y, originCenter.z))
            val shipPos: BlockPos = toCenter.offset(relative)
            copyBlock(level, itPos, shipPos, originShip, toShip, originCenter.toJOMLD() to toCenter.toJOMLD())
        }

        if (removeOriginal) {
            for (itPos in blocks) {
                level.removeBlockEntity(itPos)
                level.getChunk(itPos).setBlockState(itPos, Blocks.AIR.defaultBlockState(), true)
            }
        }

        for (itPos in blocks) {
            val relative: BlockPos = itPos.subtract(BlockPos(originCenter.x, originCenter.y, originCenter.z))
            val shipPos: BlockPos = toCenter.offset(relative)
            level.chunkSource.blockChanged(shipPos)
        }

//        VSAssemblyEvents.onPasteAfterBlocksAreLoaded.emit(VSAssemblyEvents.OnPasteAfterBlocksAreLoaded(level, originShip, toShip, oldCenter to newCenter, eventData))

        return true
    }
}
