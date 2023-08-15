package org.valkyrienskies.clockwork;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.ShaftInstance;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlockEntity;
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeBlockEntity;
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeRenderer;
import org.valkyrienskies.clockwork.content.propulsion.afterblazer.AfterblazerEngineBlockEntity;
import org.valkyrienskies.clockwork.content.propulsion.afterblazer.AfterblazerRenderer;
import org.valkyrienskies.clockwork.content.propulsion.ballooner.BalloonerBlockEntity;
import org.valkyrienskies.clockwork.content.propulsion.ballooner.BalloonerRenderer;
import org.valkyrienskies.clockwork.content.kinetics.combustion_engine.CombustionEngineBlockEntity;
import org.valkyrienskies.clockwork.content.kinetics.combustion_engine.CombustionEngineRenderer;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingRenderer;
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserRenderer;
import org.valkyrienskies.clockwork.content.physicalities.intake.IntakeBlockEntity;
import org.valkyrienskies.clockwork.content.physicalities.intake.IntakeRenderer;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingRenderer;
import org.valkyrienskies.clockwork.content.contraptions.propellor.PropellorBearingBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.propellor.PropellorBearingRenderer;
import org.valkyrienskies.clockwork.content.physicalities.reaction_wheel.ReactionWheelBlockEntity;
import org.valkyrienskies.clockwork.content.physicalities.reaction_wheel.ReactionWheelRenderer;
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorBlockEntity;
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorRenderer;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatBlockEntity;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatRenderer;
import org.valkyrienskies.clockwork.content.munitions.stationary.solver.SolverBlockEntity;
import org.valkyrienskies.clockwork.content.munitions.stationary.solver.SolverRenderer;
import org.valkyrienskies.clockwork.content.materials.solids.colorblock.ColorBlockEntity;
import org.valkyrienskies.clockwork.util.render.WingBlockEntityRenderer;

