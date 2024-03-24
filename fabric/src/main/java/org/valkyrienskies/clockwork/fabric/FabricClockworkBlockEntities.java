package org.valkyrienskies.clockwork.fabric;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.platform.block_entity.FabricBallastBlockEntity;

public class FabricClockworkBlockEntities {

    public static final BlockEntityEntry<FabricBallastBlockEntity> BALLAST = ClockworkMod.INSTANCE.getREGISTRATE()
            .blockEntity("ballast", FabricBallastBlockEntity::new)
            .validBlocks(ClockworkBlocks.BALLAST)
            .register();

    public static void register() {
    }
}
