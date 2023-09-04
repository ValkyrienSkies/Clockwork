package org.valkyrienskies.clockwork

import com.simibubi.create.AllBlocks
import com.simibubi.create.AllTags
import com.simibubi.create.content.decoration.encasing.CasingBlock
import com.simibubi.create.content.decoration.encasing.EncasingRegistry
import com.simibubi.create.content.fluids.PipeAttachmentModel
import com.simibubi.create.content.kinetics.BlockStressDefaults
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry
import com.simibubi.create.foundation.data.*
import com.simibubi.create.foundation.utility.Couple
import com.tterrag.registrate.util.entry.BlockEntry
import com.tterrag.registrate.util.nullness.NonNullFunction
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.Material
import net.minecraft.world.level.material.MaterialColor
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlock
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlock
import org.valkyrienskies.clockwork.content.kinetics.casing.ExtendedEncasedShaftBlock
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorBlock
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatBlock
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeBlock
import org.valkyrienskies.clockwork.content.physicalities.reaction_wheel.ReactionWheelBlock
import org.valkyrienskies.clockwork.content.physicalities.wing.DyedWingBlockItem
import org.valkyrienskies.clockwork.content.physicalities.wing.FlapBlock
import org.valkyrienskies.clockwork.content.physicalities.wing.WingBlock
import org.valkyrienskies.clockwork.data.CWBlockStateGen
import org.valkyrienskies.clockwork.renderer.WingBlockItemRenderer
import org.valkyrienskies.clockwork.util.builder.BuilderTransformersClockwork
import org.valkyrienskies.clockwork.util.builder.ClockworkRegistrate
import java.util.function.Supplier


object ClockworkBlocks {
    init {
        REGISTRATE.creativeModeTab { ClockworkMod.BASE_CREATIVE_TAB }
    }

    val PROPELLOR_BEARING: BlockEntry<PropellerBearingBlock> =
        REGISTRATE.block(java.lang.String("propeller_bearing"), ::PropellerBearingBlock)
            .transform(TagGen.axeOrPickaxe())
            .properties { p -> p.color(MaterialColor.PODZOL) }
            .transform(BuilderTransformers.bearing("propeller", "gearbox"))
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .register()

    val PHYS_BEARING: BlockEntry<PhysBearingBlock> = REGISTRATE.block(java.lang.String("phys_bearing"), ::PhysBearingBlock)
        .initialProperties { SharedProperties.stone() }
        .transform(TagGen.axeOrPickaxe())
        .properties { p -> p.lightLevel(PhysBearingBlock::getLight) }
        .properties { p -> p.color(MaterialColor.PODZOL) }
        .addLayer { Supplier(RenderType::cutout) }
        .blockstate { c, p ->
            p.directionalBlock(
                c.getEntry(),
                AssetLookup.partialBaseModel(c, p)
            )
        }
        .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
        .item()
        .model(AssetLookup.customBlockItemModel("phys_bearing"))
        .build()
        .register()
    val FLAP_BEARING: BlockEntry<FlapBearingBlock> = REGISTRATE.block(java.lang.String("flap_bearing"), ::FlapBearingBlock)
        .transform(TagGen.axeOrPickaxe())
        .properties { p -> p.color(MaterialColor.PODZOL) }
        .transform(BuilderTransformersClockwork.flapbearing())
        .transform(BlockStressDefaults.setImpact(4.0))
        .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
        .register()

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
//    val AFTERBLAZER: BlockEntry<AfterblazerBlock> = REGISTRATE.block("afterblazer") { AfterblazerBlock() }
//        .initialProperties { SharedProperties.softMetal() }
//        .properties { p -> p.color(MaterialColor.COLOR_GRAY) }
//        .transform(TagGen.pickaxeOnly())
//        .addLayer { RenderType::cutoutMipped }
//        .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
//        .blockstate { c, p ->
//            p.simpleBlock(
//                c.getEntry(),
//                AssetLookup.partialBaseModel(c, p)
//            )
//        }
//        .item()
//        .model(AssetLookup.customBlockItemModel("afterblazer"))
//        .build()
//        .register()
//    val INTAKE: BlockEntry<IntakeBlock> = REGISTRATE.block("intake") { IntakeBlock() }
//        .initialProperties { SharedProperties.softMetal() }
//        .properties { p -> p.color(MaterialColor.COLOR_GRAY) }
//        .transform(TagGen.pickaxeOnly())
//        .addLayer { RenderType::cutoutMipped }
//        .blockstate { c, p ->
//            p.directionalBlock(
//                c.getEntry(),
//                AssetLookup.partialBaseModel(c, p)
//            )
//        }
//        .item()
//        .transform(ModelGen.customItemModel("intake", "item"))
//        .register()

    ////////  REACTION WHEEL ///////
    val REACTIONWHEEL: BlockEntry<ReactionWheelBlock> = REGISTRATE.block(java.lang.String("reaction_wheel"), ::ReactionWheelBlock)
        .initialProperties { SharedProperties.softMetal() }
        .transform(TagGen.axeOrPickaxe())
        .properties { p -> p.color(MaterialColor.COLOR_ORANGE) }
        .properties {BlockBehaviour.Properties.of(Material.HEAVY_METAL).noOcclusion() }
        .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
        .item()
        .transform(ModelGen.customItemModel())
        .register()

//    val SOLVER: BlockEntry<SolverBlock> = REGISTRATE.block("solver") { SolverBlock() }
//        .initialProperties { SharedProperties.softMetal() }
//        .transform(TagGen.axeOrPickaxe())
//        .properties { p -> p.color(MaterialColor.COLOR_PURPLE) }
//        .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
//        .transform(BlockStressDefaults.setImpact(4.0))
//        .item()
//        .transform(ModelGen.customItemModel())
//        .register()

