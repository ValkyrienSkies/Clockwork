package org.valkyrienskies.clockwork

import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.registry.level.biome.BiomeModifications
import net.minecraft.data.worldgen.features.FeatureUtils
import net.minecraft.data.worldgen.features.OreFeatures.DEEPSLATE_ORE_REPLACEABLES
import net.minecraft.data.worldgen.placement.PlacementUtils
import net.minecraft.tags.BiomeTags
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.VerticalAnchor
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration
import net.minecraft.world.level.levelgen.placement.*
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest


object ClockworkWorldgen {

    fun init() {
        LifecycleEvent.SETUP.register{

            val configuredFeature =
                FeatureUtils.register<OreConfiguration, Feature<OreConfiguration>>(ClockworkMod.MOD_ID + ":ore_auric",
                    Feature.ORE,
                    OreConfiguration(
                        listOf(
                            OreConfiguration.target(DEEPSLATE_ORE_REPLACEABLES, ClockworkBlocks.AURIC_DEEPSLATE_ORE.get().defaultBlockState()),
                            OreConfiguration.target(BlockMatchTest(Blocks.END_STONE), ClockworkBlocks.AURIC_END_ORE.get().defaultBlockState())
                        ),
                        3))

            val placedFeature = PlacementUtils.register(ClockworkMod.MOD_ID + ":ore_auric", configuredFeature,
                listOf(
                    InSquarePlacement.spread(),
                    HeightRangePlacement.triangle(
                        VerticalAnchor.absolute(-64),
                        VerticalAnchor.absolute(64)),
                    BiomeFilter.biome()))

            BiomeModifications.addProperties { ctx, mutable ->

                if (ctx.hasTag(BiomeTags.IS_OVERWORLD) || ctx.hasTag(BiomeTags.IS_END)) {
                    mutable.getGenerationProperties()
                        .addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, placedFeature);
                }
            }
        }
    }
}