import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class ClockWorkBlockEntities {

    // Kinetics
    public static final BlockEntityEntry<PropellorBearingBlockEntity> PROPELLOR_BEARING = REGISTRATE
            .blockEntity("propellor_bearing", PropellorBearingBlockEntity::new)
//            .instance(() -> BearingInstance::new)
            .validBlocks(ClockWorkBlocks.PROPELLOR_BEARING)
            .renderer(() -> PropellorBearingRenderer::new)
            .register();

    public static final BlockEntityEntry<PhysBearingBlockEntity> PHYS_BEARING = REGISTRATE
            .blockEntity("phys_bearing", PhysBearingBlockEntity::new)
//            .instance(() -> BearingInstance::new)
            .validBlocks(ClockWorkBlocks.PHYS_BEARING)
            .renderer(() -> PhysBearingRenderer::new)
            .register();

    public static final BlockEntityEntry<SolverBlockEntity> SOLVER = REGISTRATE
            .blockEntity("solver", SolverBlockEntity::new)
            .validBlocks(ClockWorkBlocks.SOLVER)
            .renderer(() -> SolverRenderer::new)
            .register();

    public static final BlockEntityEntry<AfterblazerEngineBlockEntity> AFTERBLAZER = REGISTRATE
            .blockEntity("afterblazer", AfterblazerEngineBlockEntity::new)
            .validBlocks(ClockWorkBlocks.AFTERBLAZER)
            .renderer(() -> AfterblazerRenderer::new)
            .register();

    public static final BlockEntityEntry<PhysicsInfuserBlockEntity> PHYSICS_INFUSER = REGISTRATE
            .blockEntity("physics_infuser", PhysicsInfuserBlockEntity::new)
            .validBlocks(ClockWorkBlocks.PHYSICS_INFUSER)
            .renderer(() -> PhysicsInfuserRenderer::new)
            .register();

    /////// Sequenced Seat ////////
    public static final BlockEntityEntry<SequencedSeatBlockEntity> COMMAND_SEAT = REGISTRATE
            .blockEntity("sequenced_seat", SequencedSeatBlockEntity::new)
            .validBlocks(ClockWorkBlocks.COMMAND_SEAT)
            .renderer(() -> SequencedSeatRenderer::new)
            .register();

    // FALP
    public static final BlockEntityEntry<FlapBearingBlockEntity> FLAP_BEARING = REGISTRATE
            .blockEntity("flap_bearing", FlapBearingBlockEntity::new)
            .validBlocks(ClockWorkBlocks.FLAP_BEARING)
            .renderer(() -> FlapBearingRenderer::new)
            .register();
    // Intake
    public static final BlockEntityEntry<IntakeBlockEntity> INTAKE = REGISTRATE
            .blockEntity("intake", IntakeBlockEntity::new)
            .validBlocks(ClockWorkBlocks.INTAKE)
            .renderer(() -> IntakeRenderer::new)
            .register();

    //BALLOONER

    public static final BlockEntityEntry<BalloonerBlockEntity> BALLOONER = REGISTRATE
            .blockEntity("ballooner", BalloonerBlockEntity::new)
            .validBlocks(ClockWorkBlocks.BALLOONER)
            .renderer(() -> BalloonerRenderer::new)
            .register();

    //REDSTONE RESISTOR todo: add instance

    public static final BlockEntityEntry<RedstoneResistorBlockEntity> REDSTONE_RESISTOR = REGISTRATE
            .blockEntity("redstone_resistor", RedstoneResistorBlockEntity::new)
            .validBlocks(ClockWorkBlocks.REDSTONE_RESISTOR)
            .renderer(() -> RedstoneResistorRenderer::new)
            .register();

    //REACTION WHEEL

    public static final BlockEntityEntry<ReactionWheelBlockEntity> REACTIONWHEEL = REGISTRATE
            .blockEntity("reaction_wheel", ReactionWheelBlockEntity::new)
            .validBlocks(ClockWorkBlocks.REACTIONWHEEL)
            .renderer(() -> ReactionWheelRenderer::new)
            .register();

    public static final BlockEntityEntry<KineticBlockEntity> EXTENDED_ENCASED_SHAFT = REGISTRATE
            .blockEntity("extended_encased_shaft", KineticBlockEntity::new)
            .instance(() -> ShaftInstance::new, false)
            .validBlocks(ClockWorkBlocks.BALLOON_ENCASED_SHAFT)
            .renderer(() -> ShaftRenderer::new)
            .register();

//    public static final BlockEntityEntry<UniversalJointBlockEntity> UNIVERSAL_JOINT = REGISTRATE
//            .blockEntity("universal_joint", UniversalJointBlockEntity::new)
//            .validBlocks(ClockWorkBlocks.UNIVERSAL_JOINT)
//            .renderer(() -> UniversalJointRenderer::new)
//            .register();


    // COMBUSTION ENGINE

    public static final BlockEntityEntry<CombustionEngineBlockEntity> COMBUSTION_ENGINE = REGISTRATE
            .blockEntity("combustion_engine", CombustionEngineBlockEntity::new)
            .validBlocks(ClockWorkBlocks.COMBUSTION_ENGINE)
            .renderer(() -> CombustionEngineRenderer::new)
            .register();

    // WINX

    public static final BlockEntityEntry<ColorBlockEntity> COLOR_BLOCK_ENTITY = REGISTRATE
            .blockEntity("color_block_entity", ColorBlockEntity::new)
            .validBlocks(ClockWorkBlocks.WING, ClockWorkBlocks.FLAP)
            .renderer(() -> WingBlockEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<HeatPipeBlockEntity> HEAT_PIPE = REGISTRATE
            .blockEntity("heat_pipe", HeatPipeBlockEntity::new)
            .validBlocks(ClockWorkBlocks.HEAT_PIPE)
            .renderer(() -> HeatPipeRenderer::new)
            .register();
    public static void register() {
    }
}
