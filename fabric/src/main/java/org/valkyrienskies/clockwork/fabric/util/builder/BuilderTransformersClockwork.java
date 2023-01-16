package org.valkyrienskies.clockwork.fabric.util.builder;


import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.valkyrienskies.clockwork.fabric.ClockWorkModFabric;

public class BuilderTransformersClockwork {

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> infuser() {
        ResourceLocation baseBlockModelLocation = ClockWorkModFabric.asResource("block/physics_infuser/block");
        ResourceLocation baseItemModelLocation = ClockWorkModFabric.asResource("block/physics_infuser/item");
        ResourceLocation liquidTextureLocation = ClockWorkModFabric.asResource("block/physics_infuser/liquid");
        ResourceLocation coreTextureLocation = ClockWorkModFabric.asResource("block/physics_infuser/core");

        return b -> b.initialProperties(SharedProperties::stone)
                .properties(p -> p.noOcclusion())
                .blockstate((c, p) -> p.directionalBlock(c.get(), p.models()
                        .withExistingParent(c.getName(), baseBlockModelLocation)
                        .texture("0", baseBlockModelLocation)
                        .texture("1", coreTextureLocation)
                        .texture("2", liquidTextureLocation)))
                .item()
                .model((c, p) -> p.withExistingParent(c.getName(), baseItemModelLocation)
                        .texture("0", baseBlockModelLocation)
                        .texture("1", coreTextureLocation)
                        .texture("2", liquidTextureLocation))
                .build();
    }
    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> flapbearing() {
        ResourceLocation baseBlockModelLocation = ClockWorkModFabric.asResource("block/flap_bearing/block");
        ResourceLocation baseItemModelLocation = ClockWorkModFabric.asResource("block/bearing/item");
        ResourceLocation topTextureLocation = ClockWorkModFabric.asResource("block/flap_bearing/top");
        ResourceLocation baseTextureLocation = ClockWorkModFabric.asResource("block/flap_bearing");
        return b -> b.initialProperties(SharedProperties::stone)
                .properties(p -> p.noOcclusion())
                .blockstate((c, p) -> p.directionalBlock(c.get(), p.models()
                        .withExistingParent(c.getName(), baseBlockModelLocation)
                        .texture("0", baseBlockModelLocation)
                        .texture("1", topTextureLocation)
                        .texture("2", baseTextureLocation)))
                .item()
                .model((c, p) -> p.withExistingParent(c.getName(), baseItemModelLocation)
                        .texture("0", baseBlockModelLocation)
                        .texture("1", topTextureLocation)
                        .texture("2", baseTextureLocation))
                .build();
    }

}
