package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.content.AllSections;
import org.valkyrienskies.clockwork.ClockWorkMod;
import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class ForgeClockworkBlocks {

    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkMod.BASE_CREATIVE_TAB);
    }

    //////// Propellor Bearing ////////

    static {
        REGISTRATE.startSection(AllSections.KINETICS);
    }

    public static void register() {
    }
}
