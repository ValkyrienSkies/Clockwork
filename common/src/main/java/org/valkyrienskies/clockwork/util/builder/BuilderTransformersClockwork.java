package org.valkyrienskies.clockwork.util.builder;


import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.contraptions.relays.encased.EncasedCTBehaviour;
import com.simibubi.create.foundation.block.BlockStressDefaults;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.fabric.SimpleFlowableFluid;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.content.contraptions.casing.ExtendedEncasedShaftBlock;

import java.util.function.Supplier;

import static com.simibubi.create.foundation.data.BlockStateGen.axisBlock;


public class BuilderTransformersClockwork {


    private static <B extends RotatedPillarKineticBlock, P> BlockBuilder<B, P> encasedBase(BlockBuilder<B, P> b,
                                                                                           Supplier<ItemLike> drop) {
        return b.initialProperties(SharedProperties::stone)
                .properties(BlockBehaviour.Properties::noOcclusion)
                .transform(BlockStressDefaults.setNoImpact())
                .loot((p, lb) -> p.dropOther(lb, drop.get()));
    }

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> infuser() {
        ResourceLocation baseBlockModelLocation = ClockWorkMod.asResource("block/physics_infuser/block");
        ResourceLocation baseItemModelLocation = ClockWorkMod.asResource("block/physics_infuser/item");
        ResourceLocation liquidTextureLocation = ClockWorkMod.asResource("block/physics_infuser/liquid");
        ResourceLocation coreTextureLocation = ClockWorkMod.asResource("block/physics_infuser/core");

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
        ResourceLocation baseBlockModelLocation = ClockWorkMod.asResource("block/flap_bearing/block");
        ResourceLocation baseItemModelLocation = ClockWorkMod.asResource("block/bearing/item");
        ResourceLocation topTextureLocation = ClockWorkMod.asResource("block/flap_bearing/top");
        ResourceLocation baseTextureLocation = ClockWorkMod.asResource("block/flap_bearing");
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

    public static <B extends ExtendedEncasedShaftBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedShaft(String casing,
                                                                                                                 Supplier<CTSpriteShiftEntry> casingShift) {
        return builder -> encasedBase(builder, () -> AllBlocks.SHAFT.get())
                .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(casingShift.get())))
                .onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.make(block, casingShift.get(),
                        (s, f) -> f.getAxis() != s.getValue(ExtendedEncasedShaftBlock.AXIS))))
                .blockstate((c, p) -> axisBlock(c, p, blockState -> p.models()
                        .getExistingFile(p.modLoc("block/encased_shaft/block_" + casing)), true))
                .item()
                .model(AssetLookup.customBlockItemModel("encased_shaft", "item_" + casing))
                .build();
    }

}
