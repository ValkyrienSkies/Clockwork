package org.valkyrienskies.clockwork.forge;

import com.google.common.base.Suppliers;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.features.OreFeatures;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.valkyrienskies.clockwork.ClockworkBlocks;

import java.util.List;
import java.util.function.Supplier;

import static org.valkyrienskies.clockwork.ClockworkMod.MOD_ID;

public class ForgeClockworkWorldgen {

    public static final DeferredRegister<ConfiguredFeature<?, ?>> CONFIGURED_FEATURES
            = DeferredRegister.create(Registry.CONFIGURED_FEATURE_REGISTRY, MOD_ID);
    public static final DeferredRegister<PlacedFeature> PLACED_FEATURES
            = DeferredRegister.create(Registry.PLACED_FEATURE_REGISTRY, MOD_ID);

    //CONFIGURED
    private static final Supplier<List<OreConfiguration.TargetBlockState>> REPLACEMENTS = Suppliers.memoize(() ->
            List.of(
                    OreConfiguration.target(OreFeatures.DEEPSLATE_ORE_REPLACEABLES, ClockworkBlocks.WANDERLITE_DEEPSLATE_ORE.getDefaultState()),
                    OreConfiguration.target(new BlockMatchTest(Blocks.END_STONE), ClockworkBlocks.WANDERLITE_END_ORE.getDefaultState())
            )
    );

    public static final RegistryObject<ConfiguredFeature<?,?>> WANDERLITE_ORE = CONFIGURED_FEATURES.register("ore_wanderlite", () ->
            new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(REPLACEMENTS.get(), 6)));

    //PLACED
    public static final RegistryObject<PlacedFeature> WANDERLITE_ORE_PLACED = PLACED_FEATURES.register("ore_wanderlite", () ->
            new PlacedFeature(WANDERLITE_ORE.getHolder().get(),orePlacement(6, HeightRangePlacement.uniform(
                    VerticalAnchor.BOTTOM, VerticalAnchor.absolute(64)
            )) ));

    public static List<PlacementModifier> orePlacement(int count, PlacementModifier height){
        return List.of(CountPlacement.of(count), InSquarePlacement.spread(), height, BiomeFilter.biome());
    }
}
