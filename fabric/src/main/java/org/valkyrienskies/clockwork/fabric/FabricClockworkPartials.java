package org.valkyrienskies.clockwork.fabric;

import com.jozufozu.flywheel.core.PartialModel;
import org.valkyrienskies.clockwork.ClockWorkMod;


public class FabricClockworkPartials {

    // Platform specific partials

    private static PartialModel block(String path) {
        return new PartialModel(ClockWorkMod.asResource("block/" + path));
    }

    private static PartialModel entity(String path) {
        return new PartialModel(ClockWorkMod.asResource("entity/" + path));
    }

    public static void init() {
        // init static fields
    }
}
