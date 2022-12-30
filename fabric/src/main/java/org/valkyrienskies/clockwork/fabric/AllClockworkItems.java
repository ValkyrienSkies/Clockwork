package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.content.AllSections;

import static org.valkyrienskies.clockwork.fabric.ClockWorkModFabric.REGISTRATE;

public class AllClockworkItems {
    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkModFabric.BASE_CREATIVE_TAB);
    }

    static {
        REGISTRATE.startSection(AllSections.KINETICS);
    }

    public static void register() {}
}
