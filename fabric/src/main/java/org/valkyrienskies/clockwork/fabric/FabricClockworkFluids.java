package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.fabric.SimpleFlowableFluid;
import com.tterrag.registrate.util.entry.FluidEntry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.EmptyItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.valkyrienskies.clockwork.ClockworkMod;

import static net.minecraft.world.item.Items.BUCKET;

public class FabricClockworkFluids {

    public static final FluidEntry<SimpleFlowableFluid.Flowing> VANILLA_FROSTING =
            frostingFluid("vanilla_frosting"/*, NoColorFluidAttributes::new*/)
                    .lang("vanilla_frosting")
                    .properties(p -> p.levelDecreasePerBlock(2)
                            .tickRate(25)
                            .flowSpeed(3)
                            .blastResistance(100f))

                    .source(SimpleFlowableFluid.Still::new) // TODO: remove when Registrate fixes FluidBuilder
                    .bucket()
                    .tag(AllTags.forgeItemTag("buckets/vanilla_frosting"))
                    .build()
                    .renderHandler(() -> SimpleFluidRenderHandler::new)
                    .onRegisterAfter(Item.class, vanilla_frosting -> {
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
                    .properties(p -> p.levelDecreasePerBlock(2)
                            .tickRate(25)
                            .flowSpeed(3)
                            .blastResistance(100f))

                    .source(SimpleFlowableFluid.Still::new) // TODO: remove when Registrate fixes FluidBuilder
                    .bucket()
                    .tag(AllTags.forgeItemTag("buckets/chocolate_frosting"))
                    .build()
                    .renderHandler(() -> SimpleFluidRenderHandler::new)
                    .onRegisterAfter(Item.class, chocolate_frosting -> {
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
                    .properties(p -> p.levelDecreasePerBlock(2)
                            .tickRate(25)
                            .flowSpeed(3)
                            .blastResistance(100f))

                    .source(SimpleFlowableFluid.Still::new) // TODO: remove when Registrate fixes FluidBuilder
                    .bucket()
                    .tag(AllTags.forgeItemTag("buckets/strawberry_frosting"))
                    .build()
                    .renderHandler(() -> SimpleFluidRenderHandler::new)
                    .onRegisterAfter(Item.class, chocolate_frosting -> {
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
    public static void register() {}

//    public static FluidBuilder<SimpleFlowableFluid.Flowing, CreateRegistrate> frostingFluid(String name) {
//        return ClockWorkMod.REGISTRATE.fluid(name, Create.asResource("fluid/frosting_still"), Create.asResource("fluid/frosting_flow"));
//    }

}