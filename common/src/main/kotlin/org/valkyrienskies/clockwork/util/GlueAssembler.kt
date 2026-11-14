package org.valkyrienskies.clockwork.util

import com.simibubi.create.api.contraption.BlockMovementChecks
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.StructureTransform
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity
import net.createmod.catnip.data.UniqueLinkedList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import java.util.*

object GlueAssembler {
    // TODO: Config this
    private const val MAX_BLOCKS = 100000

    fun prepareMove(level: ServerLevel, min: BlockPos, max: BlockPos): List<SuperGlueEntity> {
        val bounds = SuperGlueEntity.span(min, max)
        val boxes = HashSet<AABB>()
        val glue = level.allEntities.filterIsInstance<SuperGlueEntity>()
            .filter { it.isAlive && bounds.intersects(it.boundingBox) }
        return glue.filter {
            if (boxes.add(it.boundingBox)) true else {
                it.discard()
                false
            }
        }
    }

    @JvmStatic
    fun moveRegion(level: ServerLevel, min: BlockPos, max: BlockPos, destination: BlockPos) {
        transferGlue(level, prepareMove(level, min, max), min, destination)
    }

    fun transferGlue(level: ServerLevel, glue: List<SuperGlueEntity>, from: BlockPos, to: BlockPos,
                     transforms: List<StructureTransform> = emptyList(), removeOriginal: Boolean = true) {
        if (glue.isEmpty()) return
        fun axis(direction: Direction) = Vec3.atLowerCornerOf(transforms.fold(direction) { value, step ->
            step.rotateFacing(step.mirrorFacing(value))
        }.normal)
        val x = axis(Direction.EAST)
        val y = axis(Direction.UP)
        val z = axis(Direction.SOUTH)
        fun transform(point: Vec3): Vec3 {
            val relative = point.subtract(from.center)
            return to.center.add(x.scale(relative.x)).add(y.scale(relative.y)).add(z.scale(relative.z))
        }
        val existing = level.allEntities.filterIsInstance<SuperGlueEntity>().filter { it.isAlive }
            .groupBy { it.boundingBox }.toMutableMap()
        for (entity in glue) {
            val box = entity.boundingBox
            val movedBox = AABB(transform(Vec3(box.minX, box.minY, box.minZ)), transform(Vec3(box.maxX, box.maxY, box.maxZ)))
            if (removeOriginal && isEmpty(level, box)) entity.discard()
            val copies = existing[movedBox].orEmpty().filter { it.isAlive }
            if (copies.isEmpty()) {
                val moved = SuperGlueEntity(level, movedBox)
                level.addFreshEntity(moved)
                existing[movedBox] = listOf(moved)
            } else copies.drop(1).forEach { it.discard() }
        }
    }

    fun removeEmpty(level: ServerLevel, glue: List<SuperGlueEntity>) {
        glue.filter { it.isAlive && isEmpty(level, it.boundingBox) }.forEach { it.discard() }
    }

    private fun isEmpty(level: ServerLevel, box: AABB): Boolean {
        val bounds = box.deflate(1.0e-4)
        val min = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ)
        val max = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ)
        return level.hasChunksAt(min, max) && BlockPos.betweenClosed(min, max).all(level::isEmptyBlock)
    }

    @Throws(AssemblyException::class)
    fun collectGlued(
        level: Level,
        startPos: BlockPos,
        canCollect: (BlockPos, BlockState) -> Boolean = { _, _ -> true },
    ): DenseBlockPosSet? {
        if (level.isOutsideBuildHeight(startPos) || !level.isLoaded(startPos)) return null
        val start = level.getBlockState(startPos)
        if (start.isAir || !canCollect(startPos, start)) return null

        val visited = DenseBlockPosSet()
        val frontier: Queue<BlockPos> = UniqueLinkedList()
        val glueCache: MutableSet<SuperGlueEntity> = HashSet()

        frontier.add(startPos)

        var processed = 0
        while (frontier.isNotEmpty()) {
            if (++processed > MAX_BLOCKS) throw AssemblyException.structureTooLarge()
            visitBlock(level, frontier, visited, glueCache, canCollect)
        }

        if (visited.isEmpty()) throw AssemblyException(Component.literal("No blocks found!"))
        return visited
    }

    @Throws(AssemblyException::class)
    private fun visitBlock(
        level: Level,
        frontier: Queue<BlockPos>,
        visited: DenseBlockPosSet,
        glueCache: MutableSet<SuperGlueEntity>,
        canCollect: (BlockPos, BlockState) -> Boolean,
    ) {
        val pos = frontier.poll()!!
        if (!visited.add(pos.x, pos.y, pos.z)) return

        val state = level.getBlockState(pos)

        for (direction in Direction.entries) {
            val newPos = pos.relative(direction)

            if (visited.contains(newPos.x, newPos.y, newPos.z)) continue
            if (level.isOutsideBuildHeight(newPos)) continue
            if (!level.isLoaded(newPos)) throw AssemblyException.unloadedChunk(newPos)

            val newState = level.getBlockState(newPos)
            if (newState.isAir) continue
            if (!isAllowed(newState) || !canCollect(newPos, newState)) continue

            val glued = SuperGlueEntity.isGlued(level, pos, direction, glueCache)

            val sticky = isStuckTogether(level, pos, state, direction, newPos, newState)

            // Torches, buttons, carpets, etc get glued automatically
            val attached = BlockMovementChecks.isBlockAttachedTowards(
                newState, level, newPos, direction.opposite
            )

            if (glued || sticky || attached) frontier.add(newPos)
        }
    }

    private fun isStuckTogether(
        level: Level,
        pos: BlockPos,
        state: BlockState,
        direction: Direction,
        newPos: BlockPos,
        newState: BlockState,
    ): Boolean {
        if (BlockMovementChecks.isBrittle(newState)) return false

        // Either side might be sticky
        val sticky = SuperGlueEntity.isSideSticky(level, pos, direction) ||
                SuperGlueEntity.isSideSticky(level, newPos, direction.opposite)
        if (!sticky) return false

        // Slime / honey but not both
        val slimeHoney =
            (state.`is`(Blocks.SLIME_BLOCK) && newState.`is`(Blocks.HONEY_BLOCK)) ||
                    (state.`is`(Blocks.HONEY_BLOCK) && newState.`is`(Blocks.SLIME_BLOCK))
        if (slimeHoney) return false

        if (state.pistonPushReaction == PushReaction.PUSH_ONLY ||
            newState.pistonPushReaction == PushReaction.PUSH_ONLY
        ) return false

        return true
    }

    private fun isAllowed(state: BlockState): Boolean {
        val key = BuiltInRegistries.BLOCK.getKey(state.block).toString()
        return !ClockworkConfig.SERVER.blockBlacklist.contains(key)
    }
}
