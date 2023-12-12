package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.util.entry.FluidEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.ClockworkSounds;

import java.util.function.Consumer;

public class ForgeClockworkFluids {

    public static final FluidEntry<ForgeFlowingFluid.Flowing> VANILLA_FROSTING =
            standardFluid("vanilla_frosting", NoColorFluidAttributes::new)
                    .lang(f -> "fluid.ClockWorkMod.vanilla_frosting", "Vanilla Frosting")
                    .properties(b -> b.viscosity(1250)
                            .density(7040)
                            .temperature(20)
                            .sound(SoundActions.BUCKET_EMPTY, ClockworkSounds.INSTANCE.getTHICK_FLUID_EMPTY().getMainEvent())
                            .sound(SoundActions.BUCKET_FILL, ClockworkSounds.INSTANCE.getTHICK_FLUID_FILL().getMainEvent()))
                    .fluidProperties(p -> p.levelDecreasePerBlock(2)
                            .tickRate(25)
                            .slopeFindDistance(3)
                            .explosionResistance(5f))
                    .source(ForgeFlowingFluid.Source::new)
                    .bucket()
                    .build()
                    .register();

    public static final FluidEntry<ForgeFlowingFluid.Flowing> CHOCOLATE_FROSTING = standardFluid("chocolate_frosting", NoColorFluidAttributes::new)
            .lang(f -> "fluid.ClockWorkMod.chocolate_frosting", "Chocolate Frosting")
            .properties(b -> b.viscosity(1250)
                    .density(7040)
                    .temperature(20)
                    .sound(SoundActions.BUCKET_EMPTY, ClockworkSounds.INSTANCE.getTHICK_FLUID_EMPTY().getMainEvent())
                    .sound(SoundActions.BUCKET_FILL, ClockworkSounds.INSTANCE.getTHICK_FLUID_FILL().getMainEvent()))
            .fluidProperties(p -> p.levelDecreasePerBlock(2)
                    .tickRate(25)
                    .slopeFindDistance(3)
                    .explosionResistance(5f))
            .source(ForgeFlowingFluid.Source::new)
            .bucket()
            .build()
            .register();

    public static final FluidEntry<ForgeFlowingFluid.Flowing> STRAWBERRY_FROSTING = standardFluid("strawberry_frosting", NoColorFluidAttributes::new)
            .lang(f -> "fluid.ClockWorkMod.strawberry_frosting", "Strawberry Frosting")
            .properties(b -> b.viscosity(1250)
                    .density(7040)
                    .temperature(20)
                    .sound(SoundActions.BUCKET_EMPTY, ClockworkSounds.INSTANCE.getTHICK_FLUID_EMPTY().getMainEvent())
                    .sound(SoundActions.BUCKET_FILL, ClockworkSounds.INSTANCE.getTHICK_FLUID_FILL().getMainEvent()))
            .fluidProperties(p -> p.levelDecreasePerBlock(2)
                    .tickRate(25)
                    .slopeFindDistance(3)
                    .explosionResistance(5f))
            .source(ForgeFlowingFluid.Source::new)
            .bucket()
            .build()
            .register();


    private static FluidBuilder<ForgeFlowingFluid.Flowing, CreateRegistrate> standardFluid(String name, FluidBuilder.FluidTypeFactory factory) {
        return ClockworkMod.INSTANCE.getREGISTRATE()
                .fluid(name, ClockworkMod.INSTANCE.asResource("fluid/" + name + "_still"), ClockworkMod.INSTANCE.asResource("fluid/" + name + "_flow"), factory)
                .removeTag(FluidTags.WATER);
    }

    private static FluidBuilder<ForgeFlowingFluid.Flowing, CreateRegistrate> frostingFluid(String name, FluidBuilder.FluidTypeFactory factory) {
        return ClockworkMod.INSTANCE.getREGISTRATE()
                .fluid(name, ClockworkMod.INSTANCE.asResource("fluid/frosting_still"), ClockworkMod.INSTANCE.asResource("fluid/frosting_flow"), factory)
                .removeTag(FluidTags.WATER);
    }

    public static void register() {
    }

    private static class NoColorFluidAttributes extends FluidType {
        private ResourceLocation stillTexture;
        private ResourceLocation flowingTexture;

        public NoColorFluidAttributes(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
            super(properties);
            this.stillTexture = stillTexture;
            this.flowingTexture = flowingTexture;
        }

        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {

                @Override
                public ResourceLocation getStillTexture() {
                    return stillTexture;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return flowingTexture;
                }
            });
        }
    }
}
