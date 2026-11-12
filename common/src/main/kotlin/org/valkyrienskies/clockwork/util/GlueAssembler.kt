package org.valkyrienskies.clockwork.util

import com.simibubi.create.api.contraption.BlockMovementChecks
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity
import net.createmod.catnip.data.UniqueLinkedList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.PushReaction
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import java.util.*
import java.util.function.Consumer
object GlueAssembler {
    // TODO: Config this
    private const val MAX_BLOCKS = 100000

    @Throws(AssemblyException::class)
    fun collectGlued(level: Level, startPos: BlockPos): DenseBlockPosSet? {
        if (level.isOutsideBuildHeight(startPos) || !level.isLoaded(startPos)) return null
        if (level.getBlockState(startPos).isAir) return null

        val visited = DenseBlockPosSet()
        val frontier: Queue<BlockPos> = UniqueLinkedList()
        val glueCache: MutableSet<SuperGlueEntity> = HashSet()

        frontier.add(startPos)

        var processed = 0
        while (frontier.isNotEmpty()) {
            if (++processed > MAX_BLOCKS) throw AssemblyException.structureTooLarge()
            visitBlock(level, frontier, visited, glueCache)
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
            if (!isAllowed(newState)) continue

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
