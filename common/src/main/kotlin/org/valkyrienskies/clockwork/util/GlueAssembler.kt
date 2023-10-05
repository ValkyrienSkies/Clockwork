package org.valkyrienskies.clockwork.util.assemble

import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity
import com.simibubi.create.foundation.utility.UniqueLinkedList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.TextComponent
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import java.util.*
import java.util.function.Consumer

object GlueAssembler {
    @Throws(AssemblyException::class)
    fun collectGlued(
        level: Level,
        pos: BlockPos,
    ): DenseBlockPosSet? {
        val toRemove: Set<Entity> = HashSet()
        val result = DenseBlockPosSet()
        val frontier: Queue<BlockPos> = UniqueLinkedList()
        if (level.getBlockState(pos).isAir) return null
        frontier.add(pos)
        for (limit in 100000 downTo 1) {
            if (frontier.isEmpty()) {
                if (result.isEmpty()) throw AssemblyException(TextComponent("No blocks found!"))
                return result
            }
            visitBlock(level, frontier, result, toRemove)
        }
        toRemove.forEach(Consumer { obj: Entity -> obj.discard() })
        throw AssemblyException.structureTooLarge()
    }

    @Throws(AssemblyException::class)
    private fun visitBlock(
        level: LevelAccessor,
        frontier: Queue<BlockPos>,
        visited: DenseBlockPosSet,
        cache: Set<Entity>
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
            if (!isAllowed(state) || state.isAir) continue
            frontier.add(newPos)
        }
    }

    private fun isAllowed(state: BlockState): Boolean {
        return true // TODO blacklisting or unmovable whatever
    }
}