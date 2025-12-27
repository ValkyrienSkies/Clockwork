package org.valkyrienskies.clockwork

import com.simibubi.create.AllBlocks
import com.simibubi.create.AllSpriteShifts
import com.simibubi.create.AllTags
import com.simibubi.create.api.behaviour.movement.MovementBehaviour.movementBehaviour
import com.simibubi.create.content.decoration.encasing.CasingBlock
import com.simibubi.create.content.decoration.encasing.EncasingRegistry
import com.simibubi.create.content.fluids.PipeAttachmentModel
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock
import com.simibubi.create.foundation.data.*
import com.simibubi.create.foundation.data.CreateRegistrate.connectedTextures
import com.simibubi.create.foundation.data.ModelGen.customItemModel
import com.simibubi.create.foundation.data.TagGen.axeOrPickaxe
import com.tterrag.registrate.builders.BlockBuilder
import com.tterrag.registrate.providers.DataGenContext
import com.tterrag.registrate.providers.RegistrateBlockstateProvider
import com.tterrag.registrate.util.entry.BlockEntry
import com.tterrag.registrate.util.nullness.NonNullFunction
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE
import org.valkyrienskies.clockwork.client.render.WingBlockItemRenderer
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlock
import org.valkyrienskies.clockwork.content.contraptions.flap.smart_flap.SmartFlapBearingBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlock
import org.valkyrienskies.clockwork.content.physicalities.goo.GooBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.SlickerBlock
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlock
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeControllerBlock
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeControllerMovementBehaviour
import org.valkyrienskies.clockwork.content.contraptions.propeller.copter.CopterBearingBlock
import org.valkyrienskies.clockwork.content.curiosities.GenericWanderliteBlock
import org.valkyrienskies.clockwork.content.curiosities.GenericWanderliteSlab
import org.valkyrienskies.clockwork.content.curiosities.GenericWanderliteStairs
import org.valkyrienskies.clockwork.content.curiosities.WanderliteOreBlock
import org.valkyrienskies.clockwork.content.curiosities.altmeter.AltMeterBlock
import org.valkyrienskies.clockwork.content.curiosities.meteor.MeteorTestBlock
import org.valkyrienskies.clockwork.content.curiosities.clock.ClockBlock
import org.valkyrienskies.clockwork.content.curiosities.sensor.distance.DistanceSensorBlock
import org.valkyrienskies.clockwork.content.curiosities.sensor.impact.ImpactSensorBlock
import org.valkyrienskies.clockwork.content.curiosities.sensor.rotation.GyroscopicSensorBlock
import org.valkyrienskies.clockwork.content.curiosities.sensor.rotation.LodefocusBlock
import org.valkyrienskies.clockwork.content.kinetics.casing.ExtendedEncasedShaftBlock
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorBlock
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatBlock
import org.valkyrienskies.clockwork.content.kinetics.universal_shaft.UniversalShaftBlock
import org.valkyrienskies.clockwork.content.logistics.gas.backtank.GasBacktankBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock
import org.valkyrienskies.clockwork.content.logistics.gas.engine.GasEngineBlock
import org.valkyrienskies.clockwork.content.logistics.gas.exhaust.ExhaustBlock
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCrafterBlock
import org.valkyrienskies.clockwork.content.logistics.gas.generation.coal_burner.CoalBurnerBlock
import org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor.AirCompressorBlock
import org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator.CreativeGeneratorBlock
import org.valkyrienskies.clockwork.content.logistics.gas.generation.steam_generator.SteamGeneratorBlock
import org.valkyrienskies.clockwork.content.logistics.gas.heater.GasHeaterBlock
import org.valkyrienskies.clockwork.content.logistics.gas.hoseport.HosePortBlock
import org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle.GasNozzleBlock
import org.valkyrienskies.clockwork.content.logistics.gas.pump.PumpDuctBlock
import org.valkyrienskies.clockwork.content.logistics.gas.redstone.RedstoneDuctBlock
import org.valkyrienskies.clockwork.content.logistics.gas.storage.tank.DuctTankBlock
import org.valkyrienskies.clockwork.content.logistics.gas.storage.tank.DuctTankCTBehaviour
import org.valkyrienskies.clockwork.content.logistics.gas.storage.tank.DuctTankModel
import org.valkyrienskies.clockwork.content.logistics.gas.valve.ValveDuctBlock
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonBlock
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlock
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonBlock
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.GasThrusterBlock
import org.valkyrienskies.clockwork.content.physicalities.gyro.GyroBlock
import org.valkyrienskies.clockwork.content.physicalities.reactionwheel.ReactionWheelBlock
import org.valkyrienskies.clockwork.content.physicalities.spinoff_bearing.SpinoffBearingBlock
import org.valkyrienskies.clockwork.content.physicalities.wing.DyedWingBlockItem
import org.valkyrienskies.clockwork.content.physicalities.wing.FlapBlock
import org.valkyrienskies.clockwork.content.physicalities.wing.WingBlock
import org.valkyrienskies.clockwork.content.propulsion.sugar_rocket.SugarRocketBlock
import org.valkyrienskies.clockwork.util.builder.BuilderTransformersClockwork
import org.valkyrienskies.clockwork.util.builder.ClockworkRegistrate
import java.util.function.Supplier


