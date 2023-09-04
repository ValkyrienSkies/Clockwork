package org.valkyrienskies.clockwork;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.fluids.PipeAttachmentModel;
import com.simibubi.create.content.kinetics.BlockStressDefaults;
import com.simibubi.create.foundation.data.*;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MaterialColor;
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlock;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlock;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlock;
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlock;
import org.valkyrienskies.clockwork.content.kinetics.casing.ExtendedEncasedShaftBlock;
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorBlock;

import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatBlock;
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeBlock;

import org.valkyrienskies.clockwork.content.physicalities.reaction_wheel.ReactionWheelBlock;
import org.valkyrienskies.clockwork.content.physicalities.wing.DyedWingBlockItem;
import org.valkyrienskies.clockwork.content.physicalities.wing.WingBlock;
import org.valkyrienskies.clockwork.content.physicalities.wing.FlapBlock;

import org.valkyrienskies.clockwork.data.CWBlockStateGen;
import org.valkyrienskies.clockwork.renderer.WingBlockItemRenderer;
import org.valkyrienskies.clockwork.util.builder.BuilderTransformersClockwork;
import org.valkyrienskies.clockwork.util.builder.ClockworkRegistrate;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

public class ClockworkBlocks {

    static {
        ClockworkMod.INSTANCE.getREGISTRATE().creativeModeTab(ClockworkMod.INSTANCE::getBASE_CREATIVE_TAB);
    }

    public static final BlockEntry<PropellerBearingBlock> PROPELLER_BEARING =
            ClockworkMod.INSTANCE.getREGISTRATE().block("propeller_bearing", PropellerBearingBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.PODZOL))
                    .transform(BuilderTransformers.bearing("propeller", "gearbox"))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .register();

    public static final BlockEntry<PhysBearingBlock> PHYS_BEARING =
            ClockworkMod.INSTANCE.getREGISTRATE().block("phys_bearing", PhysBearingBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.lightLevel(PhysBearingBlock.Companion::getLight))
                    .properties(p -> p.color(MaterialColor.PODZOL))
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
                    .properties(p -> p.color(MaterialColor.PODZOL))
                    .transform(BuilderTransformersClockwork.INSTANCE.flapbearing())
                    .transform(BlockStressDefaults.setImpact(4.0))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .register();

//    public static final BlockEntry<UniversalJointBlock> UNIVERSAL_JOINT =
//            REGISTRATE.block("universal_joint", UniversalJointBlock::new)
//                    .initialProperties(SharedProperties::copperMetal)
//                    .transform(axeOrPickaxe())
//                    .properties(p -> p.color(MaterialColor.METAL))
//                    .transform(BlockStressDefaults.setNoImpact())
//                    .blockstate((c, p) -> p.directionalBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
//                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
//                    .item()
//                    .model(AssetLookup.customBlockItemModel("universal_joint"))
//                    .build()
//                    .register();

//    public static final BlockEntry<AfterblazerBlock> AFTERBLAZER =
//            ClockworkMod.INSTANCE.getREGISTRATE().block("afterblazer", AfterblazerBlock::new)
//                    .initialProperties(SharedProperties::softMetal)
//                    .properties(p -> p.color(MaterialColor.COLOR_GRAY))
//                    .transform(pickaxeOnly())
//                    .addLayer(() -> RenderType::cutoutMipped)
//                    .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
//                    .blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
//                    .item()
//                    .model(AssetLookup.customBlockItemModel("afterblazer"))
//                    .build()
//                    .register();
//
//    public static final BlockEntry<IntakeBlock> INTAKE =
//            ClockworkMod.INSTANCE.getREGISTRATE().block("intake", IntakeBlock::new)
//                    .initialProperties(SharedProperties::softMetal)
//                    .properties(p -> p.color(MaterialColor.COLOR_GRAY))
//                    .transform(pickaxeOnly())
//                    .addLayer(() -> RenderType::cutoutMipped)
//                    .blockstate((c, p) -> p.directionalBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
//                    .item()
//                    .transform(customItemModel("intake", "item"))
//                    .register();

    ////////  REACTION WHEEL ///////

    public static final BlockEntry<ReactionWheelBlock> REACTIONWHEEL =
            ClockworkMod.INSTANCE.getREGISTRATE().block("reactionwheel", ReactionWheelBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.COLOR_ORANGE))
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .item()
                    .transform(customItemModel())
                    .register();

//    public static final BlockEntry<SolverBlock> SOLVER =
//            ClockworkMod.INSTANCE.getREGISTRATE().block("solver", SolverBlock::new)
//                    .initialProperties(SharedProperties::softMetal)
//                    .transform(axeOrPickaxe())
//                    .properties(p -> p.color(MaterialColor.COLOR_PURPLE))
//                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
//                    .transform(BlockStressDefaults.setImpact(4.0))
//                    .item()
//                    .transform(customItemModel())
//                    .register();


    /////// Ballooner ////////

