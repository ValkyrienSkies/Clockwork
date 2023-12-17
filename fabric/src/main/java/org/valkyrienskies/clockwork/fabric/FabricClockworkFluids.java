package org.valkyrienskies.clockwork.fabric;


import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.fabric.SimpleFlowableFluid;
import com.tterrag.registrate.util.entry.FluidEntry;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.EmptyItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import org.valkyrienskies.clockwork.ClockworkMod;

import static net.minecraft.world.item.Items.BUCKET;

public class FabricClockworkFluids {

    public static final FluidEntry<SimpleFlowableFluid.Flowing> VANILLA_FROSTING =
            frostingFluid("vanilla_frosting"/*, NoColorFluidAttributes::new*/)
                    .lang("vanilla_frosting")
                    .fluidProperties(p -> p.levelDecreasePerBlock(2)
                            .tickRate(25)
                            .flowSpeed(3)
                            .blastResistance(100f))

                    .source(SimpleFlowableFluid.Source::new) // TODO: remove when Registrate fixes FluidBuilder
                    .bucket()
                    .tag(AllTags.forgeItemTag("buckets/vanilla_frosting"))
                    .build()
                    .onRegisterAfter(Registries.ITEM, vanilla_frosting -> {
                        Fluid source = vanilla_frosting.getSource();
                        FluidStorage.combinedItemApiProvider(source.getBucket()).register(context ->
                                new FullItemFluidStorage(context, bucket -> ItemVariant.of(BUCKET), FluidVariant.of(source), FluidConstants.BUCKET));
                        FluidStorage.combinedItemApiProvider(BUCKET).register(context ->
                                new EmptyItemFluidStorage(context, bucket -> ItemVariant.of(source.getBucket()), source, FluidConstants.BUCKET));
                    })
                    .register();

    public static final FluidEntry<SimpleFlowableFluid.Flowing> CHOCOLATE_FROSTING =
            frostingFluid("chocolate_frosting")
                    .lang("chocolate_frosting")
                    .fluidProperties(p -> p.levelDecreasePerBlock(2)
                            .tickRate(25)
                            .flowSpeed(3)
                            .blastResistance(100f))

                    .source(SimpleFlowableFluid.Source::new) // TODO: remove when Registrate fixes FluidBuilder
                    .bucket()
                    .tag(AllTags.forgeItemTag("buckets/chocolate_frosting"))
                    .build()
                    .onRegisterAfter(Registries.ITEM, chocolate_frosting -> {
                        Fluid source = chocolate_frosting.getSource();
                        FluidStorage.combinedItemApiProvider(source.getBucket()).register(context ->
                                new FullItemFluidStorage(context, bucket -> ItemVariant.of(BUCKET), FluidVariant.of(source), FluidConstants.BUCKET));
                        FluidStorage.combinedItemApiProvider(BUCKET).register(context ->
                                new EmptyItemFluidStorage(context, bucket -> ItemVariant.of(source.getBucket()), source, FluidConstants.BUCKET));
                    })
                    .register();

    public static final FluidEntry<SimpleFlowableFluid.Flowing> STRAWBERRY_FROSTING =
            frostingFluid("strawberry_frosting")
                    .lang("strawberry_frosting")
                    .fluidProperties(p -> p.levelDecreasePerBlock(2)
                            .tickRate(25)
                            .flowSpeed(3)
                            .blastResistance(100f))

                    .source(SimpleFlowableFluid.Source::new) // TODO: remove when Registrate fixes FluidBuilder
                    .bucket()
                    .tag(AllTags.forgeItemTag("buckets/strawberry_frosting"))
                    .build()
                    .onRegisterAfter(Registries.ITEM, chocolate_frosting -> {
                        Fluid source = chocolate_frosting.getSource();
                        FluidStorage.combinedItemApiProvider(source.getBucket()).register(context ->
                                new FullItemFluidStorage(context, bucket -> ItemVariant.of(BUCKET), FluidVariant.of(source), FluidConstants.BUCKET));
                        FluidStorage.combinedItemApiProvider(BUCKET).register(context ->
                                new EmptyItemFluidStorage(context, bucket -> ItemVariant.of(source.getBucket()), source, FluidConstants.BUCKET));
                    })
                    .register();

    public static FluidBuilder<SimpleFlowableFluid.Flowing, CreateRegistrate> frostingFluid(String name) {
        return ClockworkMod.INSTANCE.getREGISTRATE().fluid(name, ClockworkMod.INSTANCE.asResource("fluid/" + name + "_still"), ClockworkMod.INSTANCE.asResource("fluid/" + name + "_flow"));
    }

    public static void register() {
    }

//    public static FluidBuilder<SimpleFlowableFluid.Flowing, CreateRegistrate> frostingFluid(String name) {
//        return ClockWorkMod.REGISTRATE.fluid(name, Create.asResource("fluid/frosting_still"), Create.asResource("fluid/frosting_flow"));
//    }

}