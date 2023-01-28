package org.valkyrienskies.clockwork;


import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingInstance;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerRenderer;
import org.valkyrienskies.clockwork.content.contraptions.ballooner.BalloonerBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.ballooner.BalloonerRenderer;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingRenderer;
import org.valkyrienskies.clockwork.content.contraptions.infuser.PhysicsInfuserBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.infuser.PhysicsInfuserRenderer;
import org.valkyrienskies.clockwork.content.contraptions.intake.IntakeBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.intake.IntakeRenderer;
import org.valkyrienskies.clockwork.content.contraptions.propellor.PropellorBearingBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.propellor.PropellorBearingRenderer;
import org.valkyrienskies.clockwork.content.contraptions.resistor.RedstoneResistorBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.resistor.RedstoneResistorRenderer;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.SequencedSeatBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.SequencedSeatRenderer;

import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class ClockWorkBlockEntities {

    // Kinetics
    public static final BlockEntityEntry<PropellorBearingBlockEntity> PROPELLOR_BEARING = REGISTRATE
            .tileEntity("propellor_bearing", PropellorBearingBlockEntity::new)
//            .instance(() -> BearingInstance::new)
            .validBlocks(ClockWorkBlocks.PROPELLOR_BEARING)
            .renderer(() -> PropellorBearingRenderer::new)
            .register();

    public static final BlockEntityEntry<AfterblazerBlockEntity> AFTERBLAZER = REGISTRATE
            .tileEntity("afterblazer", AfterblazerBlockEntity::new)
            .validBlocks(ClockWorkBlocks.AFTERBLAZER)
            .renderer(() -> AfterblazerRenderer::new)
            .register();

    public static final BlockEntityEntry<PhysicsInfuserBlockEntity> PHYSICS_INFUSER = REGISTRATE
            .tileEntity("physics_infuser", PhysicsInfuserBlockEntity::new)
            .validBlocks(ClockWorkBlocks.PHYSICS_INFUSER)
            .renderer(() -> PhysicsInfuserRenderer::new)
            .register();

    /////// Sequenced Seat ////////
    public static final BlockEntityEntry<SequencedSeatBlockEntity> COMMAND_SEAT = REGISTRATE
            .tileEntity("sequenced_seat", SequencedSeatBlockEntity::new)
            .validBlocks(ClockWorkBlocks.COMMAND_SEAT)
            .renderer(() -> SequencedSeatRenderer::new)
            .register();

    // FALP
    public static final BlockEntityEntry<FlapBearingBlockEntity> FLAP_BEARING = REGISTRATE
            .tileEntity("flap_bearing", FlapBearingBlockEntity::new)
            .validBlocks(ClockWorkBlocks.FLAP_BEARING)
            .renderer(() -> FlapBearingRenderer::new)
            .register();
    // Intake
    public static final BlockEntityEntry<IntakeBlockEntity> INTAKE = REGISTRATE
            .tileEntity("intake", IntakeBlockEntity::new)
            .validBlocks(ClockWorkBlocks.INTAKE)
            .renderer(() -> IntakeRenderer::new)
            .register();

    //BALLOONER

    public static final BlockEntityEntry<BalloonerBlockEntity> BALLOONER = REGISTRATE
            .tileEntity("ballooner", BalloonerBlockEntity::new)
            .validBlocks(ClockWorkBlocks.BALLOONER)
            .renderer(() -> BalloonerRenderer::new)
            .register();

    //REDSTONE RESISTOR todo: add instance

    public static final BlockEntityEntry<RedstoneResistorBlockEntity> REDSTONE_RESISTOR = REGISTRATE
            .tileEntity("redstone_resistor", RedstoneResistorBlockEntity::new)
            .validBlocks(ClockWorkBlocks.REDSTONE_RESISTOR)
            .renderer(() -> RedstoneResistorRenderer::new)
            .register();

    public static void register() {
    }
}
