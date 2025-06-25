package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.Create;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.architectury.registry.registries.DeferredRegister;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.OreFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.ClockworkMod;

import java.util.Arrays;
import java.util.function.Predicate;

import static org.valkyrienskies.clockwork.ClockworkMod.MOD_ID;

public class FabricClockworkWorldgen {

    public static final DeferredRegister<ConfiguredFeature<?, ?>> CONFIGURED_FEATURES
            = DeferredRegister.create(MOD_ID, Registries.CONFIGURED_FEATURE);
    public static final DeferredRegister<PlacedFeature> PLACED_FEATURES
            = DeferredRegister.create(MOD_ID, Registries.PLACED_FEATURE);

    private static ConfiguredFeature<?, ?> WANDERLITE_ORE_CONFIGURED_FEATURE = new ConfiguredFeature
            (Feature.ORE, new OreConfiguration(
                    new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES),
                    ClockworkBlocks.WANDERLITE_DEEPSLATE_ORE.getDefaultState(),
                    9)); // vein size

    public static PlacedFeature WANDERLITE_ORE_PLACED_FEATURE = new PlacedFeature(
            Holder.direct(WANDERLITE_ORE_CONFIGURED_FEATURE),
            Arrays.asList(
                    CountPlacement.of(20), // number of veins per chunk
                    InSquarePlacement.spread(), // spreading horizontally
                    HeightRangePlacement.uniform(VerticalAnchor.BOTTOM, VerticalAnchor.absolute(64))
            )); // height

    public static final ResourceKey<PlacedFeature>
            WANDERLITE_ORE = ResourceKey.create(Registries.PLACED_FEATURE, ClockworkMod.asResource("ore_wanderlite"));


    public static void bootstrap() {
        PLACED_FEATURES.register("ore_wanderlite", () -> WANDERLITE_ORE_PLACED_FEATURE);
        CONFIGURED_FEATURES.register("ore_wanderlite", () -> WANDERLITE_ORE_CONFIGURED_FEATURE);

        Predicate<BiomeSelectionContext> isOverworld = BiomeSelectors.foundInOverworld();
        Predicate<BiomeSelectionContext> isEnd = BiomeSelectors.foundInTheEnd();

        addOre(isOverworld, WANDERLITE_ORE);
        addOre(isEnd, WANDERLITE_ORE);
    }

    private static void addOre(Predicate<BiomeSelectionContext> test, ResourceKey<PlacedFeature> feature) {
        BiomeModifications.addFeature(test, GenerationStep.Decoration.UNDERGROUND_ORES, feature);
    }
}
