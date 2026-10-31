package org.valkyrienskies.clockwork.content.forces

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import org.joml.Vector3d
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandItem.Companion.toAABBic
import org.valkyrienskies.clockwork.content.forces.BalloonController.Companion.isValidBalloonEnclosure
import org.valkyrienskies.clockwork.util.AABBHelper.mergeAdjacentFast
import java.util.PriorityQueue

/** Cached membership and boundary geometry. */
internal class BalloonGeometry(regions: List<AABBic>) {
    val cells = HashSet<BlockPos>()
    val boundary = HashSet<BlockPos>()
    val enclosure = HashSet<BlockPos>()
    val center = Vector3d()

    init {
        for (box in regions) for (x in box.minX() until box.maxX())
            for (y in box.minY() until box.maxY()) for (z in box.minZ() until box.maxZ()) {
                cells.add(BlockPos(x, y, z))
            }
        for (pos in cells) {
            center.add(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
            for (direction in Direction.values()) {
                val neighbor = pos.relative(direction)
                if (neighbor !in cells) {
                    boundary.add(neighbor)
                    if (direction != Direction.DOWN) enclosure.add(neighbor)
                }
            }
        }
        if (cells.isNotEmpty()) center.div(cells.size.toDouble())
    }

    fun airComponents(level: Level): List<Set<BlockPos>>? {
        if (cells.any { !level.isLoaded(it) }) return null
        val remaining = cells.filterTo(HashSet()) { !level.getBlockState(it).isValidBalloonEnclosure(level, it) }
        val result = ArrayList<Set<BlockPos>>()
        while (remaining.isNotEmpty()) {
            val queue = ArrayDeque<BlockPos>()
            val component = HashSet<BlockPos>()
            val seed = remaining.first()
            remaining.remove(seed)
            queue.add(seed)
            while (queue.isNotEmpty()) {
                val pos = queue.removeFirst()
                component.add(pos)
                for (direction in Direction.values()) {
                    val neighbor = pos.relative(direction)
                    if (remaining.remove(neighbor)) queue.add(neighbor)
                }
            }
            result.add(component)
        }
        return result.sortedByDescending { it.size }
    }

    companion object {
        fun regions(cells: Collection<BlockPos>): List<AABBic> = mergeAdjacentFast(cells.map { it.toAABBic() })

        private data class Visit(val pos: BlockPos, val floor: Int)

        /** The highest escape path sets the open bottom. */
        fun fill(shell: BalloonController.ShellInfo, seed: BlockPos, level: Level, limit: Int, onUnloaded: (BlockPos) -> Unit = {}): List<AABBic> {
            if (limit < 1) return emptyList()
            if (!level.isLoaded(seed)) { onUnloaded(seed); return emptyList() }
            if (level.getBlockState(seed).isValidBalloonEnclosure(level, seed)) return emptyList()
            if (seed.x !in shell.minX..shell.maxX || seed.z !in shell.minZ..shell.maxZ || seed.y !in shell.minY..shell.maxY) return emptyList()
            val queue = PriorityQueue<Visit>(compareByDescending { it.floor })
            val best = HashMap<BlockPos, Int>()
            val scanned = HashSet<BlockPos>()
            val retained = HashSet<BlockPos>()
            queue.add(Visit(seed, seed.y))
            best[seed] = seed.y
            while (queue.isNotEmpty()) {
                val (pos, floor) = queue.remove()
                if (!scanned.add(pos)) continue
                if (pos.x !in shell.minX..shell.maxX || pos.z !in shell.minZ..shell.maxZ || pos.y !in shell.minY..shell.maxY) {
                    return regions(retained.filter { best.getValue(it) > floor })
                }
                if (retained.size >= limit) return emptyList()
                retained.add(pos)
                for (direction in Direction.values()) {
                    val next = pos.relative(direction)
                    if (next in scanned) continue
                    if (!level.isLoaded(next)) { onUnloaded(next); return emptyList() }
                    if (level.getBlockState(next).isValidBalloonEnclosure(level, next)) continue
                    val nextFloor = minOf(floor, next.y)
                    if (nextFloor > (best[next] ?: Int.MIN_VALUE)) {
                        best[next] = nextFloor
                        queue.add(Visit(next, nextFloor))
                    }
                }
            }
            return regions(retained)
        }
    }
}
