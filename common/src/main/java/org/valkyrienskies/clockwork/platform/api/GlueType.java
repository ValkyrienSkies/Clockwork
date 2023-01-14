package org.valkyrienskies.clockwork.platform.api;

import com.simibubi.create.content.contraptions.components.structureMovement.glue.SuperGlueEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import java.util.Set;

public interface GlueType {

    boolean isGlued(LevelAccessor level, BlockPos pos, Direction dir, Set<Entity> cache);

    //TODO
    public static final GlueType SUPER = new GlueType() {
        @Override
        public boolean isGlued(LevelAccessor level, BlockPos pos, Direction dir, Set<Entity> cache) {
            return SuperGlueEntity.isGlued(level, pos, dir, (Set) cache);
        }
    };

    public static final GlueType BLUE = null;
}
