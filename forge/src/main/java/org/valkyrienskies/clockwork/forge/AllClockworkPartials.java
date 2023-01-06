package org.valkyrienskies.clockwork.forge;

import com.jozufozu.flywheel.core.PartialModel;


public class AllClockworkPartials {
    public static final PartialModel

        BEARING_TOP_VSIFIED = block("vstop")

    ;

    private static PartialModel block(String path) {
        return new PartialModel(ClockWorkModFabric.asResource("block/" + path));
    }
    private static PartialModel entity(String path) {
        return new PartialModel(ClockWorkModFabric.asResource("entity/" + path));
    }

    public static void init() {
        // init static fields
    }
}
