package org.valkyrienskies.clockwork;

import com.jozufozu.flywheel.core.PartialModel;


public class ClockWorkPartials {
    public static final PartialModel

            BEARING_TOP_VSIFIED = block("vstop"),
            BEARING_TOP_FLAP = block("flap_bearing/top"),
            PHYSICS_CORE = block("physics_infuser/core"),
            STRANGE_FLUID = block("physics_infuser/liquid"),
            ZAP = block("physics_infuser/zap");

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
