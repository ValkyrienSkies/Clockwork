package org.valkyrienskies.clockwork

import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer
import com.simibubi.create.foundation.data.CreateEntityBuilder
import com.tterrag.registrate.util.entry.EntityEntry
import com.tterrag.registrate.util.nullness.NonNullConsumer
import com.tterrag.registrate.util.nullness.NonNullFunction
import com.tterrag.registrate.util.nullness.NonNullSupplier
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.content.contraptions.smart_propeller.SuperContraptionEntity
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatEntity
import org.valkyrienskies.clockwork.platform.SharedValues

object ClockworkEntities {
    val SEQUENCED_SEAT: EntityEntry<SequencedSeatEntity> = SharedValues.sequencedSeat

    val SUPER_CONTRAPTION: EntityEntry<SuperContraptionEntity?> =
        contraption("super_contraption",
            { type: EntityType<SuperContraptionEntity?>?, world: Level? ->
                SuperContraptionEntity(type, world)
            },
            {
                NonNullFunction<EntityRendererProvider.Context, EntityRenderer<in SuperContraptionEntity?>> { context: EntityRendererProvider.Context ->
                    ContraptionEntityRenderer(context)
                }
            }, 20, 40, false).register()


    //
    private fun <T : Entity?> contraption(name: String,
                                          factory: EntityType.EntityFactory<T>,
                                          renderer: NonNullSupplier<NonNullFunction<EntityRendererProvider.Context, EntityRenderer<in T>>>,
                                          range: Int,
                                          updateFrequency: Int,
                                          sendVelocity: Boolean): CreateEntityBuilder<T, *> {
        return register(name,
            factory,
            renderer,
            MobCategory.MISC,
            range,
            updateFrequency,
            sendVelocity,
            true) { builder: FabricEntityTypeBuilder<T>? -> AbstractContraptionEntity.build(builder) }
    }

    private fun <T : Entity?> register(name: String,
                                       factory: EntityType.EntityFactory<T>,
                                       renderer: NonNullSupplier<NonNullFunction<EntityRendererProvider.Context, EntityRenderer<in T>>>,
                                       group: MobCategory,
                                       range: Int,
                                       updateFrequency: Int,
                                       sendVelocity: Boolean,
                                       immuneToFire: Boolean,
                                       propertyBuilder: NonNullConsumer<FabricEntityTypeBuilder<T>>): CreateEntityBuilder<T, *> {
        val id = ClockworkLang.asId(name)
        return ClockworkMod.REGISTRATE
            .entity(id, factory, group)
            .properties { b: FabricEntityTypeBuilder<T> ->
                b.trackRangeChunks(range)
                    .trackedUpdateRate(updateFrequency)
                    .forceTrackedVelocityUpdates(sendVelocity)
            }
            .properties(propertyBuilder)
            .properties { b: FabricEntityTypeBuilder<T> ->
                if (immuneToFire) b.fireImmune()
            }
            .renderer(renderer) as CreateEntityBuilder<T, *>
    }

    @JvmStatic
    fun register() {
    }
}