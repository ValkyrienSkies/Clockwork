package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.content.AllSections;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.util.entry.FluidEntry;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.valkyrienskies.clockwork.ClockWorkItems;
import org.valkyrienskies.clockwork.ClockWorkMod;

import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class ForgeClockworkFluids {

    public static final FluidEntry<ForgeFlowingFluid.Flowing> VANILLA_FROSTING =
            standardFluid("vanilla_frosting", NoColorFluidAttributes::new)
                    .lang(f -> "fluid.ClockWorkMod.vanilla_frosting", "Vanilla Frosting")
                    .attributes(b -> b.viscosity(1250)
                            .density(7040)
                            .temperature(20))
                    .properties(p -> p.levelDecreasePerBlock(2)
                            .tickRate(25)
                            .slopeFindDistance(3)
                            .explosionResistance(5f))
                    .source(ForgeFlowingFluid.Source::new)
                    .bucket()
                    .build()
                    .register();

    private static FluidBuilder<ForgeFlowingFluid.Flowing, CreateRegistrate> standardFluid(String name, NonNullBiFunction<FluidAttributes.Builder, Fluid, FluidAttributes> factory) {
        return ClockWorkMod.REGISTRATE
                .fluid(name, ClockWorkMod.asResource("fluid/" + name + "_still"), ClockWorkMod.asResource("fluid/" + name + "_flow"), factory)
                .removeTag(FluidTags.WATER);
    }

    public static void register() {}

    private static class NoColorFluidAttributes extends FluidAttributes {

        protected NoColorFluidAttributes(Builder builder, Fluid fluid) {
            super(builder, fluid);
        }

        @Override
        public int getColor(BlockAndTintGetter level, BlockPos pos) {
            return 0x00ffffff;
        }
    }
}
