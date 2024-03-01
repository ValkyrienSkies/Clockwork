package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer;
import com.simibubi.create.foundation.data.CreateEntityBuilder;
import com.tterrag.registrate.util.entry.EntityEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.valkyrienskies.clockwork.ClockworkLang;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.content.contraptions.smart_propeller.contraption.SuperContraptionEntity;
import org.valkyrienskies.clockwork.platform.entity.FabricSequencedSeatEntity;

public class FabricClockworkEntities {
    public static final EntityEntry<FabricSequencedSeatEntity> SEQUENCED_SEAT = register(
            "sequenced_seat",
            FabricSequencedSeatEntity::new,
            () -> SeatEntity.Render::new,
            MobCategory.MISC,
            5,
            Integer.MAX_VALUE,
            false,
            true,
            FabricSequencedSeatEntity::build
    ).register();


    public static final EntityEntry<SuperContraptionEntity> SUPER_CONTRAPTION_ENTITY = contraption(
            "super_contraption",
            SuperContraptionEntity::new,
            () -> ContraptionEntityRenderer::new,
            20, 40, false
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
                                                                         NonNullConsumer<FabricEntityTypeBuilder<T>> propertyBuilder) {
        String id = ClockworkLang.asId(name);
        return (CreateEntityBuilder<T, ?>) ClockworkMod.INSTANCE.getREGISTRATE()
                .entity(id, factory, group)
                .properties(b -> b.trackRangeChunks(range)
                        .trackedUpdateRate(updateFrequency)
                        .forceTrackedVelocityUpdates(sendVelocity))
                .properties(propertyBuilder)
                .properties(b -> {
                    if (immuneToFire)
                        b.fireImmune();
                })
                .renderer(renderer);
    }

    public static void register() {
    }
}
