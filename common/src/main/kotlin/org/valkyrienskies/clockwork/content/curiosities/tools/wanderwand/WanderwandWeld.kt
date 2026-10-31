package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.joml.Quaterniond
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingAssembler
import org.valkyrienskies.mod.common.inAssemblyBlacklist
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

object WanderwandWeld {
    fun weld(level: ServerLevel, from: WandAnchor, to: WandAnchor): String? {
        if (from.shipId < 0 || from.shipId == to.shipId) return "Choose the world or a different ship."
        val source = level.shipObjectWorld.loadedShips.getById(from.shipId) ?: return "The source ship is not loaded."
        val target = if (to.shipId >= 0) level.shipObjectWorld.loadedShips.getById(to.shipId)
            ?: return "The target ship is not loaded." else null
        if (level.getBlockState(from.pos).isAir) return "The selected source block is gone."
        if (kotlin.math.abs(source.transform.shipToWorldScaling.x() - (target?.transform?.shipToWorldScaling?.x() ?: 1.0)) > 0.001)
            return "Ships must have the same scale to weld their blocks."
        val rotation = WandGridRotation.choose(from.face, to.face, source.transform.shipToWorldRotation,
            target?.transform?.shipToWorldRotation ?: Quaterniond())
        val blocks = linkedSetOf<BlockPos>()
        var unloaded = false
        source.activeChunksSet.forEach { cx, cz ->
            if (!level.hasChunk(cx, cz)) { unloaded = true; return@forEach }
            val chunk = level.getChunk(cx, cz)
            for ((index, section) in chunk.sections.withIndex()) {
                if (section.hasOnlyAir()) continue
                for (x in 0..15) for (y in 0..15) for (z in 0..15) {
                    if (!section.getBlockState(x, y, z).isAir && blocks.size <= 100000)
                        blocks.add(BlockPos(cx * 16 + x, level.minBuildHeight + index * 16 + y, cz * 16 + z))
                }
            }
        }
        if (unloaded) return "Load the whole source ship before welding."
        if (blocks.size > 100000) return "Welds are limited to 100,000 blocks."
        if (blocks.isEmpty()) return "There are no blocks to weld."
        if (blocks.any { level.getBlockState(it).inAssemblyBlacklist() }) return "The ship contains a block that cannot be moved."
        val destination = to.pos.relative(to.face)
        val moves = blocks.associateWith { destination.offset(rotation.offset(it.subtract(from.pos))) }
        if (moves.values.any { level.getShipManagingPos(it)?.id != target?.id })
            return "The welded ship would extend outside the destination's build area."
        if (moves.values.any { it.y !in level.minBuildHeight until level.maxBuildHeight ||
                !level.worldBorder.isWithinBounds(if (target == null) it else to.world(level)!!.let(BlockPos::containing)) })
            return "The weld would extend outside the world."
        if (moves.values.any { !level.hasChunkAt(it) }) return "Load the destination area before welding."
        if (moves.values.any { !level.getBlockState(it).isAir }) return "The weld would overlap existing blocks."
        val success = PhysBearingAssembler.moveBlocksWithTransform(level, moves, source, target,
            from.pos, destination, rotation.steps)
        if (!success) return "The weld could not be completed; the source blocks were preserved."
        // End constraints to the consumed body instead of leaving invisible, orphaned anchors.
        val data = WandLinks.get(level)
        data.links.values.filter { it.a.shipId == source.id || it.b.shipId == source.id }.toList().forEach {
            data.remove(level, it); WanderwandServer.breakEffect(level, it)
        }
        return null
    }
}
