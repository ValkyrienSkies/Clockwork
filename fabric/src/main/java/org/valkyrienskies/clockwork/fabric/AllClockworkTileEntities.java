package org.valkyrienskies.clockwork.fabric;


import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingInstance;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import org.valkyrienskies.clockwork.fabric.content.contraptions.components.propellor.PropellorBearingTileEntity;

import static org.valkyrienskies.clockwork.fabric.ClockWorkModFabric.REGISTRATE;

public class AllClockworkTileEntities {

    // Kinetics
    public static final BlockEntityEntry<PropellorBearingTileEntity> PROPELLOR_BEARING = REGISTRATE
            .tileEntity("propellor_bearing", PropellorBearingTileEntity::new)
            .instance(() -> BearingInstance::new)
            .validBlocks(AllClockworkBlocks.PROPELLOR_BEARING)
            .renderer(() -> BearingRenderer::new)
            .register();


    public static void register() {}
}
