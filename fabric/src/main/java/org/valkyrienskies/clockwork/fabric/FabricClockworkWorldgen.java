package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.worldgen.AllPlacedFeatures;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.valkyrienskies.clockwork.ClockworkMod;

import java.util.function.Predicate;

public class FabricClockworkWorldgen {

    public static final ResourceKey<PlacedFeature>
            WANDERLITE_ORE = ResourceKey.create(Registries.PLACED_FEATURE, ClockworkMod.asResource("ore_wanderlite"));


    public static void bootstrap() {
        Predicate<BiomeSelectionContext> isOverworld = BiomeSelectors.foundInOverworld();
        Predicate<BiomeSelectionContext> isEnd = BiomeSelectors.foundInTheEnd();

        addOre(isOverworld, WANDERLITE_ORE);
        addOre(isEnd, WANDERLITE_ORE);
    }

    private static void addOre(Predicate<BiomeSelectionContext> test, ResourceKey<PlacedFeature> feature) {
        BiomeModifications.addFeature(test, GenerationStep.Decoration.UNDERGROUND_ORES, feature);
    }
}
