package org.valkyrienskies.clockwork

import com.simibubi.create.content.contraptions.bearing.BearingRenderer
import com.tterrag.registrate.util.entry.BlockEntityEntry
import com.tterrag.registrate.util.nullness.NonNullFunction
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.client.render.WingBlockEntityRenderer
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingRenderer
import org.valkyrienskies.clockwork.content.contraptions.flap.smart_flap.SmartFlapBearingBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.altmeter.AltMeterBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingRenderer
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.GasThrusterBlockEntity
import org.valkyrienskies.clockwork.content.physicalities.gyro.GyroBlockEntity
import org.valkyrienskies.clockwork.content.physicalities.gyro.GyroBlockEntityRenderer
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserRenderer
import org.valkyrienskies.clockwork.content.physicalities.goo.GooBlockEntity
import org.valkyrienskies.clockwork.content.physicalities.goo.GooBlockEntityRenderer
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.SlickerBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.SlickerBlockEntityRenderer
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeControllerBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeControllerRenderer
import org.valkyrienskies.clockwork.content.curiosities.altmeter.AltMeterRenderer
import org.valkyrienskies.clockwork.content.curiosities.clock.ClockBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.clock.ClockRenderer
import org.valkyrienskies.clockwork.content.curiosities.sensor.rotation.LodefocusBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.sensor.rotation.LodefocusRenderer
import org.valkyrienskies.clockwork.content.generic.ColorBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorRenderer
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatRenderer
import org.valkyrienskies.clockwork.content.kinetics.universal_shaft.UniversalShaftBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.backtank.GasBacktankBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctRenderer
import org.valkyrienskies.clockwork.content.logistics.gas.engine.GasEngineBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.exhaust.ExhaustBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCrafterBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCrafterBlockEntityRenderer
import org.valkyrienskies.clockwork.content.logistics.gas.generation.coal_burner.CoalBurnerBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor.AirCompressorBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor.AirCompressorRenderer
import org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator.CreativeGeneratorBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.generation.steam_generator.SteamGeneratorBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.heater.GasHeaterBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.pump.PumpDuctBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.pump.PumpDuctRenderer
import org.valkyrienskies.clockwork.content.logistics.gas.storage.tank.DuctTankBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle.GasNozzleBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle.GasNozzleRenderer
import org.valkyrienskies.clockwork.content.logistics.gas.valve.ValveDuctBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.valve.ValveDuctRenderer
import org.valkyrienskies.clockwork.content.logistics.gas.valve.ValveDuctVisual
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonBlockEntity
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlockEntity
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotRenderer
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonRenderer
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonBlockEntity
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonRenderer
import org.valkyrienskies.clockwork.content.physicalities.reactionwheel.ReactionWheelBlockEntity
import org.valkyrienskies.clockwork.content.physicalities.reactionwheel.ReactionWheelRenderer
import org.valkyrienskies.clockwork.content.physicalities.spinoff_bearing.SpinoffBearingBlockEntity
import org.valkyrienskies.clockwork.content.propulsion.sugar_rocket.SugarRocketBlockEntity
import org.valkyrienskies.clockwork.content.propulsion.sugar_rocket.SugarRocketRenderer

object ClockworkBlockEntities {

    @JvmField
    val SUGAR_ROCKET: BlockEntityEntry<SugarRocketBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<SugarRocketBlockEntity>(
            "sugar_rocket"
        ) { type: BlockEntityType<SugarRocketBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            SugarRocketBlockEntity(
                type!!,
                pos!!,
                state!!
            )
        }
        .validBlocks(ClockworkBlocks.SUGAR_ROCKET)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in SugarRocketBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                SugarRocketRenderer(
                    context!!
                )
            }
        }
        .register()

    @JvmField
    val PROPELLER_BEARING: BlockEntityEntry<PropellerBearingBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<PropellerBearingBlockEntity>(
            "propeller_bearing"
        ) { type: BlockEntityType<PropellerBearingBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            PropellerBearingBlockEntity(
                type!!, pos!!, state!!
            )
        }