    /////// Ballooner ////////
//    val BALLOONER: BlockEntry<BalloonerBlock> = REGISTRATE.block("ballooner") { BalloonerBlock() }
//        .initialProperties { SharedProperties.softMetal() }
//        .properties { p -> p.color(MaterialColor.COLOR_GRAY) }
//        .transform(TagGen.pickaxeOnly())
//        .addLayer { RenderType::cutoutMipped }
//        .item()
//        .transform(ModelGen.customItemModel("ballooner", "item"))
//        .register()

    /////// REDSTONE RESISTOR ////////
    val REDSTONE_RESISTOR: BlockEntry<RedstoneResistorBlock> = REGISTRATE.block(java.lang.String("redstone_resistor"), ::RedstoneResistorBlock)
            .initialProperties { SharedProperties.stone() }
            .properties { BlockBehaviour.Properties.of(Material.STONE).noOcclusion() }
            .properties { p -> p.color(MaterialColor.PODZOL) }
            .transform(BlockStressDefaults.setNoImpact())
            .transform(TagGen.axeOrPickaxe())
            .addLayer { Supplier(RenderType::cutoutMipped) }
            .item()
            .transform(ModelGen.customItemModel("redstone_resistor", "item"))
            .register()

    /////// Sequenced Seat ////////
    val COMMAND_SEAT: BlockEntry<SequencedSeatBlock> = REGISTRATE.block(java.lang.String("command_seat"), ::SequencedSeatBlock)
        .transform(TagGen.axeOrPickaxe())
        .properties { p -> p.color(MaterialColor.COLOR_LIGHT_GREEN) }
        .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
        .item()
        .transform(ModelGen.customItemModel("command_seat", "item"))
        .register()

    //////// Flap Bearing ////////
    val PHYSICS_INFUSER: BlockEntry<PhysicsInfuserBlock> = REGISTRATE.block(java.lang.String("physics_infuser"), ::PhysicsInfuserBlock)
        .transform(TagGen.axeOrPickaxe())
        .properties { p -> p.color(MaterialColor.COLOR_PURPLE) }
        .addLayer { Supplier(RenderType::cutoutMipped) }
        .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
        .item()
        .transform(ModelGen.customItemModel("physics_infuser", "item"))
        .register()

    /////// WINX CLUB //////
    val WING: BlockEntry<WingBlock> = REGISTRATE.block(java.lang.String("wing"), ::WingBlock)
        .transform(TagGen.axeOrPickaxe())
        .properties { p -> p.color(MaterialColor.TERRACOTTA_WHITE) }
        .addLayer { Supplier(RenderType::cutoutMipped) }
        .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
        .item(::DyedWingBlockItem)
        .transform(ClockworkRegistrate.customRenderedBlockItem { { WingBlockItemRenderer() } })
        .register()
    val FLAP: BlockEntry<FlapBlock> = REGISTRATE.block(java.lang.String("flap"), ::FlapBlock)
        .transform(TagGen.axeOrPickaxe())
        .properties { p -> p.color(MaterialColor.TERRACOTTA_WHITE) }
        .addLayer { Supplier(RenderType::cutoutMipped) }
        .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
        .item(::DyedWingBlockItem)
        .transform(ClockworkRegistrate.customRenderedBlockItem { { WingBlockItemRenderer() } })
        .register()

    val BALLOON_ENCASED_SHAFT: BlockEntry<ExtendedEncasedShaftBlock> =
        REGISTRATE.block(java.lang.String("balloon_encased_shaft"), ExtendedEncasedShaftBlock::balloon)
            .properties { p -> p.color(MaterialColor.WOOL) }
            .properties { p -> p.sound(SoundType.BAMBOO) }
            .transform(BuilderTransformersClockwork.encasedShaft("balloon") { ClockworkSpriteShifts.BALLOON_CASING })
            .transform(EncasingRegistry.addVariantTo(AllBlocks.SHAFT))
            .transform(TagGen.axeOrPickaxe())
            .register()

    // COMBUSTION ENGINE //
//    val COMBUSTION_ENGINE: BlockEntry<CombustionEngineBlock> =
//        REGISTRATE.block("combustion_engine") { CombustionEngineBlock() }
//            .initialProperties { SharedProperties.copperMetal() }
//            .properties { BlockBehaviour.Properties.noOcclusion() }
//            .transform(TagGen.pickaxeOnly())
//            .blockstate { c, p ->
//                p.horizontalFaceBlock(
//                    c.get(),
//                    AssetLookup.partialBaseModel(c, p)
//                )
//            }
//            .transform(BlockStressDefaults.setCapacity(128.0))
//            .transform(BlockStressDefaults.setGeneratorSpeed(Supplier<Couple<Int>> {
//                Couple.create(
//                    0,
//                    128
//                )
//            }))
//            .item()
//            .transform(ModelGen.customItemModel())
//            .register()

    val HEAT_PIPE: BlockEntry<HeatPipeBlock> = REGISTRATE.block(java.lang.String("heat_pipe"), ::HeatPipeBlock )
        .initialProperties { SharedProperties.netheriteMetal() }
        .blockstate(CWBlockStateGen.pipe())
        .onRegister(CreateRegistrate.blockModel(Supplier<NonNullFunction<BakedModel?, out BakedModel>> {
            NonNullFunction<BakedModel?, BakedModel> { template: BakedModel? ->
                PipeAttachmentModel(
                    template
                )
            }
        }))
        .item()
        .transform(ModelGen.customItemModel())
        .register()

    fun register() {}
}