package org.valkyrienskies.clockwork;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.ShaftInstance;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingRenderer;
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingRenderer;
import org.valkyrienskies.clockwork.content.contraptions.phys.gyro.GyroBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserRenderer;
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingRenderer;
import org.valkyrienskies.clockwork.content.generic.ColorBlockEntity;
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorBlockEntity;
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorRenderer;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatBlockEntity;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatRenderer;
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeBlockEntity;
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeRenderer;
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonBlockEntity;
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlockEntity;
import org.valkyrienskies.clockwork.content.physicalities.reaction_wheel.ReactionWheelBlockEntity;
import org.valkyrienskies.clockwork.content.physicalities.reaction_wheel.ReactionWheelRenderer;
import org.valkyrienskies.clockwork.util.render.WingBlockEntityRenderer;

public class ClockworkBlockEntities {

    private static final CreateRegistrate REGISTRATE = ClockworkMod.INSTANCE.getREGISTRATE();
    // Kinetics
    public static final BlockEntityEntry<PropellerBearingBlockEntity> PROPELLER_BEARING = REGISTRATE
            .blockEntity("propeller_bearing", PropellerBearingBlockEntity::new)
            // .instance(() -> BearingInstance::new)
            .validBlocks(ClockworkBlocks.PROPELLER_BEARING)
            .renderer(() -> PropellerBearingRenderer::new)
            .register();

    public static final BlockEntityEntry<PhysBearingBlockEntity> PHYS_BEARING = REGISTRATE
            .blockEntity("phys_bearing", PhysBearingBlockEntity::new)
            // .instance(() -> BearingInstance::new)
            .validBlocks(ClockworkBlocks.PHYS_BEARING)
            .renderer(() -> PhysBearingRenderer::new)
            .register();

    public static final BlockEntityEntry<PhysicsInfuserBlockEntity> PHYSICS_INFUSER = REGISTRATE
            .blockEntity("physics_infuser", PhysicsInfuserBlockEntity::new)
            .validBlocks(ClockworkBlocks.PHYSICS_INFUSER)
            .renderer(() -> PhysicsInfuserRenderer::new)
            .register();

    /////// Sequenced Seat ////////
    public static final BlockEntityEntry<SequencedSeatBlockEntity> COMMAND_SEAT = REGISTRATE
            .blockEntity("sequenced_seat", SequencedSeatBlockEntity::new)
            .validBlocks(ClockworkBlocks.COMMAND_SEAT)
            .renderer(() -> SequencedSeatRenderer::new)
            .register();

    // FALP
    public static final BlockEntityEntry<FlapBearingBlockEntity> FLAP_BEARING = REGISTRATE
            .blockEntity("flap_bearing", FlapBearingBlockEntity::new)
            .validBlocks(ClockworkBlocks.FLAP_BEARING)
            .renderer(() -> FlapBearingRenderer::new)
            .register();

    public static final BlockEntityEntry<DeliveryCannonBlockEntity> DELIVERY_CANNON = REGISTRATE
            .blockEntity("delivery_cannon", DeliveryCannonBlockEntity::new)
            .validBlocks(ClockworkBlocks.DELIVERY_CANNON)
            .register();

    public static final BlockEntityEntry<DeliveryChuteBlockEntity> DELIVERY_CHUTE = REGISTRATE
            .blockEntity("delivery_chute", DeliveryChuteBlockEntity::new)
            .validBlocks(ClockworkBlocks.DELIVERY_CHUTE)
            .register();

    public static final BlockEntityEntry<AltMeterBlockEntity> ALT_METER = REGISTRATE
            .blockEntity("alt_meter", AltMeterBlockEntity::new)
            .validBlocks(ClockworkBlocks.ALT_METER)
            .register();

    // REDSTONE RESISTOR todo: add instance

    public static final BlockEntityEntry<RedstoneResistorBlockEntity> REDSTONE_RESISTOR = REGISTRATE
            .blockEntity("redstone_resistor", RedstoneResistorBlockEntity::new)
            .validBlocks(ClockworkBlocks.REDSTONE_RESISTOR)
            .renderer(() -> RedstoneResistorRenderer::new)
            .register();

    // REACTION WHEEL

    public static final BlockEntityEntry<ReactionWheelBlockEntity> REACTIONWHEEL = REGISTRATE
            .blockEntity("reaction_wheel", ReactionWheelBlockEntity::new)
            .validBlocks(ClockworkBlocks.REACTIONWHEEL)
            .renderer(() -> ReactionWheelRenderer::new)
            .register();

    public static final BlockEntityEntry<KineticBlockEntity> EXTENDED_ENCASED_SHAFT = REGISTRATE
            .blockEntity("extended_encased_shaft", KineticBlockEntity::new)
            .instance(() -> ShaftInstance::new, false)
            .validBlocks(ClockworkBlocks.BALLOON_ENCASED_SHAFT)
            .renderer(() -> ShaftRenderer::new)
            .register();

    // public static final BlockEntityEntry<UniversalJointBlockEntity> UNIVERSAL_JOINT = REGISTRATE
            // .blockEntity("universal_joint", UniversalJointBlockEntity::new)
            // .validBlocks(ClockworkBlocks.UNIVERSAL_JOINT)
            // .renderer(() -> UniversalJointRenderer::new)
            // .register();


    public static final BlockEntityEntry<ColorBlockEntity> COLOR_BLOCK_ENTITY = REGISTRATE
            .blockEntity("color_block_entity", ColorBlockEntity::new)
            .validBlocks(ClockworkBlocks.WING, ClockworkBlocks.FLAP)
            .renderer(() -> WingBlockEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<HeatPipeBlockEntity> HEAT_PIPE = REGISTRATE
            .blockEntity("heat_pipe", HeatPipeBlockEntity::new)
            .validBlocks(ClockworkBlocks.HEAT_PIPE)
            .renderer(() -> HeatPipeRenderer::new)
            .register();

    public static final BlockEntityEntry<GyroBlockEntity> GYRO = REGISTRATE
            .blockEntity("gyro", GyroBlockEntity::new)
            .validBlocks(ClockworkBlocks.GYRO)
            .register();


    public static void register() {}
}