//        .visual { factory: SimpleBlockEntityVisualizer.Factory<PropellerBearingBlockEntity> ->
//            BearingVisual<PropellerBearingBlockEntity>()
//        }
        .validBlocks(ClockworkBlocks.JURYRIGGED_PROPELLER_BEARING)
        .validBlocks(ClockworkBlocks.BRASS_PROPELLER_BEARING)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in PropellerBearingBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                BearingRenderer<PropellerBearingBlockEntity>(
                    context!!
                )
            }
        }
        .register()

    @JvmField
    val BLADE_CONTROLLER: BlockEntityEntry<BladeControllerBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<BladeControllerBlockEntity>(
            "blade_controller"
        ) { type: BlockEntityType<BladeControllerBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            BladeControllerBlockEntity(
                type!!,
                pos!!,
                state!!
            )
        }
        .validBlocks(ClockworkBlocks.BLADE_CONTROLLER)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in BladeControllerBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                BladeControllerRenderer(
                    context!!
                )
            }
        }
        .register()

    @JvmField
    val PHYS_BEARING: BlockEntityEntry<PhysBearingBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<PhysBearingBlockEntity>(
            "phys_bearing"
        ) { type: BlockEntityType<PhysBearingBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            PhysBearingBlockEntity(
                type,
                pos,
                state
            )
        }
        .validBlocks(ClockworkBlocks.PHYS_BEARING)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in PhysBearingBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                PhysBearingRenderer(
                    context!!
                )
            }
        }
        .register()


    @JvmField
    val COMMAND_SEAT: BlockEntityEntry<SequencedSeatBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<SequencedSeatBlockEntity>(
            "sequenced_seat"
        ) { typeIn: BlockEntityType<SequencedSeatBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            SequencedSeatBlockEntity(
                typeIn,
                pos,
                state
            )
        }
        .validBlocks(ClockworkBlocks.COMMAND_SEAT)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in SequencedSeatBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                SequencedSeatRenderer(
                    context!!
                )
            }
        }
        .register()

    @JvmField
    val FLAP_BEARING: BlockEntityEntry<FlapBearingBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<FlapBearingBlockEntity>(
            "flap_bearing"
        ) { type: BlockEntityType<FlapBearingBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            FlapBearingBlockEntity(
                type,
                pos!!, state!!
            )
        }
        .validBlocks(ClockworkBlocks.ANDESITE_FLAP_BEARING)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in FlapBearingBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                FlapBearingRenderer(
                    context!!
                )
            }
        }
        .register()

    @JvmField
    val SMART_FLAP_BEARING: BlockEntityEntry<SmartFlapBearingBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<SmartFlapBearingBlockEntity>(
            "smart_flap_bearing"
        ) { type: BlockEntityType<SmartFlapBearingBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            SmartFlapBearingBlockEntity(
                type,
                pos!!, state!!
            )
        }
        .validBlocks(ClockworkBlocks.SMART_FLAP_BEARING)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in SmartFlapBearingBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                FlapBearingRenderer(
                    context!!
                )
            }
        }
        .register()

    @JvmField
    val ALT_METER: BlockEntityEntry<AltMeterBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<AltMeterBlockEntity>(
            "alt_meter"
        ) { typeIn: BlockEntityType<AltMeterBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            AltMeterBlockEntity(
                typeIn,
                pos!!, state!!
            )
        }
        .validBlocks(ClockworkBlocks.ALT_METER)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in AltMeterBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                AltMeterRenderer(
                    context
                )
            }
        }
        .register()

    @JvmField
    val LODEFOCUS = ClockworkMod.REGISTRATE.blockEntity<LodefocusBlockEntity>("lodefocus") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
        LodefocusBlockEntity(
            type,
            pos,
            state
        )
    }
        .validBlocks(ClockworkBlocks.LODEFOCUS)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in LodefocusBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                LodefocusRenderer(
                    context
                )
            }
        }
        .register()

    @JvmField
    val REACTIONWHEEL: BlockEntityEntry<ReactionWheelBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<ReactionWheelBlockEntity>(
            "reactionwheel"
        ) { typeIn: BlockEntityType<ReactionWheelBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            ReactionWheelBlockEntity(
                typeIn!!,
                pos!!, state!!
            )
        }
