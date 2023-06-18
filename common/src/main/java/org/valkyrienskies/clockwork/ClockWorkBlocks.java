package org.valkyrienskies.clockwork;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.AllSections;
import com.simibubi.create.content.contraptions.base.CasingBlock;
import com.simibubi.create.foundation.block.BlockStressDefaults;
import com.simibubi.create.foundation.data.*;
import com.simibubi.create.foundation.utility.Couple;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MaterialColor;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerBlock;
import org.valkyrienskies.clockwork.content.contraptions.ballooner.BalloonerBlock;
import org.valkyrienskies.clockwork.content.contraptions.casing.ExtendedEncasedShaftBlock;
import org.valkyrienskies.clockwork.content.contraptions.combustion_engine.CombustionEngineBlock;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlock;
import org.valkyrienskies.clockwork.content.contraptions.infuser.PhysicsInfuserBlock;
import org.valkyrienskies.clockwork.content.contraptions.intake.IntakeBlock;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlock;
import org.valkyrienskies.clockwork.content.contraptions.propellor.PropellorBearingBlock;
import org.valkyrienskies.clockwork.content.contraptions.reaction_wheel.ReactionWheelBlock;
import org.valkyrienskies.clockwork.content.contraptions.resistor.RedstoneResistorBlock;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.SequencedSeatBlock;
import org.valkyrienskies.clockwork.content.contraptions.solver.SolverBlock;
import org.valkyrienskies.clockwork.content.contraptions.universal_joint.UniversalJointBlock;
import org.valkyrienskies.clockwork.content.physicalities.motion.wing.FlapBlock;
import org.valkyrienskies.clockwork.content.physicalities.motion.wing.WingBlock;
import org.valkyrienskies.clockwork.content.physicalities.motion.wing.DyedWingBlockItem;
import org.valkyrienskies.clockwork.util.builder.BuilderTransformersClockwork;
import org.valkyrienskies.clockwork.util.builder.ClockworkRegistrate;
import org.valkyrienskies.clockwork.util.render.WingBlockItemRenderer;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;
import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class ClockWorkBlocks {

    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkMod.BASE_CREATIVE_TAB);
    }

    static {
        REGISTRATE.startSection(AllSections.KINETICS);
    }

    public static final BlockEntry<PropellorBearingBlock> PROPELLOR_BEARING =
            REGISTRATE.block("propellor_bearing", PropellorBearingBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.PODZOL))
                    .transform(BuilderTransformers.bearing("propellor", "gearbox", false))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .register();

    public static final BlockEntry<PhysBearingBlock> PHYS_BEARING =
            REGISTRATE.block("phys_bearing", PhysBearingBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.lightLevel(PhysBearingBlock::getLight))
                    .properties(p -> p.color(MaterialColor.PODZOL))
                    .addLayer(() -> RenderType::cutout)
                    .blockstate((c, p) -> p.directionalBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .item()
                    .model(AssetLookup.customBlockItemModel("phys_bearing"))
                    .build()
                    .register();
    public static final BlockEntry<FlapBearingBlock> FLAP_BEARING =
            REGISTRATE.block("flap_bearing", FlapBearingBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.PODZOL))
                    .transform(BuilderTransformersClockwork.flapbearing())
                    .transform(BlockStressDefaults.setImpact(4.0))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .register();

    public static final BlockEntry<UniversalJointBlock> UNIVERSAL_JOINT =
            REGISTRATE.block("universal_joint", UniversalJointBlock::new)
                    .initialProperties(SharedProperties::copperMetal)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.METAL))
                    .transform(BlockStressDefaults.setNoImpact())
                    .blockstate((c, p) -> p.directionalBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .item()
                    .model(AssetLookup.customBlockItemModel("universal_joint"))
                    .build()
                    .register();

    public static final BlockEntry<AfterblazerBlock> AFTERBLAZER =
            REGISTRATE.block("afterblazer", AfterblazerBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.color(MaterialColor.COLOR_GRAY))
                    .transform(pickaxeOnly())
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
                    .blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
                    .item()
                    .model(AssetLookup.customBlockItemModel("afterblazer"))
                    .build()
                    .register();

    public static final BlockEntry<IntakeBlock> INTAKE =
            REGISTRATE.block("intake", IntakeBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.color(MaterialColor.COLOR_GRAY))
                    .transform(pickaxeOnly())
                    .addLayer(() -> RenderType::cutoutMipped)
                    .blockstate((c, p) -> p.directionalBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
                    .item()
                    .transform(customItemModel("intake", "item"))
                    .register();

    ////////  REACTION WHEEL ///////

    public static final BlockEntry<ReactionWheelBlock> REACTIONWHEEL =
            REGISTRATE.block("reactionwheel", ReactionWheelBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.COLOR_ORANGE))
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .item()
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<SolverBlock> SOLVER =
            REGISTRATE.block("solver", SolverBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.COLOR_PURPLE))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .transform(BlockStressDefaults.setImpact(4.0))
                    .item()
                    .transform(customItemModel())
                    .register();


    /////// Ballooner ////////

    public static final BlockEntry<BalloonerBlock> BALLOONER =
            REGISTRATE.block("ballooner", BalloonerBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.color(MaterialColor.COLOR_GRAY))
                    .transform(pickaxeOnly())
                    .addLayer(() -> RenderType::cutoutMipped)
                    .item()
                    .transform(customItemModel("ballooner", "item"))
                    .register();


    /////// REDSTONE RESISTOR ////////

    public static final BlockEntry<RedstoneResistorBlock> REDSTONE_RESISTOR =
            REGISTRATE.block("redstone_resistor", RedstoneResistorBlock::new)
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
            REGISTRATE.block("command_seat", SequencedSeatBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.COLOR_LIGHT_GREEN))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .item()
                    .transform(customItemModel("command_seat", "item"))
                    .register();

    //////// Flap Bearing ////////
    public static final BlockEntry<PhysicsInfuserBlock> PHYSICS_INFUSER =
            REGISTRATE.block("physics_infuser", PhysicsInfuserBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.COLOR_PURPLE))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .item()
                    .transform(customItemModel("physics_infuser", "item"))
                    .register();

    static {
        REGISTRATE.startSection(AllSections.CURIOSITIES);
    }

    /////// WINX CLUB //////
    public static final BlockEntry<WingBlock> WING =
            REGISTRATE.block("wing", WingBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.TERRACOTTA_WHITE))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
                    .item(DyedWingBlockItem::new)
                    .transform(ClockworkRegistrate.customRenderedBlockItem(() -> WingBlockItemRenderer::new))
                    .register();
    public static final BlockEntry<FlapBlock> FLAP =
            REGISTRATE.block("flap", FlapBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.TERRACOTTA_WHITE))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
                    .item(DyedWingBlockItem::new)
                    .transform(ClockworkRegistrate.customRenderedBlockItem(() -> WingBlockItemRenderer::new))
                    .register();

    /////// Physics infuser ////////

    public static final BlockEntry<CasingBlock> BALLOON_CASING = REGISTRATE.block("balloon_casing", CasingBlock::new)
            .properties(p -> p.color(MaterialColor.WOOL))
            .properties(p -> p.sound(SoundType.BAMBOO))
            .transform(BuilderTransformers.casing(() -> ClockWorkSpriteShifts.BALLOON_CASING))
            .transform(axeOrPickaxe())

            .register();


    public static final BlockEntry<ExtendedEncasedShaftBlock> BALLOON_ENCASED_SHAFT =
            REGISTRATE.block("balloon_encased_shaft", ExtendedEncasedShaftBlock::balloon)
                    .properties(p -> p.color(MaterialColor.WOOL))
                    .properties(p -> p.sound(SoundType.BAMBOO))
                    .transform(BuilderTransformersClockwork.encasedShaft("balloon", () -> ClockWorkSpriteShifts.BALLOON_CASING))
                    .transform(axeOrPickaxe())
                    .register();


    // COMBUSTION ENGINE //

    public static final BlockEntry<CombustionEngineBlock> COMBUSTION_ENGINE =
            REGISTRATE.block("combustion_engine", CombustionEngineBlock::new)
                    .initialProperties(SharedProperties::copperMetal)
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .transform(pickaxeOnly())
                    .blockstate((c, p) -> p.horizontalFaceBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
                    .transform(BlockStressDefaults.setCapacity(128.0))
                    .transform(BlockStressDefaults.setGeneratorSpeed(() -> Couple.create(0, 128)))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static void register() {
    }
}
