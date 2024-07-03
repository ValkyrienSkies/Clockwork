package org.valkyrienskies.clockwork

import com.tterrag.registrate.util.entry.BlockEntityEntry
import com.tterrag.registrate.util.nullness.NonNullFunction
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.client.render.WingBlockEntityRenderer
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingRenderer
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingRenderer
import org.valkyrienskies.clockwork.content.contraptions.phys.gyro.GyroBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.gyro.GyroBlockEntityRenderer
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserRenderer
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.GooBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.GooBlockEntityRenderer
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.SlickerBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.SlickerBlockEntityRenderer
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingRenderer
import org.valkyrienskies.clockwork.content.generic.ColorBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorRenderer
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatRenderer
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.duct.PumpDuctBlockEntity
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonBlockEntity
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlockEntity

object ClockworkBlockEntities {

    @JvmField
    val PROPELLER_BEARING: BlockEntityEntry<PropellerBearingBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<PropellerBearingBlockEntity>(
            "propeller_bearing"
        ) { type: BlockEntityType<PropellerBearingBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            PropellerBearingBlockEntity(
                type!!, pos!!, state!!
            )
        }
        .validBlocks(ClockworkBlocks.PROPELLER_BEARING)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in PropellerBearingBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
                PropellerBearingRenderer(
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
        .validBlocks(ClockworkBlocks.FLAP_BEARING)
        .renderer {
            NonNullFunction<BlockEntityRendererProvider.Context?, BlockEntityRenderer<in FlapBearingBlockEntity?>> { context: BlockEntityRendererProvider.Context? ->
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
        .validBlocks(ClockworkBlocks.PUMP_DUCT)
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
    val DELIVERY_CANNON: BlockEntityEntry<DeliveryCannonBlockEntity> = ClockworkMod.REGISTRATE
        .blockEntity<DeliveryCannonBlockEntity>(
            "delivery_cannon"
        ) { typeIn: BlockEntityType<DeliveryCannonBlockEntity?>?, pos: BlockPos?, state: BlockState? ->
            DeliveryCannonBlockEntity(
                typeIn,
                pos!!, state!!
            )
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
        .validBlocks(ClockworkBlocks.DELIVERY_CHUTE)
        .register()

    @JvmStatic
    fun register() {
    }
}