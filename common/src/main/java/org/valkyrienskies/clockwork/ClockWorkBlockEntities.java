package org.valkyrienskies.clockwork;


import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingInstance;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MaterialColor;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingRenderer;
import org.valkyrienskies.clockwork.content.contraptions.infuser.PhysicsInfuserBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.infuser.PhysicsInfuserRenderer;
import org.valkyrienskies.clockwork.content.contraptions.propellor.PropellorBearingBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.SequencedSeatBlock;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.SequencedSeatBlockEntity;

import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class ClockWorkBlockEntities {

    // Kinetics
    public static final BlockEntityEntry<PropellorBearingBlockEntity> PROPELLOR_BEARING = REGISTRATE
            .tileEntity("propellor_bearing", PropellorBearingBlockEntity::new)
            .instance(() -> BearingInstance::new)
            .validBlocks(ClockWorkBlocks.PROPELLOR_BEARING)
            .renderer(() -> BearingRenderer::new)
            .register();

    public static final BlockEntityEntry<PhysicsInfuserBlockEntity> PHYSICS_INFUSER = REGISTRATE
            .tileEntity("physics_infuser", PhysicsInfuserBlockEntity::new)
            .validBlocks(ClockWorkBlocks.PHYSICS_INFUSER)
            .renderer(() -> PhysicsInfuserRenderer::new)
            .register();

    /////// Sequenced Seat ////////
    public static final BlockEntityEntry<SequencedSeatBlockEntity> SEQUENCED_SEAT = REGISTRATE
            .tileEntity("sequenced_seat", SequencedSeatBlockEntity::new)
            .validBlocks(ClockWorkBlocks.SEQUENCED_SEAT)
            //.renderer(() -> SequencedSeatRenderer::new)
            .register();

    // FALP
    public static final BlockEntityEntry<FlapBearingBlockEntity> FLAP_BEARING = REGISTRATE
            .tileEntity("flap_bearing", FlapBearingBlockEntity::new)
            .validBlocks(ClockWorkBlocks.FLAP_BEARING)
            .renderer(() -> FlapBearingRenderer::new)
            .register();

    public static void register() {}
}
