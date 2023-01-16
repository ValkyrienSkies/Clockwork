package org.valkyrienskies.clockwork.fabric.util;
import com.simibubi.create.content.contraptions.components.structureMovement.glue.SuperGlueEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.valkyrienskies.clockwork.fabric.content.curiosities.tools.bluperglue.BluperGlueEntity;

import java.util.Set;

public interface GlueType {

    boolean isGlued(LevelAccessor level, BlockPos pos, Direction dir, Set<Entity> cache);
    boolean isBluGlued(LevelAccessor level, BlockPos pos, Direction dir, Set<Entity> cache);


    public static final GlueType SUPER = new GlueType() {
        @Override
        public boolean isGlued(LevelAccessor level, BlockPos pos, Direction dir, Set<Entity> cache) {
            return SuperGlueEntity.isGlued(level, pos, dir, (Set) cache);
        }

        @Override
        public boolean isBluGlued(LevelAccessor level, BlockPos pos, Direction dir, Set<Entity> cache) {
            return false;
        }
    };

    public static final GlueType BLUPER = new GlueType() {
        @Override
        public boolean isGlued(LevelAccessor level, BlockPos pos, Direction dir, Set<Entity> cache) {
            return false;
        }

        @Override
        public boolean isBluGlued(LevelAccessor level, BlockPos pos, Direction dir, Set<Entity> cache) {
            return BluperGlueEntity.isBluGlued(level, pos, dir, (Set) cache);
        }
    };
}