//        .instance { BiFunction<MaterialManager?, ReactionWheelBlockEntity?, BlockEntityInstance<in ReactionWheelBlockEntity?>> { materialManager: MaterialManager?, blockEntity: ReactionWheelBlockEntity? ->
//                ReactionWheelInstance(
//                    materialManager,
//                    blockEntity
//                )
//            }
//        }
        .validBlocks(ClockworkBlocks.REACTIONWHEEL)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in ReactionWheelBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                ReactionWheelRenderer(
                    context!!
                )
            }
        }
        .register()

    @JvmField
    val REDSTONE_RESISTOR: BlockEntityEntry<RedstoneResistorBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<RedstoneResistorBlockEntity>(
            "redstone_resistor"
        ) { type: BlockEntityType<RedstoneResistorBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            RedstoneResistorBlockEntity(
                type, pos!!, state!!
            )
        }
        .validBlocks(ClockworkBlocks.REDSTONE_RESISTOR)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in RedstoneResistorBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                RedstoneResistorRenderer(
                    context
                )
            }
        }
        .register()

    @JvmField
    val COLOR_BLOCK_ENTITY: BlockEntityEntry<ColorBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<ColorBlockEntity>(
            "color_block_entity"
        ) { type: BlockEntityType<ColorBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            ColorBlockEntity(
                type,
                pos,
                state
            )
        }
        .validBlocks(ClockworkBlocks.WING, ClockworkBlocks.FLAP)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in ColorBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                WingBlockEntityRenderer(
                    context
                )
            }
        }
        .register()

    @JvmField
    val GYRO: BlockEntityEntry<GyroBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<GyroBlockEntity>(
            "gyro"
        ) { typeIn: BlockEntityType<GyroBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            GyroBlockEntity(
                typeIn,
                pos!!, state!!
            )
        }
        .validBlocks(ClockworkBlocks.GYRO)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in GyroBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                GyroBlockEntityRenderer(
                    context
                )
            }
        }
        .register()


    @JvmField
    val PHYSICS_INFUSER: BlockEntityEntry<PhysicsInfuserBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<PhysicsInfuserBlockEntity>(
            "physics_infuser"
        ) { typeIn: BlockEntityType<PhysicsInfuserBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            PhysicsInfuserBlockEntity(
                typeIn,
                pos!!, state!!
            )
        }
        .validBlocks(ClockworkBlocks.PHYSICS_INFUSER)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in PhysicsInfuserBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                PhysicsInfuserRenderer(
                    context
                )
            }
        }
        .register()

//    @JvmField
//    val HEAT_PIPE: BlockEntityEntry<HeatPipeBlockEntity> = ClockworkMod.REGISTRATE
//        .blockEntity<HeatPipeBlockEntity>("heat_pipe") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
//            HeatPipeBlockEntity(
//                type,
//                pos,
//                state
//            )
//        }
//        .renderer {
//            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in HeatPipeBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
//                HeatPipeBlockEntityRenderer(
//                    context!!
//                )
//            }
//        }
//        .validBlocks(ClockworkBlocks.HEAT_PIPE)
//        .register()

    @JvmField
    val DUCT: BlockEntityEntry<DuctBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<DuctBlockEntity>("duct") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
            DuctBlockEntity(
                type,
                pos,
                state
            )
        }
        .validBlocks(ClockworkBlocks.DUCT)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in DuctBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                DuctRenderer(
                    context!!
                )
            }
        }
        .register()


    @JvmField
    val PUMP_DUCT: BlockEntityEntry<PumpDuctBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<PumpDuctBlockEntity>("pump_duct") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
            PumpDuctBlockEntity(
                type,
                pos,
                state
            )
        }
//        .instance {
//            BiFunction<MaterialManager?, PumpDuctBlockEntity?, BlockEntityInstance<in PumpDuctBlockEntity?>> { materialManager: MaterialManager?, blockEntity: PumpDuctBlockEntity? ->
//                PumpDuctCogInstance(
//                    materialManager,
//                    blockEntity
//                )
//            }
//        }
        .validBlocks(ClockworkBlocks.PUMP_DUCT)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in PumpDuctBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                PumpDuctRenderer(
                    context!!
                )
            }
        }
        .register()

    @JvmField
    val VALVE_DUCT: BlockEntityEntry<ValveDuctBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<ValveDuctBlockEntity>("valve_duct") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
            ValveDuctBlockEntity(
                type,
                pos,
                state
            )
        }
