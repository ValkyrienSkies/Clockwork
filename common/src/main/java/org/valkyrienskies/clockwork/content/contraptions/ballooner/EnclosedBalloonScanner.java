package org.valkyrienskies.clockwork.content.contraptions.ballooner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.data.ClockWorkTags;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import it.unimi.dsi.fastutil.Pair;

public record EnclosedBalloonScanner(Level level, int maxChecks) {
    public Pair<Set<BlockPos>, Set<BlockPos>> getEnclosedBalloons(BlockPos burnerPos) {
        Set<BlockPos> positions = new HashSet<>();
        Set<BlockPos> balloonPositions = new HashSet<>();
        Set<BlockPos> queuedPositions = new HashSet<>();
        queuedPositions.add(burnerPos);
        while (!queuedPositions.isEmpty()) {
            if (positions.size() >= maxChecks) {
                break;
            }
            Iterator<BlockPos> iterator = queuedPositions.iterator();
            BlockPos pos = iterator.next();
            iterator.remove();

            BlockState state = level.getBlockState(pos);

            if (state.is(ClockWorkTags.AllBlockTags.BALLOON_BLOCK.tag)) {
                balloonPositions.add(pos);
                continue;
            }

            if (state.isAir() || !state.isCollisionShapeFullBlock(level, pos)) {
                positions.add(pos);
            }


            for (Direction dir : Direction.values()) {
                BlockPos offsetPos = pos.relative(dir);
                if (!positions.contains(offsetPos)) {
                    queuedPositions.add(offsetPos);
                }
            }
        }
        return Pair.of(positions, balloonPositions);
    }
}