object ClockworkBlocks {

    @JvmField
    val SUGAR_ROCKET: BlockEntry<SugarRocketBlock> =
        REGISTRATE.block<SugarRocketBlock>("sugar_rocket") { properties: BlockBehaviour.Properties? ->
            SugarRocketBlock(properties!!)
        }
            .initialProperties(AllBlocks.BELT)
            .transform(axeOrPickaxe())
            .properties { it.instabreak().explosionResistance(0f).noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val BRASS_PROPELLER_BEARING: BlockEntry<PropellerBearingBlock> =
        REGISTRATE.block<PropellerBearingBlock>("brass_propeller_bearing") { properties: BlockBehaviour.Properties? ->
            PropellerBearingBlock(properties!!)
        }
            .transform(axeOrPickaxe())
            //.transform(BuilderTransformersClockwork.bearing("propeller", "gearbox"))
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .addLayer { Supplier { RenderType.cutoutMipped() } }
            .properties { it.noOcclusion() }
            .item()
            .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val COPTER_BEARING: BlockEntry<CopterBearingBlock> =
        REGISTRATE.block<CopterBearingBlock>("copter_bearing") { properties: BlockBehaviour.Properties? ->
            CopterBearingBlock(properties!!)
        }
            .transform(axeOrPickaxe())
            //.transform(BuilderTransformersClockwork.bearing("propeller", "gearbox"))
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .addLayer { Supplier { RenderType.cutoutMipped() } }
            .properties { it.noOcclusion() }
            .item()
            .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val JURYRIGGED_PROPELLER_BEARING: BlockEntry<PropellerBearingBlock> =
        REGISTRATE.block("juryrigged_propeller_bearing") { properties: BlockBehaviour.Properties? ->
            PropellerBearingBlock(properties!!)
        }
            .transform(axeOrPickaxe())
            //.transform(BuilderTransformersClockwork.bearing("propeller", "gearbox"))
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .addLayer { Supplier { RenderType.cutoutMipped() } }
            .properties { it.noOcclusion() }
            .item()
            .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val BLADE_CONTROLLER: BlockEntry<BladeControllerBlock> =
        REGISTRATE.block<BladeControllerBlock>("blade_controller") { properties: BlockBehaviour.Properties? ->
            BladeControllerBlock(properties!!)
        }
            .transform(axeOrPickaxe())
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutoutMipped() } }
            .onRegister(movementBehaviour(BladeControllerMovementBehaviour()))
            .item()
            .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val PHYS_BEARING: BlockEntry<PhysBearingBlock> =
        REGISTRATE.block<PhysBearingBlock>("phys_bearing") { properties: BlockBehaviour.Properties? ->
            PhysBearingBlock(properties!!)
        }
            .initialProperties { SharedProperties.stone() }
            .transform(axeOrPickaxe())
            .properties {
                it.lightLevel { state: BlockState? -> PhysBearingBlock.getLight(state) }
            }
            .addLayer { Supplier { RenderType.cutout() } }
            .blockstate { c: DataGenContext<Block?, PhysBearingBlock>, p: RegistrateBlockstateProvider ->
                p.directionalBlock(c.entry, AssetLookup.partialBaseModel(c, p))
            }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
            .model(AssetLookup.customBlockItemModel("phys_bearing"))
            .build()
            .register()

    @JvmField
    val ANDESITE_FLAP_BEARING: BlockEntry<FlapBearingBlock> =
        REGISTRATE.block<FlapBearingBlock>("andesite_flap_bearing") { properties: BlockBehaviour.Properties? ->
            FlapBearingBlock(properties)
        }
            .transform(axeOrPickaxe())
            .transform(ClockworkStress.setImpact(4.0))
            .addLayer { Supplier { RenderType.cutout() } }
            .properties { it.noOcclusion() }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val SMART_FLAP_BEARING: BlockEntry<SmartFlapBearingBlock> =
        REGISTRATE.block<SmartFlapBearingBlock>("smart_flap_bearing") { properties: BlockBehaviour.Properties? ->
            SmartFlapBearingBlock(properties)
        }
            .transform(axeOrPickaxe())
            //.transform(flapbearing())
            .transform(ClockworkStress.setImpact(4.0))
            .addLayer { Supplier { RenderType.cutout() } }
            .properties { it.noOcclusion() }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val ALT_METER: BlockEntry<AltMeterBlock> =
        REGISTRATE.block<AltMeterBlock>("alt_meter") { properties: BlockBehaviour.Properties? ->
            AltMeterBlock(properties!!)
        }
            .initialProperties { SharedProperties.stone() }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val DISTANCE_SENSOR: BlockEntry<DistanceSensorBlock> =
        REGISTRATE.block<DistanceSensorBlock>("distance_sensor") { properties: BlockBehaviour.Properties? ->
            DistanceSensorBlock(properties!!)
        }
            .initialProperties { SharedProperties.stone() }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val GYROSCOPIC_SENSOR: BlockEntry<GyroscopicSensorBlock> = REGISTRATE.block<GyroscopicSensorBlock>("gyroscopic_sensor") { properties: BlockBehaviour.Properties? ->
        GyroscopicSensorBlock(properties!!)
    }
        .initialProperties { SharedProperties.stone() }
        .transform(axeOrPickaxe())
        .properties { it.noOcclusion() }
        .addLayer { Supplier { RenderType.cutout() } }
        .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
        .item()
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val LODEFOCUS: BlockEntry<LodefocusBlock> = REGISTRATE.block<LodefocusBlock>("lodefocus") { properties: BlockBehaviour.Properties? ->
        LodefocusBlock(properties!!)
    }
        .initialProperties { Blocks.GLASS }
        .transform(axeOrPickaxe())
        .properties { it.noOcclusion() ; it.lightLevel{ _ -> 1 } ; it.isViewBlocking{ _, _, _ -> false } }
        .addLayer { Supplier { RenderType.cutout() } }
        .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
        .tag(ClockworkTags.AllBlockTags.SENSOR_LENS.tag)
        .item()
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val IMPACT_SENSOR: BlockEntry<ImpactSensorBlock> =
        REGISTRATE.block<ImpactSensorBlock>("impact_sensor") { properties: BlockBehaviour.Properties? ->
            ImpactSensorBlock(properties!!)
        }
            .initialProperties { SharedProperties.stone() }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val GYRO: BlockEntry<GyroBlock> = REGISTRATE.block<GyroBlock>("gyro") { properties: BlockBehaviour.Properties? ->
        GyroBlock(properties!!)
    }
        .initialProperties { SharedProperties.stone() }
        .properties {
            it.noOcclusion()
        }
        .transform(axeOrPickaxe())
        .addLayer { Supplier { RenderType.cutout() } }
        .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
        .item()
        .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
        // I have no idea why the item model is still fucked when the block model is working
        .model(AssetLookup.customBlockItemModel("gyro"))
        .build()
        .register()

    @JvmField
    val REACTIONWHEEL: BlockEntry<ReactionWheelBlock> = REGISTRATE.block<ReactionWheelBlock>(
        "reactionwheel"
    ) { properties: BlockBehaviour.Properties? ->
        ReactionWheelBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.stone() }
        .transform(axeOrPickaxe())
        .addLayer { Supplier { RenderType.cutout() } }
        .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
        .item()
        .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
        .model(AssetLookup.customBlockItemModel("reactionwheel"))
        .build()
        .register()

    @JvmField
    val REDSTONE_RESISTOR: BlockEntry<RedstoneResistorBlock> =
        REGISTRATE.block<RedstoneResistorBlock>("redstone_resistor") { properties: BlockBehaviour.Properties? ->
            RedstoneResistorBlock(properties!!)
        }
            .initialProperties { SharedProperties.stone() }
            .properties {
                it.noOcclusion()
            }
            .transform<Block, RedstoneResistorBlock, CreateRegistrate, BlockBuilder<RedstoneResistorBlock, CreateRegistrate>>(
                ClockworkStress.setNoImpact()
            )
            .transform<Block, RedstoneResistorBlock, CreateRegistrate, BlockBuilder<RedstoneResistorBlock, CreateRegistrate>>(
                axeOrPickaxe()
            )
            .addLayer { Supplier { RenderType.cutoutMipped() } }
            .item()
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .transform(
                customItemModel<BlockItem, BlockBuilder<RedstoneResistorBlock, CreateRegistrate>>(
                    "redstone_resistor",
                    "item"
                )
            )
            .register()

    @JvmField
    val COMMAND_SEAT: BlockEntry<SequencedSeatBlock> =
        REGISTRATE.block<SequencedSeatBlock>("command_seat") { properties: BlockBehaviour.Properties? ->
            SequencedSeatBlock(properties!!)
        }
            .properties {
                it.noOcclusion()
            }
            .transform(axeOrPickaxe())
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            //.tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .transform(customItemModel("command_seat", "item"))
            .register()

    @JvmField
    val WING: BlockEntry<WingBlock> = REGISTRATE.block<WingBlock>("wing") { properties: BlockBehaviour.Properties? ->
        WingBlock(properties)
    }
        .transform<Block, WingBlock, CreateRegistrate, BlockBuilder<WingBlock, CreateRegistrate>>(axeOrPickaxe())
        .addLayer { Supplier { RenderType.cutoutMipped() } }
        .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
        .item { block: WingBlock?, properties: Item.Properties? ->
            DyedWingBlockItem(block, properties)
        }
        .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
        .transform(ClockworkRegistrate.customRenderedBlockItem<DyedWingBlockItem, BlockBuilder<WingBlock, CreateRegistrate>> { Supplier { WingBlockItemRenderer(
            ClockworkPartials.WING_FRAME_ITEM) } })
        .register()

    @JvmField
    val FLAP: BlockEntry<FlapBlock> = REGISTRATE.block<FlapBlock>("flap") { properties: BlockBehaviour.Properties? ->
        FlapBlock(properties)
    }
        .transform(axeOrPickaxe())
        .addLayer { Supplier { RenderType.cutoutMipped() } }
        .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
        .item { block: FlapBlock?, properties: Item.Properties? ->
            DyedWingBlockItem(
                block,
                properties
            )
        }
        .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
        .transform(ClockworkRegistrate.customRenderedBlockItem { Supplier { WingBlockItemRenderer(
            ClockworkPartials.FLAP_FRAME_ITEM) } })
        .register()

    @JvmField
    val PHYSICS_INFUSER: BlockEntry<PhysicsInfuserBlock> =
        REGISTRATE.block<PhysicsInfuserBlock>("physics_infuser") { properties: BlockBehaviour.Properties? ->
            PhysicsInfuserBlock(properties!!)
        }
            .transform<Block, PhysicsInfuserBlock, CreateRegistrate, BlockBuilder<PhysicsInfuserBlock, CreateRegistrate>>(
                axeOrPickaxe()
            )

            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
            .transform(customItemModel("physics_infuser", "item"))
            .register()

    @JvmField
    val DUCT: BlockEntry<DuctBlock> = REGISTRATE.block<DuctBlock>(
        "duct"
    ) { properties: BlockBehaviour.Properties? ->
        DuctBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .onRegister(CreateRegistrate.blockModel {
            NonNullFunction<BakedModel?, BakedModel> { template: BakedModel? ->
                PipeAttachmentModel(
                    template,
                    false
                )
            }
        })
        .item()
        .tab(ClockworkMod.GAS_CREATIVE_TABINFO)
        .transform(customItemModel())
        .register()

    @JvmField
    val COAL_BURNER: BlockEntry<CoalBurnerBlock> = REGISTRATE.block<CoalBurnerBlock>(
        "coal_burner"
    ) { properties: BlockBehaviour.Properties? ->
        CoalBurnerBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .addLayer { Supplier { RenderType.cutout() } }
        .properties { it.noOcclusion() }
        .item()
        .tab(ClockworkMod.GAS_CREATIVE_TABINFO)
        .build()
        .register()




    @JvmField
    val DUCT_TANK: BlockEntry<DuctTankBlock> = REGISTRATE.block(
        "duct_tank"
    ) { properties: BlockBehaviour.Properties? ->
        DuctTankBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .addLayer { Supplier { RenderType.cutout() } }
        .properties { it.noOcclusion() }
        //.onRegister(connectedTextures { DuctTankCTBehaviour(ClockworkSpriteShifts.DUCT_TANK, ClockworkSpriteShifts.DUCT_TANK_TOP, AllSpriteShifts.FLUID_TANK_INNER) })
        .onRegister(ClockworkRegistrate.blockModel { NonNullFunction<BakedModel, BakedModel> { originalModel: BakedModel -> DuctTankModel(originalModel) } })
        .item()
        .tab(ClockworkMod.GAS_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val AIR_COMPRESSOR: BlockEntry<AirCompressorBlock> = REGISTRATE.block<AirCompressorBlock>(
        "air_compressor"
    ) { properties: BlockBehaviour.Properties? ->
        AirCompressorBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .addLayer { Supplier { RenderType.cutout() } }
        .transform(ClockworkStress.setImpact(4.0))
        .properties { it.noOcclusion() }
        .item()
        .tab(ClockworkMod.GAS_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val PUMP_DUCT: BlockEntry<PumpDuctBlock> = REGISTRATE.block<PumpDuctBlock>(
        "pump_duct"
    ) { properties: BlockBehaviour.Properties? ->
        PumpDuctBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .addLayer { Supplier { RenderType.cutoutMipped() } }
        .properties { it.noOcclusion() }
        .item()
        .tab(ClockworkMod.GAS_CREATIVE_TABINFO)
        .model(AssetLookup.customBlockItemModel("pump", "item"))
        .transform(customItemModel())
        .transform(ClockworkStress.setImpact(4.0))
        .register()

    @JvmField
    val REDSTONE_DUCT: BlockEntry<RedstoneDuctBlock> = REGISTRATE.block<RedstoneDuctBlock>(
        "redstone_duct"
    ) { properties: BlockBehaviour.Properties? ->
        RedstoneDuctBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .addLayer { Supplier { RenderType.cutoutMipped() } }
        .properties { it.noOcclusion() }
//        .blockstate { c: DataGenContext<Block?, RedstoneDuctBlock>, p: RegistrateBlockstateProvider ->
//            BlockStateGen.axisBlock(c, p, { AssetLookup.partialBaseModel(c, p, "redstone_duct") })
//        }
        .item()
        .tab(ClockworkMod.GAS_CREATIVE_TABINFO)
        .model(AssetLookup.customBlockItemModel("redstone_duct", "block"))
        .transform(customItemModel())
        .transform(ClockworkStress.setImpact(4.0))
        .register()

    @JvmField
    val VALVE_DUCT: BlockEntry<ValveDuctBlock> =
        REGISTRATE.block<ValveDuctBlock>("valve_duct") { properties: BlockBehaviour.Properties? ->
            ValveDuctBlock(properties!!)
        }
            .initialProperties { SharedProperties.netheriteMetal() }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.GAS_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val GAS_NOZZLE: BlockEntry<GasNozzleBlock> =
        REGISTRATE.block<GasNozzleBlock>("gas_nozzle") { properties: BlockBehaviour.Properties? ->
            GasNozzleBlock(properties!!)
        }
            .initialProperties { Blocks.IRON_BLOCK }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .properties { it.noOcclusion() }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            //.tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .model(AssetLookup.customBlockItemModel("gas_nozzle"))
            .build()
            .register()

    @JvmField
    val STEAM_GENERATOR: BlockEntry<SteamGeneratorBlock> =
        REGISTRATE.block<SteamGeneratorBlock>("steam_generator") { properties: BlockBehaviour.Properties? ->
            SteamGeneratorBlock(properties!!)
        }
            .initialProperties { Blocks.IRON_BLOCK }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .properties { it.noOcclusion() }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.GAS_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val GAS_ENGINE: BlockEntry<GasEngineBlock> =
        REGISTRATE.block<GasEngineBlock>("gas_engine") { properties: BlockBehaviour.Properties? ->
            GasEngineBlock(properties!!)
        }
            .initialProperties { Blocks.IRON_BLOCK }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .properties { it.noOcclusion() }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.GAS_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val GAS_CRAFTER: BlockEntry<GasCrafterBlock> =
        REGISTRATE.block("gas_crafter") { properties: BlockBehaviour.Properties? ->
            GasCrafterBlock(properties!!)
        }
            .initialProperties { Blocks.IRON_BLOCK }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .properties { it.noOcclusion() }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.GAS_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val EXHAUST: BlockEntry<ExhaustBlock> =
        REGISTRATE.block<ExhaustBlock>("exhaust") { properties: BlockBehaviour.Properties? ->
            ExhaustBlock(properties!!)
        }
            .initialProperties { Blocks.IRON_BLOCK }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .properties { it.noOcclusion() }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.GAS_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val GAS_HEATER: BlockEntry<GasHeaterBlock> =
        REGISTRATE.block<GasHeaterBlock>("gas_heater") { properties: BlockBehaviour.Properties? ->
            GasHeaterBlock(properties!!)
        }
            .initialProperties { Blocks.IRON_BLOCK }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.GAS_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val GAS_THRUSTER: BlockEntry<GasThrusterBlock> =
        REGISTRATE.block<GasThrusterBlock>("gas_thruster") { properties: BlockBehaviour.Properties? ->
            GasThrusterBlock(properties!!)
        }
            .initialProperties { Blocks.IRON_BLOCK }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .properties { it.noOcclusion() }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.GAS_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val EXTENDON: BlockEntry<ExtendonBlock> =
        REGISTRATE.block<ExtendonBlock>("extendon") { properties: BlockBehaviour.Properties? ->
            ExtendonBlock(properties!!)
        }
            .initialProperties { SharedProperties.netheriteMetal() }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.GAS_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val HOSE_PORT: BlockEntry<HosePortBlock> =
        REGISTRATE.block<HosePortBlock>("hose_port") { properties: BlockBehaviour.Properties? ->
            HosePortBlock(properties!!)
        }
            .initialProperties { SharedProperties.netheriteMetal() }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            //.tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .build()
            .register()


    @JvmField
    val GAS_BACKTANK: BlockEntry<GasBacktankBlock> =
        REGISTRATE.block<GasBacktankBlock>("gas_backtank") { properties: BlockBehaviour.Properties? ->
            GasBacktankBlock(properties!!)
        }
            .initialProperties { SharedProperties.softMetal() }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .register()

    @JvmField
    val CREATIVE_GENERATOR: BlockEntry<CreativeGeneratorBlock> = REGISTRATE.block<CreativeGeneratorBlock>(
        "creative_gas_generator"
    ) { properties: BlockBehaviour.Properties? ->
        CreativeGeneratorBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .addLayer { Supplier { RenderType.cutout() } }
        .properties { it.noOcclusion() }
        .item()
        .tab(ClockworkMod.GAS_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val GOO_BLOCK = REGISTRATE.block<GooBlock>("goo_block") { properties: BlockBehaviour.Properties? ->
        GooBlock(
            properties!!
        )
    }
        .initialProperties { Blocks.HONEY_BLOCK }
        .addLayer { Supplier { RenderType.translucent() } }
        .item()
        .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val SLICKER = REGISTRATE.block<SlickerBlock>(
        "slicker"
    ) { properties: BlockBehaviour.Properties? ->
        SlickerBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.softMetal() }
        .addLayer { Supplier { RenderType.translucent() } }
        .item()
        //.tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
        .transform(customItemModel())
        .register()

    @JvmField
    val WANDERLITE_DEEPSLATE_ORE = REGISTRATE.block<WanderliteOreBlock>(
        "wanderlite_deepslate_ore"
    ) { properties: BlockBehaviour.Properties? ->
        WanderliteOreBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .item()
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val WANDERLITE_END_ORE = REGISTRATE.block<WanderliteOreBlock>(
        "wanderlite_end_ore"
    ) { properties: BlockBehaviour.Properties? ->
        WanderliteOreBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .item()
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val WANDERLITE_NYX_ORE = REGISTRATE.block<WanderliteOreBlock>(
        "wanderlite_nyx_ore"
    ) { properties: BlockBehaviour.Properties? ->
        WanderliteOreBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .item()
        .properties {
            it.fireResistant()
        }
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val NYX = REGISTRATE.block<Block>(
        "nyx"
    ) { properties: BlockBehaviour.Properties? ->
        Block(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .item()
        .properties {
            it.fireResistant()
        }
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val COBBLED_NYX = REGISTRATE.block<Block>(
        "cobbled_nyx"
    ) { properties: BlockBehaviour.Properties? ->
        Block(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .item()
        .properties {
            it.fireResistant()
        }
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val CHARGED_NYX = REGISTRATE.block<Block>(
        "charged_nyx"
    ) { properties: BlockBehaviour.Properties? ->
        Block(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .item()
        .properties {
            it.fireResistant()
        }
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val WANDERLITE_BLOCK = REGISTRATE.block<GenericWanderliteBlock>(
        "wanderlite_block"
    ) { properties: BlockBehaviour.Properties? ->
        GenericWanderliteBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .item()
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val CHISELED_WANDERLITE = REGISTRATE.block<GenericWanderliteBlock>(
        "chiseled_wanderlite"
    ) { properties: BlockBehaviour.Properties? ->
        GenericWanderliteBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .item()
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val SMOOTH_WANDERLITE = REGISTRATE.block<GenericWanderliteBlock>(
        "smooth_wanderlite"
    ) { properties: BlockBehaviour.Properties? ->
        GenericWanderliteBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .item()
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val SMOOTH_WANDERLITE_SLAB = REGISTRATE.block<GenericWanderliteSlab>(
        "smooth_wanderlite_slab"
    ) { properties: BlockBehaviour.Properties? ->
        GenericWanderliteSlab(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .item()
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val SMOOTH_WANDERLITE_STAIRS = REGISTRATE.block<GenericWanderliteStairs>(
        "smooth_wanderlite_stairs"
    ) { properties: BlockBehaviour.Properties? ->
        GenericWanderliteStairs(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .item()
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val WANDERLITE_BRICKS = REGISTRATE.block<GenericWanderliteBlock>(
        "wanderlite_bricks"
    ) { properties: BlockBehaviour.Properties? ->
        GenericWanderliteBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .item()
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val WANDERGLASS = REGISTRATE.block<GenericWanderliteBlock>(
        "wanderglass"
    ) { properties: BlockBehaviour.Properties? ->
        GenericWanderliteBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .properties { it.noOcclusion() ; it.lightLevel{ _ -> 1 } ; it.isViewBlocking{ _, _, _ -> false } }
        .addLayer { Supplier { RenderType.cutout() } }
        .tag(ClockworkTags.AllBlockTags.SENSOR_LENS.tag)
        .item()
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .build()
        .register()

    @JvmField
    val BALLOON_CASING = REGISTRATE.block<CasingBlock>(
        "balloon_casing"
    ) { properties: BlockBehaviour.Properties? ->
        CasingBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.wooden() }
        .transform(BuilderTransformers.casing { ClockworkSpriteShifts.BALLOON_CASING })
        .register()

    @JvmField
    val BALLOON_ENCASED_SHAFT = REGISTRATE.block<ExtendedEncasedShaftBlock>(
        "balloon_encased_shaft"
    ) { properties: BlockBehaviour.Properties? ->
        ExtendedEncasedShaftBlock.balloon(
            properties!!,
        )
    }
        .initialProperties { SharedProperties.wooden() }
        .transform(axeOrPickaxe())
        .transform(BuilderTransformersClockwork.encasedShaft("balloon") { ClockworkSpriteShifts.BALLOON_CASING })
        //.transform(EncasingRegistry.addVariantTo { AllBlocks.SHAFT.get() })
        .register()

    @JvmField
    val CLOCK: BlockEntry<ClockBlock> = REGISTRATE.block<ClockBlock>(
        "clock"
    ) { properties: BlockBehaviour.Properties? ->
        ClockBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.wooden() }
        .addLayer { Supplier { RenderType.cutoutMipped() } }
        .item()
        //.tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .transform(customItemModel())
        .register()

    @JvmField
    val DELIVERY_CANNON: BlockEntry<DeliveryCannonBlock> =
        REGISTRATE.block<DeliveryCannonBlock>("delivery_cannon") { properties: BlockBehaviour.Properties? ->
            DeliveryCannonBlock(properties!!)
        }
            .initialProperties { SharedProperties.wooden() }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val DELIVERY_CHUTE: BlockEntry<DeliveryChuteBlock> =
        REGISTRATE.block<DeliveryChuteBlock>("delivery_chute") { properties: BlockBehaviour.Properties? ->
            DeliveryChuteBlock(properties!!)
        }
            .initialProperties { SharedProperties.wooden() }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val ASTEROID: BlockEntry<MeteorTestBlock> =
        REGISTRATE.block<MeteorTestBlock>("asteroid_block") { properties: BlockBehaviour.Properties? ->
            MeteorTestBlock(properties!!)
        }
            .initialProperties { SharedProperties.wooden() }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(CreativeModeTabs.OP_BLOCKS)
            .build()
            .register()

    @JvmField
    val UNIVERSAL_SHAFT: BlockEntry<UniversalShaftBlock> =
        REGISTRATE.block<UniversalShaftBlock>("universal_shaft") { properties: BlockBehaviour.Properties? ->
            UniversalShaftBlock(properties!!)
        }
            .initialProperties { SharedProperties.stone() }
            .transform(axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmField
    val SPINOFF_BEARING: BlockEntry<SpinoffBearingBlock> =
        REGISTRATE.block<SpinoffBearingBlock>("spinoff_bearing") { properties: BlockBehaviour.Properties? ->
            SpinoffBearingBlock(properties!!)
        }
            .transform(axeOrPickaxe())
            //.transform(BuilderTransformersClockwork.bearing("propeller", "gearbox"))
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .item()
            .tab(ClockworkMod.PHYSICAL_CREATIVE_TABINFO)
            .build()
            .register()

    @JvmStatic
    fun register() {

    }
}
