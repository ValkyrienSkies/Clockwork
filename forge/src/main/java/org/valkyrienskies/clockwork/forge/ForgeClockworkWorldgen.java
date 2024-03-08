package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.worldgen.AllPlacedFeatures;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.registries.ForgeRegistries;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.ClockworkMod;

import java.util.List;

import static net.minecraft.data.worldgen.features.FeatureUtils.register;

public class ForgeClockworkWorldgen {

    public static final ResourceKey<BiomeModifier>
            WANDERLITE_ORE = ResourceKey.create(ForgeRegistries.Keys.BIOME_MODIFIERS, ClockworkMod.asResource("ore_wanderlite"));

    public static final ResourceKey<PlacedFeature>
            WANDERLITE_ORE_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, ClockworkMod.asResource("ore_wanderlite"));


    public static void bootstrap(BootstapContext<BiomeModifier> ctx) {
        HolderGetter<Biome> biomeLookup = ctx.lookup(Registries.BIOME);
        HolderSet<Biome> isOverworld = biomeLookup.getOrThrow(BiomeTags.IS_OVERWORLD);
        HolderSet<Biome> isEnd = biomeLookup.getOrThrow(BiomeTags.IS_END);

        HolderGetter<PlacedFeature> featureLookup = ctx.lookup(Registries.PLACED_FEATURE);
        Holder<PlacedFeature> wanderliteOre = featureLookup.getOrThrow(WANDERLITE_ORE_PLACED);

        ctx.register(WANDERLITE_ORE, addOre(isOverworld, wanderliteOre));
        ctx.register(WANDERLITE_ORE, addOre(isEnd, wanderliteOre));
    }

    private static ForgeBiomeModifiers.AddFeaturesBiomeModifier addOre(HolderSet<Biome> biomes, Holder<PlacedFeature> feature) {
        return new ForgeBiomeModifiers.AddFeaturesBiomeModifier(biomes, HolderSet.direct(feature), GenerationStep.Decoration.UNDERGROUND_ORES);
    }
}
