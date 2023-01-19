package org.valkyrienskies.clockwork.platform.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueEntity;

public class ForgeBluperGlueEntity extends BluperGlueEntity implements IEntityAdditionalSpawnData {

    public ForgeBluperGlueEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    public static void build(EntityType.Builder<ForgeBluperGlueEntity> tBuilder) {

    }
}
