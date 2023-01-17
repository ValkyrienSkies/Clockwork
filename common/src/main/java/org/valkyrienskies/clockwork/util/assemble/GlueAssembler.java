package org.valkyrienskies.clockwork.util.assemble;

import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.foundation.utility.UniqueLinkedList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.platform.api.GlueType;
import org.valkyrienskies.core.impl.datastructures.DenseBlockPosSet;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class GlueAssembler {

    public static DenseBlockPosSet collectGlued(
            Level level,
            BlockPos pos,
            GlueType glueType
    ) throws AssemblyException {
        Set<Entity> toRemove = new HashSet<>();
        DenseBlockPosSet result = new DenseBlockPosSet();
        Queue<BlockPos> frontier = new UniqueLinkedList<>();


        if (level.getBlockState(pos).isAir()) return null;

        frontier.add(pos);

        for (int limit = 100000; limit > 0; limit--) {
            if (frontier.isEmpty()) {
                if (result.isEmpty()) throw new AssemblyException(new TextComponent("No blocks found!"));
                return result;
            }

            visitBlock(level, frontier, result, glueType, toRemove);
            ;
        }

        toRemove.forEach(Entity::discard);

        throw AssemblyException.structureTooLarge();
    }

    public static Set<Entity> collectEntities(
            Level level,
            BlockPos pos,
            GlueType glueType
    ) throws AssemblyException {
        Set<Entity> toTranspose = new HashSet<>();

        toTranspose = visitEntity(level, pos, pos, glueType, toTranspose);
        return toTranspose;
    }

    private static void visitBlock(
            LevelAccessor level,
            Queue<BlockPos> frontier,
            DenseBlockPosSet visited,
            GlueType glue,
            Set<Entity> cache
    ) throws AssemblyException {
        BlockPos pos = frontier.poll();
        assert pos != null;

        // TODO we should prob make it look like originals create's assembly method more.
        // So more check etc

        visited.add(pos.getX(), pos.getY(), pos.getZ());
        if (glue == GlueType.SUPER) {
            for (Direction direction : Direction.values()) {
                if (!glue.isGlued(level, pos, direction, cache)) continue;

                BlockPos newPos = pos.relative(direction);
                if (visited.contains(newPos.getX(), newPos.getY(), newPos.getZ())) continue;

                BlockState state = level.getBlockState(newPos);
                if (!isAllowed(state) || state.isAir()) continue;

                frontier.add(newPos);
            }
        }
        if (glue == GlueType.BLUPER) {
            for (Direction direction : Direction.values()) {
                if (!glue.isBluGlued(level, pos, direction, cache)) continue;

                BlockPos newPos = pos.relative(direction);
                if (visited.contains(newPos.getX(), newPos.getY(), newPos.getZ())) continue;

                BlockState state = level.getBlockState(newPos);
                if (!isAllowed(state) || state.isAir()) continue;

                frontier.add(newPos);
            }
        }
    }

    private static Set<Entity> visitEntity(
            Level level,
            BlockPos pos,
            BlockPos endPos,
            GlueType glue,
            Set<Entity> cache
    ) throws AssemblyException {

        if (glue == GlueType.BLUPER) {
            cache = glue.caughtEntities(level, pos, endPos);
        }
        if (cache.isEmpty()) {
            return null;
        }
        return cache;

    }

    private static boolean isAllowed(BlockState state) {
        return true; // TODO blacklisting or unmovable whatever
    }
}
