package org.valkyrienskies.clockwork

import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.registry.level.biome.BiomeModifications
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BiomeTags
import net.minecraft.world.level.levelgen.GenerationStep


object ClockworkWorldgen {

    fun init() {
        LifecycleEvent.SETUP.register{


            BiomeModifications.addProperties { ctx, mutable ->
                if (ctx.hasTag(BiomeTags.IS_OVERWORLD) || ctx.hasTag(BiomeTags.IS_END)) {
                    mutable.getGenerationProperties()
                        .addFeature(GenerationStep.Decoration.UNDERGROUND_ORES,
                            ResourceKey.create(Registries.PLACED_FEATURE,
                                ResourceLocation(ClockworkMod.MOD_ID + ":ore_wanderlite")));
                }
            }
        }
    }
}