//    public static final BlockEntry<BalloonerBlock> BALLOONER =
//            ClockworkMod.INSTANCE.getREGISTRATE().block("ballooner", BalloonerBlock::new)
//                    .initialProperties(SharedProperties::softMetal)
//                    .properties(p -> p.color(MaterialColor.COLOR_GRAY))
//                    .transform(pickaxeOnly())
//                    .addLayer(() -> RenderType::cutoutMipped)
//                    .item()
//                    .transform(customItemModel("ballooner", "item"))
//                    .register();


    /////// REDSTONE RESISTOR ////////

    public static final BlockEntry<RedstoneResistorBlock> REDSTONE_RESISTOR =
            ClockworkMod.INSTANCE.getREGISTRATE().block("redstone_resistor", RedstoneResistorBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .properties(p -> p.color(MaterialColor.PODZOL))
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
                    .properties(p -> p.color(MaterialColor.COLOR_LIGHT_GREEN))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .item()
                    .transform(customItemModel("command_seat", "item"))
                    .register();

    //////// Flap Bearing ////////
    public static final BlockEntry<PhysicsInfuserBlock> PHYSICS_INFUSER =
            ClockworkMod.INSTANCE.getREGISTRATE().block("physics_infuser", PhysicsInfuserBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.COLOR_PURPLE))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .item()
                    .transform(customItemModel("physics_infuser", "item"))
                    .register();

    /////// WINX CLUB //////
    public static final BlockEntry<WingBlock> WING =
            ClockworkMod.INSTANCE.getREGISTRATE().block("wing", WingBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.TERRACOTTA_WHITE))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
                    .item(DyedWingBlockItem::new)
                    .transform(ClockworkRegistrate.customRenderedBlockItem(() -> WingBlockItemRenderer::new))
                    .register();
    public static final BlockEntry<FlapBlock> FLAP =
            ClockworkMod.INSTANCE.getREGISTRATE().block("flap", FlapBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.TERRACOTTA_WHITE))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
                    .item(DyedWingBlockItem::new)
                    .transform(ClockworkRegistrate.customRenderedBlockItem(() -> WingBlockItemRenderer::new))
                    .register();

    /////// Physics infuser ////////

    public static final BlockEntry<CasingBlock> BALLOON_CASING = ClockworkMod.INSTANCE.getREGISTRATE().block("balloon_casing", CasingBlock::new)
            .properties(p -> p.color(MaterialColor.WOOL))
            .properties(p -> p.sound(SoundType.BAMBOO))
            .transform(BuilderTransformers.casing(ClockworkSpriteShifts.INSTANCE::getBALLOON_CASING))
            .transform(axeOrPickaxe())

            .register();


    public static final BlockEntry<ExtendedEncasedShaftBlock> BALLOON_ENCASED_SHAFT =
            ClockworkMod.INSTANCE.getREGISTRATE().block("balloon_encased_shaft", ExtendedEncasedShaftBlock.Companion::balloon)
                    .properties(p -> p.color(MaterialColor.WOOL))
                    .properties(p -> p.sound(SoundType.BAMBOO))
                    .transform(BuilderTransformersClockwork.INSTANCE.encasedShaft("balloon", () -> ClockworkSpriteShifts.INSTANCE.getBALLOON_CASING()))
                    .transform(EncasingRegistry.addVariantTo(AllBlocks.SHAFT))
                    .transform(axeOrPickaxe())
                    .register();


    // COMBUSTION ENGINE //

//    public static final BlockEntry<CombustionEngineBlock> COMBUSTION_ENGINE =
//            ClockworkMod.INSTANCE.getREGISTRATE().block("combustion_engine", CombustionEngineBlock::new)
//                    .initialProperties(SharedProperties::copperMetal)
//                    .properties(BlockBehaviour.Properties::noOcclusion)
//                    .transform(pickaxeOnly())
//                    .blockstate((c, p) -> p.horizontalFaceBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
//                    .transform(BlockStressDefaults.setCapacity(128.0))
//                    .transform(BlockStressDefaults.setGeneratorSpeed(() -> Couple.create(0, 128)))
//                    .item()
//                    .transform(customItemModel())
//                    .register();

    public static final BlockEntry<HeatPipeBlock> HEAT_PIPE = ClockworkMod.INSTANCE.getREGISTRATE().block("heat_pipe", HeatPipeBlock::new)
            .initialProperties(SharedProperties::netheriteMetal)
            .blockstate(CWBlockStateGen.INSTANCE.pipe())
            .onRegister(CreateRegistrate.blockModel(() -> PipeAttachmentModel::new))
            .item()
            .transform(customItemModel())
            .register();

    public static void register() {
    }
}