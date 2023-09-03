package org.valkyrienskies.clockwork

import com.jozufozu.flywheel.api.MaterialManager
import com.simibubi.create.content.contraptions.bearing.BearingRenderer
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.base.ShaftInstance
import com.simibubi.create.content.kinetics.base.ShaftRenderer
import com.tterrag.registrate.util.entry.BlockEntityEntry
import com.tterrag.registrate.util.nullness.NonNullFunction
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingRenderer
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingRenderer
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserRenderer
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingRenderer
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorRenderer
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatRenderer
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeBlockEntity
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeRenderer


object ClockworkBlockEntities {
    // Kinetics
    val PROPELLOR_BEARING: BlockEntityEntry<PropellerBearingBlockEntity> = REGISTRATE
        .blockEntity(java.lang.String("propellor_bearing"), ::PropellerBearingBlockEntity) //            .instance(() -> BearingInstance::new)
        .validBlocks(ClockworkBlocks.PROPELLOR_BEARING)
        .renderer{NonNullFunction(::PropellerBearingRenderer)}
        .register()

    val PHYS_BEARING: BlockEntityEntry<PhysBearingBlockEntity> = REGISTRATE
        .blockEntity(java.lang.String("phys_bearing"), ::PhysBearingBlockEntity)  //            .instance(() -> BearingInstance::new)
        .validBlocks(ClockworkBlocks.PHYS_BEARING)
        .renderer{NonNullFunction(::PhysBearingRenderer)}
        .register()

    val PHYSICS_INFUSER: BlockEntityEntry<PhysicsInfuserBlockEntity> = REGISTRATE
        .blockEntity(java.lang.String("physics_infuser"), ::PhysicsInfuserBlockEntity)
        .validBlocks(ClockworkBlocks.PHYSICS_INFUSER)
        .renderer{NonNullFunction(::PhysicsInfuserRenderer)}
        .register()

    /////// Sequenced Seat ////////
    val COMMAND_SEAT: BlockEntityEntry<SequencedSeatBlockEntity> = REGISTRATE
        .blockEntity(java.lang.String("sequenced_seat"), ::SequencedSeatBlockEntity)
        .validBlocks(ClockworkBlocks.COMMAND_SEAT)
        .renderer {NonNullFunction(::SequencedSeatRenderer)}
        .register()

    // FALP
    val FLAP_BEARING: BlockEntityEntry<FlapBearingBlockEntity> = REGISTRATE
        .blockEntity(java.lang.String("flap_bearing"), ::FlapBearingBlockEntity)
        .validBlocks(ClockworkBlocks.FLAP_BEARING)
        .renderer{NonNullFunction(::FlapBearingRenderer)}
        .register()

    // Intake
//    val INTAKE: BlockEntityEntry<IntakeBlockEntity> = REGISTRATE
//        .blockEntity("intake") { IntakeBlockEntity() }
//        .validBlocks(ClockworkBlocks.INTAKE)
//        .renderer { { IntakeRenderer() } }
//        .register()

    //BALLOONER
//    val BALLOONER: BlockEntityEntry<BalloonerBlockEntity> = REGISTRATE
//        .blockEntity("ballooner") { BalloonerBlockEntity() }
//        .validBlocks(ClockworkBlocks.BALLOONER)
//        .renderer { { BalloonerRenderer() } }
//        .register()

    //REDSTONE RESISTOR todo: add instance
    val REDSTONE_RESISTOR: BlockEntityEntry<RedstoneResistorBlockEntity> = REGISTRATE
        .blockEntity(java.lang.String("redstone_resistor"), ::RedstoneResistorBlockEntity)
        .validBlocks(ClockworkBlocks.REDSTONE_RESISTOR)
        .renderer {NonNullFunction(::RedstoneResistorRenderer)}
        .register()

    //REACTION WHEEL
    val REACTIONWHEEL: BlockEntityEntry<ReactionWheelBlockEntity> = REGISTRATE
        .blockEntity(java.lang.String("reaction_wheel"), ::ReactionWheelBlockEntity)
        .validBlocks(ClockworkBlocks.REACTIONWHEEL)
        .renderer {NonNullFunction(::ReactionWheelRenderer)}
        .register()

    val EXTENDED_ENCASED_SHAFT: BlockEntityEntry<KineticBlockEntity> = REGISTRATE
        .blockEntity(java.lang.String("extended_encased_shaft"), ::KineticBlockEntity)
        .validBlocks(ClockworkBlocks.BALLOON_ENCASED_SHAFT)
        .renderer { NonNullFunction(::ShaftRenderer) }
        .register()

    //    public static final BlockEntityEntry<UniversalJointBlockEntity> UNIVERSAL_JOINT = REGISTRATE
    //            .blockEntity("universal_joint", UniversalJointBlockEntity::new)
    //            .validBlocks(ClockworkBlocks.UNIVERSAL_JOINT)
    //            .renderer(() -> UniversalJointRenderer::new)
    //            .register();
    // COMBUSTION ENGINE
//    val COMBUSTION_ENGINE: BlockEntityEntry<CombustionEngineBlockEntity> = REGISTRATE
//        .blockEntity("combustion_engine") { CombustionEngineBlockEntity() }
//        .validBlocks(ClockworkBlocks.COMBUSTION_ENGINE)
//        .renderer { { CombustionEngineRenderer() } }
//        .register()

    // WINX
    val COLOR_BLOCK_ENTITY: BlockEntityEntry<ColorBlockEntity> = REGISTRATE
        .blockEntity(java.lang.String("color_block_entity"), ::ColorBlockEntity)
        .validBlocks(ClockworkBlocks.WING, ClockworkBlocks.FLAP)
        .renderer {NonNullFunction(::WingBlockEntityRenderer)}
        .register()

    val HEAT_PIPE: BlockEntityEntry<HeatPipeBlockEntity> = REGISTRATE
        .blockEntity(java.lang.String("heat_pipe"), ::HeatPipeBlockEntity)
        .validBlocks(ClockworkBlocks.HEAT_PIPE)
        .renderer {NonNullFunction(::HeatPipeRenderer)}
        .register()

    fun register() {}
}
