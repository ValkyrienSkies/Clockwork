package org.valkyrienskies.clockwork

import dev.architectury.hooks.level.biome.BiomeProperties
import dev.architectury.registry.level.biome.BiomeModifications
import dev.architectury.registry.registries.DeferredRegister
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.BiomeTags
import net.minecraft.world.level.levelgen.GenerationStep
import org.valkyrienskies.clockwork.content.worldgen.TarPitFeature

object ClockworkWorldgen {
    private val FEATURES = DeferredRegister.create(ClockworkMod.MOD_ID, Registries.FEATURE)
    val TAR_PIT = FEATURES.register("tar_pit") { TarPitFeature() }

    fun register() {
        FEATURES.register()
        BiomeModifications.addProperties { ctx: BiomeModifications.BiomeContext, mutable: BiomeProperties.Mutable ->
            if (ctx.hasTag(BiomeTags.IS_NETHER)) return@addProperties

            mutable.generationProperties.addFeature(
                GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, ClockworkMod.asResource("wanderlite_ore"))
            )

            if (ctx.hasTag(BiomeTags.IS_OVERWORLD)) {
                mutable.generationProperties.addFeature(
                    GenerationStep.Decoration.LAKES,
                    ResourceKey.create(Registries.PLACED_FEATURE, ClockworkMod.asResource("tar_pit"))
                )
            }
        }
    }
}
