package org.valkyrienskies.clockwork.fabric.util.assemble;

import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.glue.SuperGlueEntity;
import com.simibubi.create.foundation.utility.UniqueLinkedList;
import com.tterrag.registrate.fabric.TriFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.fabric.content.curiosities.tools.bluperglue.BluperGlueEntity;
import org.valkyrienskies.core.impl.datastructures.DenseBlockPosSet;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class GlueAssembler {

    public static DenseBlockPosSet collectGlued(LevelAccessor level, BlockPos pos) throws AssemblyException {
        Set<SuperGlueEntity> toRemove = new HashSet<>();
        DenseBlockPosSet result = collectGlued(level, pos,
                (world, bpos, offset) -> SuperGlueEntity.isGlued(world, bpos, offset, toRemove));

        // TODO place glue in the ship? we should cache glue somewhere for ... dissembly
        toRemove.forEach(Entity::discard);

        return result;
    }

    public static DenseBlockPosSet collectBluGlued(LevelAccessor level, BlockPos pos) throws AssemblyException {
        Set<BluperGlueEntity> toRemove = new HashSet<>();
        DenseBlockPosSet result = collectGlued(level, pos,
                (world, bpos, offset) -> BluperGlueEntity.isBluGlued(world, bpos, offset, toRemove));

        // TODO place glue in the ship? we should cache glue somewhere for ... dissembly
        toRemove.forEach(Entity::discard);

        return result;
    }

    private static DenseBlockPosSet collectGlued(
            LevelAccessor level,
            BlockPos pos,
            TriFunction<LevelAccessor, BlockPos, Direction, Boolean> isGlued
    ) throws AssemblyException {
        DenseBlockPosSet result = new DenseBlockPosSet();
        Queue<BlockPos> frontier = new UniqueLinkedList<>();

        if (level.getBlockState(pos).isAir()) return null;

        frontier.add(pos);

        for (int limit = 100000; limit > 0; limit--) {
            if (frontier.isEmpty()) {
                if (result.isEmpty()) throw new AssemblyException(new TextComponent("No blocks found!"));
                return result;
            }

            visitBlock(level, frontier, result, isGlued);
        }

        throw AssemblyException.structureTooLarge();
    }

    private static void visitBlock(
            LevelAccessor level,
            Queue<BlockPos> frontier,
            DenseBlockPosSet visited,
            TriFunction<LevelAccessor, BlockPos, Direction, Boolean> isGlued
    ) throws AssemblyException {
        BlockPos pos = frontier.poll();
        assert pos != null;

        // TODO we should prob make it look like originals create's assembly method more.
        // So more check etc

        visited.add(pos.getX(), pos.getY(), pos.getZ());

        for (Direction direction : Direction.values()) {
            if (!isGlued.apply(level, pos, direction)) continue;

            BlockPos newPos = pos.relative(direction);
            if (visited.contains(newPos.getX(), newPos.getY(), newPos.getZ())) continue;

            BlockState state = level.getBlockState(newPos);
            if (!isAllowed(state) || state.isAir()) continue;

            frontier.add(newPos);
        }
    }

    private static boolean isAllowed(BlockState state) {
        return true; // TODO blacklisting or unmovable whatever
    }

}
