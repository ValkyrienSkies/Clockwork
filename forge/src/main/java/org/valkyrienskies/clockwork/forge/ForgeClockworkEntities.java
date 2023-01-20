package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.content.contraptions.components.actors.SeatEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.foundation.data.CreateEntityBuilder;
import com.simibubi.create.foundation.utility.Lang;
import com.tterrag.registrate.util.entry.EntityEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueRenderer;
import org.valkyrienskies.clockwork.platform.entity.ForgeBluperGlueEntity;
import org.valkyrienskies.clockwork.platform.entity.ForgeSequencedSeatEntity;

public class ForgeClockworkEntities {

    public static final EntityEntry<ForgeBluperGlueEntity> BLUPERGLUE =
            register(
                    "bluperglue",
                    ForgeBluperGlueEntity::new,
                    () -> BluperGlueRenderer::new,
                    MobCategory.MISC,
                    10,
                    Integer.MAX_VALUE,
                    false,
                    true,
                    ForgeBluperGlueEntity::build
            ).register();

    public static final EntityEntry<ForgeSequencedSeatEntity> SEQUENCED_SEAT =
            register(
                    "sequenced_seat",
                    ForgeSequencedSeatEntity::new,
                    () -> SeatEntity.Render::new,
                    MobCategory.MISC,
                    5,
                    Integer.MAX_VALUE,
                    false,
                    true,
                    ForgeSequencedSeatEntity::build
            ).register();

    private static <T extends Entity> CreateEntityBuilder<T, ?> contraption(String name, EntityType.EntityFactory<T> factory,
                                                                            NonNullSupplier<NonNullFunction<EntityRendererProvider.Context, EntityRenderer<? super T>>> renderer, int range,
                                                                            int updateFrequency, boolean sendVelocity) {
        return register(name, factory, renderer, MobCategory.MISC, range, updateFrequency, sendVelocity, true,
                AbstractContraptionEntity::build);
    }

    private static <T extends Entity> CreateEntityBuilder<T, ?> register(String name, EntityType.EntityFactory<T> factory,
                                                                         NonNullSupplier<NonNullFunction<EntityRendererProvider.Context, EntityRenderer<? super T>>> renderer,
                                                                         MobCategory group, int range, int updateFrequency, boolean sendVelocity, boolean immuneToFire,
                                                                         NonNullConsumer<EntityType.Builder<T>> propertyBuilder) {
        String id = Lang.asId(name);
        return (CreateEntityBuilder<T, ?>) ClockWorkMod.REGISTRATE
                .entity(id, factory, group)
                .properties(b -> b.setTrackingRange(range)
                        .setUpdateInterval(updateFrequency)
                        .setShouldReceiveVelocityUpdates(sendVelocity))
                .properties(propertyBuilder)
                .properties(b -> {
                    if (immuneToFire)
                        b.fireImmune();
                })
                .renderer(renderer);
    }

    public static void register() {}
}
