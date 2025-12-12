package org.valkyrienskies.clockwork.fabric;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import org.valkyrienskies.clockwork.ClockworkMod;

public class FabricClockworkPartials {

    // Platform specific partials

    private static PartialModel block(String path) {
        return PartialModel.of(ClockworkMod.asResource("block/" + path));
    }

    private static PartialModel entity(String path) {
        return PartialModel.of(ClockworkMod.asResource("entity/" + path));
    }

    public static void init() {
        // init static fields
    }
}
