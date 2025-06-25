package org.valkyrienskies.clockwork

import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.registry.level.biome.BiomeModifications
import net.minecraft.data.worldgen.features.FeatureUtils
import net.minecraft.data.worldgen.placement.PlacementUtils
import net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.VerticalAnchor
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration
import net.minecraft.world.level.levelgen.placement.*
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest


object ClockworkWorldgen {

    fun init() {
//        LifecycleEvent.SETUP.register {
//
//            val configuredFeature = FeatureUtils.register<OreConfiguration, Feature<OreConfiguration>>(ClockworkMod.MOD_ID + ":ore_wanderlite",
//                    Feature.ORE,
//                    OreConfiguration(
//                        listOf(
//                            OreConfiguration.target(TagMatchTest(DEEPSLATE_ORE_REPLACEABLES), ClockworkBlocks.WANDERLITE_DEEPSLATE_ORE.get().defaultBlockState()),
//                            OreConfiguration.target(BlockMatchTest(Blocks.END_STONE), ClockworkBlocks.WANDERLITE_END_ORE.get().defaultBlockState())
//                        ),
//                        3))
//
//            val placedFeature = PlacementUtils.register(ClockworkMod.MOD_ID + ":ore_wanderlite", configuredFeature,
//                listOf(
//                    InSquarePlacement.spread(),
//                    HeightRangePlacement.triangle(
//                        VerticalAnchor.absolute(-64),
//                        VerticalAnchor.absolute(64)),
//                    BiomeFilter.biome()))
//
//            BiomeModifications.addProperties { ctx, mutable ->
//                if (true) {
//                    mutable.getGenerationProperties()
//                        .addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, placedFeature);
//                }
//            }
//        }
    }
}