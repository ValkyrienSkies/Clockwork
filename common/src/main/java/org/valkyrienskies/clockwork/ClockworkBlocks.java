package org.valkyrienskies.clockwork;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.fluids.PipeAttachmentModel;
import com.simibubi.create.content.kinetics.BlockStressDefaults;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlock;
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlock;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlock;
import org.valkyrienskies.clockwork.content.contraptions.phys.gyro.GyroBlock;
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlock;
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorBlock;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatBlock;
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonBlock;
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlock;
import org.valkyrienskies.clockwork.content.physicalities.wing.DyedWingBlockItem;
import org.valkyrienskies.clockwork.content.physicalities.wing.FlapBlock;
import org.valkyrienskies.clockwork.content.physicalities.wing.WingBlock;
import org.valkyrienskies.clockwork.renderer.WingBlockItemRenderer;
import org.valkyrienskies.clockwork.util.builder.BuilderTransformersClockwork;
import org.valkyrienskies.clockwork.util.builder.ClockworkRegistrate;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

public class ClockworkBlocks {

    static {
    }

    public static final BlockEntry<PropellerBearingBlock> PROPELLER_BEARING =
            ClockworkMod.INSTANCE.getREGISTRATE().block("propeller_bearing", PropellerBearingBlock::new)
                    .transform(axeOrPickaxe())
                    .transform(BuilderTransformers.bearing("propeller", "gearbox"))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .register();

    public static final BlockEntry<PhysBearingBlock> PHYS_BEARING =
            ClockworkMod.INSTANCE.getREGISTRATE().block("phys_bearing", PhysBearingBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.lightLevel(PhysBearingBlock.Companion::getLight))
                    .addLayer(() -> RenderType::cutout)
                    .blockstate((c, p) -> p.directionalBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .item()
                    .model(AssetLookup.customBlockItemModel("phys_bearing"))
                    .build()
                    .register();
    public static final BlockEntry<FlapBearingBlock> FLAP_BEARING =
            ClockworkMod.INSTANCE.getREGISTRATE().block("flap_bearing", FlapBearingBlock::new)
                    .transform(axeOrPickaxe())
                    .transform(BuilderTransformersClockwork.INSTANCE.flapbearing())
                    .transform(BlockStressDefaults.setImpact(4.0))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .register();

    public static final BlockEntry<DeliveryCannonBlock> DELIVERY_CANNON =
            ClockworkMod.INSTANCE.getREGISTRATE().block("delivery_cannon", DeliveryCannonBlock::new)
                    .initialProperties(SharedProperties::netheriteMetal)
                    .properties(p -> p.sound(SoundType.METAL))
                    .transform(BlockStressDefaults.setImpact(4.0))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .register();

    public static final BlockEntry<DeliveryChuteBlock> DELIVERY_CHUTE =
            ClockworkMod.INSTANCE.getREGISTRATE().block("delivery_chute", DeliveryChuteBlock::new)
                    .initialProperties(SharedProperties::netheriteMetal)
                    .properties(p -> p.sound(SoundType.METAL))
                    .transform(BlockStressDefaults.setImpact(4.0))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .register();

    public static final BlockEntry<AltMeterBlock> ALT_METER =
            ClockworkMod.INSTANCE.getREGISTRATE().block("alt_meter", AltMeterBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .transform(axeOrPickaxe())
                    .addLayer(() -> RenderType::cutout)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .item()
                    .model(AssetLookup.customBlockItemModel("alt_meter"))
                    .build()
                    .register();

    public static final BlockEntry<GyroBlock> GYRO =
            ClockworkMod.INSTANCE.getREGISTRATE().block("gyro", GyroBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .transform(axeOrPickaxe())
                    .addLayer(() -> RenderType::cutout)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .item()
                    .model(AssetLookup.customBlockItemModel("gyro"))
                    .build()
                    .register();


    public static final BlockEntry<RedstoneResistorBlock> REDSTONE_RESISTOR =
            ClockworkMod.INSTANCE.getREGISTRATE().block("redstone_resistor", RedstoneResistorBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .transform(BlockStressDefaults.setNoImpact())
                    .transform(axeOrPickaxe())
                    .addLayer(() -> RenderType::cutoutMipped)
                    .item()
                    .transform(customItemModel("redstone_resistor", "item"))
                    .register();

    /////// Sequenced Seat ////////
    public static final BlockEntry<SequencedSeatBlock> COMMAND_SEAT =
            ClockworkMod.INSTANCE.getREGISTRATE().block("command_seat", SequencedSeatBlock::new)
                    .transform(axeOrPickaxe())
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .item()
                    .transform(customItemModel("command_seat", "item"))
                    .register();



    public static final BlockEntry<WingBlock> WING =
            ClockworkMod.INSTANCE.getREGISTRATE().block("wing", WingBlock::new)
                    .transform(axeOrPickaxe())
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
                    .item(DyedWingBlockItem::new)
                    .transform(ClockworkRegistrate.customRenderedBlockItem(() -> WingBlockItemRenderer::new))
                    .register();

    public static final BlockEntry<FlapBlock> FLAP =
            ClockworkMod.INSTANCE.getREGISTRATE().block("flap", FlapBlock::new)
                    .transform(axeOrPickaxe())
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
                    .item(DyedWingBlockItem::new)
                    .transform(ClockworkRegistrate.customRenderedBlockItem(() -> WingBlockItemRenderer::new))
                    .register();


    public static void register() {
    }
}
