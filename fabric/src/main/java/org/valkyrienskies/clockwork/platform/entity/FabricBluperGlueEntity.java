package org.valkyrienskies.clockwork.platform.entity;

import io.github.fabricators_of_create.porting_lib.entity.ExtraSpawnDataEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueEntity;

public class FabricBluperGlueEntity extends BluperGlueEntity implements ExtraSpawnDataEntity {

    public FabricBluperGlueEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    public FabricBluperGlueEntity(Level world, AABB boundingBox) {
        super(world, boundingBox);
    }

    public static FabricEntityTypeBuilder<?> build(FabricEntityTypeBuilder<?> builder) {
        return builder;
    }
}
