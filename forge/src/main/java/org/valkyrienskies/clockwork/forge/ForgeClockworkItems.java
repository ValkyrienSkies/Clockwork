package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.content.AllSections;
import org.valkyrienskies.clockwork.ClockWorkMod;

import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class ForgeClockworkItems {
    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkMod.BASE_CREATIVE_TAB);
    }

    static {
        REGISTRATE.startSection(AllSections.KINETICS);
    }

    public static void register() {
    }
}
