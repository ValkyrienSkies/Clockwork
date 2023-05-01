package org.valkyrienskies.clockwork;

import com.jozufozu.flywheel.core.PartialModel;


public class ClockWorkPartials {
    public static final PartialModel

            BEARING_TOP_VSIFIED = block("vstop"),
            BEARING_TOP_FLAP = block("flap_bearing/top"),
            JOYSTICK = block("command_seat/joystick"),
            BUTTON_ONE = block("command_seat/buttonone"),
            BUTTON_TWO = block("command_seat/buttontwo"),
            PHYSICS_CORE = block("physics_infuser/core"),
            STRANGE_FLUID = block("physics_infuser/liquid"),
            ZAP = block("physics_infuser/zap"),

            RESISTOR_INDICATOR = block("redstone_resistor/resistorindicator"),

            BLAZE_INFURIATED = block("afterblazer/blaze_infuriated"),

            PLUME_ANGRY = block("afterblazer/plume_angry"),
            PLUME_FUMING = block("afterblazer/plume_angry"),
            PLUME_INFURIATED = block("afterblazer/plume_infuriated"),
            WHEEL_BOTTOM = block("reactionwheel/wheelbottom"),
            WHEEL_TOP = block("reactionwheel/wheeltop"),

            ENGINE = block("combustion_engine/main"),

            SINGLE_ENGINE_PISTON = block("combustion_engine/single_piston"),

            PHYSFLAP_EAST = block("phys_bearing/flapEast"),

            PHYSFLAP_WEST = block("phys_bearing/flapWest"),

            PHYSFLAP_NORTH = block("phys_bearing/flapNorth"),

            PHYSFLAP_SOUTH = block("phys_bearing/flapSouth"),

            PHYSCORNER_NE = block("phys_bearing/cornerNE"),

            PHYSCORNER_NW = block("phys_bearing/cornerNW"),

            PHYSCORNER_SE = block("phys_bearing/cornerSE"),

            PHYSCORNER_SW = block("phys_bearing/cornerSW")
    ;

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
