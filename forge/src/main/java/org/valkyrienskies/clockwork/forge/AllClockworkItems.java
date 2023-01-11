package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.content.AllSections;

import static org.valkyrienskies.clockwork.forge.ClockWorkModForge.REGISTRATE;

public class AllClockworkItems {
    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkModForge.BASE_CREATIVE_TAB);
    }

    static {
        REGISTRATE.startSection(AllSections.KINETICS);
    }

    public static void register() {
    }
}