//        .visual {
//            SimpleBlockEntityVisualizer.Factory { ctx, blockEntity, partialTick ->
//                ValveDuctVisual(ctx, blockEntity, partialTick)
//            }
//        }
//        .instance {
//            BiFunction<MaterialManager?, ValveDuctBlockEntity?, BlockEntityInstance<in ValveDuctBlockEntity?>> { materialManager: MaterialManager?, blockEntity: ValveDuctBlockEntity? ->
//                ShaftInstance(
//                    materialManager,
//                    blockEntity
//                )
//            }
//        }
        .validBlocks(ClockworkBlocks.VALVE_DUCT)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in ValveDuctBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                ValveDuctRenderer(
                    context!!
                )
            }
        }
        .register()



    @JvmField
    val COAL_BURNER: BlockEntityEntry<CoalBurnerBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<CoalBurnerBlockEntity>("coal_burner") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
            CoalBurnerBlockEntity(
                type,
                pos,
                state
            )
        }
        .validBlocks(ClockworkBlocks.COAL_BURNER)
        .register()

    @JvmField
    val CREATIVE_GENERATOR: BlockEntityEntry<CreativeGeneratorBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<CreativeGeneratorBlockEntity>("creative_gas_generator") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
            CreativeGeneratorBlockEntity(
                type,
                pos,
                state
            )
        }
        .validBlocks(ClockworkBlocks.CREATIVE_GENERATOR)
        .register()

    @JvmField
    val DUCT_TANK: BlockEntityEntry<DuctTankBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<DuctTankBlockEntity>("duct_tank") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
            DuctTankBlockEntity(
                type,
                pos,
                state
            )
        }
        .validBlocks(ClockworkBlocks.DUCT_TANK)
        .register()

    @JvmField
    val AIR_COMPRESSOR: BlockEntityEntry<AirCompressorBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<AirCompressorBlockEntity>("air_compressor") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
            AirCompressorBlockEntity(
                type,
                pos,
                state
            )
        }
        .validBlocks(ClockworkBlocks.AIR_COMPRESSOR)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in AirCompressorBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                AirCompressorRenderer(
                    context
                )
            }
        }
        .register()

    @JvmField
    val GAS_NOZZLE: BlockEntityEntry<GasNozzleBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<GasNozzleBlockEntity>("gas_nozzle") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
            GasNozzleBlockEntity(
                type,
                pos,
                state
            )
        }
