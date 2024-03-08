package org.valkyrienskies.clockwork.forge;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.content.physicalities.ballast.BallastBlockEntity;
import org.valkyrienskies.clockwork.platform.SharedValues;
import org.valkyrienskies.clockwork.platform.block_entity.ForgeBallastBlockEntity;

public class ForgeClockworkBlockEntities {

    public static final BlockEntityEntry<ForgeBallastBlockEntity> BALLAST = ClockworkMod.INSTANCE.getREGISTRATE()
            .blockEntity("ballast", ForgeBallastBlockEntity::new)
            .validBlocks(ClockworkBlocks.BALLAST)
            .register();

    public static void register() {
    }
}
