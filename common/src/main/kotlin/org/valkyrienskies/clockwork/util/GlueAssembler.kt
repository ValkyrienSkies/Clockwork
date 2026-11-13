package org.valkyrienskies.clockwork.util

import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.StructureTransform
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity
import net.createmod.catnip.data.UniqueLinkedList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import java.util.*
import java.util.function.Consumer

object GlueAssembler {
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
        pos: BlockPos,
        canCollect: (BlockPos, BlockState) -> Boolean = { _, _ -> true },
    ): DenseBlockPosSet? {
        val toRemove: Set<Entity> = HashSet()
        val result = DenseBlockPosSet()
        val frontier: Queue<BlockPos> = UniqueLinkedList()
        val start = level.getBlockState(pos)
        if (start.isAir || !canCollect(pos, start)) return null
        frontier.add(pos)
        for (limit in 100000 downTo 1) {
            if (frontier.isEmpty()) {
                if (result.isEmpty()) throw AssemblyException(Component.literal("No blocks found!"))
                return result
            }
            visitBlock(level, frontier, result, toRemove, canCollect)
        }
        toRemove.forEach(Consumer { obj: Entity -> obj.discard() })
        throw AssemblyException.structureTooLarge()
    }

    @Throws(AssemblyException::class)
    private fun visitBlock(
        level: LevelAccessor,
        frontier: Queue<BlockPos>,
        visited: DenseBlockPosSet,
        cache: Set<Entity>,
        canCollect: (BlockPos, BlockState) -> Boolean,
    ) {
        val pos = frontier.poll()!!

        // TODO we should prob make it look like originals create's assembly method more.
        // So more check etc
        visited.add(pos.x, pos.y, pos.z)

        for (direction in Direction.values()) {
            if (!SuperGlueEntity.isGlued(level, pos, direction, (cache as Set<SuperGlueEntity>))) continue
            val newPos = pos.relative(direction)
            if (visited.contains(newPos.x, newPos.y, newPos.z)) continue
            val state = level.getBlockState(newPos)
            if (!isAllowed(state) || state.isAir || !canCollect(newPos, state)) continue
            frontier.add(newPos)
        }
    }

    private fun isAllowed(state: BlockState): Boolean {
        return !ClockworkConfig.SERVER.blockBlacklist.contains(BuiltInRegistries.BLOCK.getKey(state.block).toString())
    }
}