//        .instance {
//            BiFunction<MaterialManager?, GasNozzleBlockEntity?, BlockEntityInstance<in GasNozzleBlockEntity?>> { materialManager: MaterialManager?, blockEntity: GasNozzleBlockEntity? ->
//                GasNozzleInstance(
//                    materialManager,
//                    blockEntity
//                )
//            }
//        }
        .validBlocks(ClockworkBlocks.GAS_NOZZLE)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in GasNozzleBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                GasNozzleRenderer(
                    context
                )
            }
        }
        .register()

    @JvmField
    val GAS_THRUSTER: BlockEntityEntry<GasThrusterBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<GasThrusterBlockEntity>("gas_thruster") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
            GasThrusterBlockEntity(
                type,
                pos,
                state
            )
        }
        .validBlocks(ClockworkBlocks.GAS_THRUSTER)
        .register()

    @JvmField
    val STEAM_GENERATOR: BlockEntityEntry<SteamGeneratorBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<SteamGeneratorBlockEntity>("steam_generator") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
            SteamGeneratorBlockEntity(
                type,
                pos,
                state
            )
        }
        .validBlocks(ClockworkBlocks.STEAM_GENERATOR)
        .register()

    @JvmField
    val GAS_ENGINE: BlockEntityEntry<GasEngineBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<GasEngineBlockEntity>("gas_engine") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
            GasEngineBlockEntity(
                type,
                pos,
                state
            )
        }
        .validBlocks(ClockworkBlocks.GAS_ENGINE)
        .register()

    @JvmField
    val GAS_CRAFTER: BlockEntityEntry<GasCrafterBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<GasCrafterBlockEntity>("gas_crafter") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
            GasCrafterBlockEntity(
                type,
                pos,
                state
            )
        }
        .renderer {
            NonNullFunction { context: BlockEntityRendererProvider.Context? ->
                GasCrafterBlockEntityRenderer(
                    context!!
                )
            }
        }
        .validBlocks(ClockworkBlocks.GAS_CRAFTER)
        .register()

    @JvmField
    val EXHAUST: BlockEntityEntry<ExhaustBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<ExhaustBlockEntity>("exhaust") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
            ExhaustBlockEntity(
                type,
                pos,
                state
            )
        }
        .validBlocks(ClockworkBlocks.EXHAUST)
        .register()

    @JvmField
    val GAS_HEATER: BlockEntityEntry<GasHeaterBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<GasHeaterBlockEntity>("gas_heater") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
            GasHeaterBlockEntity(
                type,
                pos,
                state
            )
        }

        .validBlocks(ClockworkBlocks.GAS_HEATER)
        .register()

    @JvmField
    val GOO_BLOCK = ClockworkMod.REGISTRATE.blockEntity<GooBlockEntity>("goo_block") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
        GooBlockEntity(
            type,
            pos,
            state
        )
    }
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in GooBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                GooBlockEntityRenderer(
                    context!!
                )
            }
        }
        .validBlocks(ClockworkBlocks.GOO_BLOCK)
        .register()

    @JvmField
    val SLICKER = ClockworkMod.REGISTRATE.blockEntity<SlickerBlockEntity>("slicker") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
        SlickerBlockEntity(
            type,
            pos,
            state
        )
    }
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in SlickerBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                SlickerBlockEntityRenderer(
                    context!!
                )
            }
        }
        .validBlocks(ClockworkBlocks.SLICKER)
        .register()

    @JvmField
    val CLOCK = ClockworkMod.REGISTRATE.blockEntity<ClockBlockEntity>("clock") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
        ClockBlockEntity(
            type,
            pos,
            state
        )
    }
        .validBlocks(ClockworkBlocks.CLOCK)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in ClockBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                ClockRenderer(
                    context!!
                )
            }
        }
        .register()

    @JvmField
    val DELIVERY_CANNON: BlockEntityEntry<DeliveryCannonBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<DeliveryCannonBlockEntity>(
            "delivery_cannon"
        ) { typeIn: BlockEntityType<DeliveryCannonBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            DeliveryCannonBlockEntity(
                typeIn,
                pos!!, state!!
            )
        }
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in DeliveryCannonBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                DeliveryCannonRenderer(
                    context!!
                )
            }
        }
        .validBlocks(ClockworkBlocks.DELIVERY_CANNON)
        .register()

    @JvmField
    val DELIVERY_CHUTE: BlockEntityEntry<DeliveryChuteBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<DeliveryChuteBlockEntity>(
            "delivery_chute"
        ) { typeIn: BlockEntityType<DeliveryChuteBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            DeliveryChuteBlockEntity(
                typeIn,
                pos!!, state!!
            )
        }
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in DeliveryChuteBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                FrequencySlotRenderer<DeliveryChuteBlockEntity>(
                    context!!
                )
            }
        }
        .validBlocks(ClockworkBlocks.DELIVERY_CHUTE)
        .register()

    @JvmField
    val GAS_BACKTANK: BlockEntityEntry<GasBacktankBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<GasBacktankBlockEntity>("gas_backtank") { type: BlockEntityType<*>, pos: BlockPos, state: BlockState ->
            GasBacktankBlockEntity(
                type,
                pos,
                state
            )
        }
        .validBlocks(ClockworkBlocks.GAS_BACKTANK)
        .register()

    @JvmField
    val UNIVERSAL_SHAFT: BlockEntityEntry<UniversalShaftBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<UniversalShaftBlockEntity>(
            "universal_shaft"
        ) { typeIn: BlockEntityType<UniversalShaftBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            UniversalShaftBlockEntity(
                typeIn,
                pos!!, state!!
            )
        }
        .validBlocks(ClockworkBlocks.UNIVERSAL_SHAFT)
        .register()

    @JvmField
    val EXTENDON: BlockEntityEntry<ExtendonBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<ExtendonBlockEntity>(
            "extendon"
        ) { typeIn: BlockEntityType<ExtendonBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            ExtendonBlockEntity(
                typeIn,
                pos!!, state!!
            )
        }
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in ExtendonBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                ExtendonRenderer(
                    context!!
                )
            }
        }
        .validBlocks(ClockworkBlocks.EXTENDON)
        .register()

    @JvmField
    val SPINOFF_BEARING: BlockEntityEntry<SpinoffBearingBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<SpinoffBearingBlockEntity>(
            "spinoff_bearing"
        ) { typeIn: BlockEntityType<SpinoffBearingBlockEntity>, pos: BlockPos, state: BlockState? ->
            SpinoffBearingBlockEntity(
                typeIn,
                pos, state!!
            )
        }
        .validBlocks(ClockworkBlocks.SPINOFF_BEARING)
        .register()

    @JvmStatic
    fun register() {
    }
}
