package org.valkyrienskies.clockwork.platform.api;

import com.simibubi.create.content.contraptions.components.structureMovement.glue.SuperGlueEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.Set;

public interface GlueType {

    boolean isGlued(LevelAccessor level, BlockPos pos, Direction dir, Set<Entity> cache);

    boolean isBluGlued(LevelAccessor level, BlockPos pos, Direction dir, Set<Entity> cache);

    Set<Entity> caughtEntities(Level level, BlockPos startPos, BlockPos endPos);


    public static final GlueType SUPER = new GlueType() {
        @Override
        public boolean isGlued(LevelAccessor level, BlockPos pos, Direction dir, Set<Entity> cache) {
            return SuperGlueEntity.isGlued(level, pos, dir, (Set) cache);
        }

        @Override
        public boolean isBluGlued(LevelAccessor level, BlockPos pos, Direction dir, Set<Entity> cache) {
            return false;
        }

        @Override
        public Set<Entity> caughtEntities(Level level, BlockPos startPos, BlockPos endPos) {
            return null;
        }
    };

    public static final GlueType BLUPER = SUPER;/*new GlueType() {
        @Override
        public boolean isGlued(LevelAccessor level, BlockPos pos, Direction dir, Set<Entity> cache) {
            return false;
        }

        @Override
        public boolean isBluGlued(LevelAccessor level, BlockPos pos, Direction dir, Set<Entity> cache) {
            return BluperGlueEntity.isBluGlued(level, pos, dir, (Set) cache);
        }

        @Override
        public Set<Entity> caughtEntities(Level level, BlockPos startPos, BlockPos endPos) {
            return BluperGlueEntity.searchGlueGroupForEntities(level, startPos, endPos);
        }
    };*/
}