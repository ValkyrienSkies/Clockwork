package org.valkyrienskies.clockwork

import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.hooks.level.biome.BiomeProperties
import dev.architectury.registry.level.biome.BiomeModifications
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BiomeTags
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.placement.PlacedFeature

object ClockworkWorldgen {
    fun register() {

        BiomeModifications.addProperties { ctx: BiomeModifications.BiomeContext, mutable: BiomeProperties.Mutable ->
            if (ctx.hasTag(BiomeTags.IS_NETHER)) return@addProperties

            ClockworkMod.LOGGER.info("REGISTERING WANDERLITE")
            mutable.generationProperties.addFeature(
                GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, ClockworkMod.asResource("wanderlite_ore"))
            )
        }

    }

}