package org.valkyrienskies.clockwork

import dev.engine_room.flywheel.lib.model.baked.PartialModel

object ClockworkPartials {

    val BEARING_TOP_FLAP = block("flap_bearing_top")
    val JOYSTICK = block("command_seat/joystick")
    val BUTTON_ONE = block("command_seat/buttonone")
    val BUTTON_TWO = block("command_seat/buttontwo")
    val PHYSICS_CORE = block("physics_infuser/core")
    val STRANGE_FLUID = block("physics_infuser/liquid")
    val ZAP = block("physics_infuser/zap")
    val RESISTOR_INDICATOR = block("redstone_resistor/resistorindicator")
    val BLAZE_INFURIATED = block("afterblazer/blaze_infuriated")
    val PLUME_ANGRY = block("afterblazer/plume_angry")
    val PLUME_FUMING = block("afterblazer/plume_angry")
    val PLUME_INFURIATED = block("afterblazer/plume_infuriated")
    val WHEEL_BOTTOM = block("reactionwheel/wheelbottom")
    val WHEEL_TOP = block("reactionwheel/wheeltop")
    val ENGINE = block("combustion_engine/main")
    val SINGLE_ENGINE_PISTON = block("combustion_engine/single_piston")
    /*
    val PHYSFLAP_EAST = block("phys_bearing/flapeast")
    val PHYSFLAP_WEST = block("phys_bearing/flapwest")
    val PHYSFLAP_NORTH = block("phys_bearing/flapnorth")
    val PHYSFLAP_SOUTH = block("phys_bearing/flapsouth")
    val PHYSCORNER_NE = block("phys_bearing/cornerne")
    val PHYSCORNER_NW = block("phys_bearing/cornernw")
    val PHYSCORNER_SE = block("phys_bearing/cornerse")
    val PHYSCORNER_SW = block("phys_bearing/cornersw")

     */
    val WING_MIDDLE = block("wing/wing_middle")
    val WING_SIDE = block("wing/wing_side")
    val WING_SIDE_VERTICAL = block("wing/wing_side_vertical")
    val WING_SAIL_ITEM = item("wing/wing_sail")
    val WING_FRAME_ITEM = item("wing")
    val FLAP_FRAME_ITEM = item("flap")

    val BLADE_BASE = block("blade_controller/blade/blade_base")
    val BLADE_EXTENSION = block("blade_controller/blade/blade_extension")
    val BLADE_TIP = block("blade_controller/blade/blade_tip")
    val WIDEBLADE_BASE = block("blade_controller/blade/wideblade_base")
    val WIDEBLADE_EXTENSION = block("blade_controller/blade/wideblade_extension")
    val WIDEBLADE_TIP = block("blade_controller/blade/wideblade_tip")

    val PROPELLER_TOP = block("propeller_bearing/top")

    // region Gravitron
    val GRAV_DIAL_HAND = item("gravitron/dialhand")
    val GRAV_PRONG_LEFT_ONE = item("gravitron/prongleftone")
    val GRAV_PRONG_LEFT_TWO = item("gravitron/pronglefttwo")
    val GRAV_PRONG_RIGHT_ONE = item("gravitron/prongrightone")
    val GRAV_PRONG_RIGHT_TWO = item("gravitron/prongrighttwo")
    val GRAV_PRONG_TOP_ONE = item("gravitron/prongtopone")
    val GRAV_PRONG_TOP_TWO = item("gravitron/prongtoptwo")
    val GRAV_PRONG_LEFT_THREE = item("gravitron/prongleftthree")
    val GRAV_PRONG_RIGHT_THREE = item("gravitron/prongrightthree")
    val GRAV_PRONG_TOP_THREE = item("gravitron/prongtopthree")
    val OVERLOAD_FX = item("gravitron/overload_fx")
    // endregion

    val CRYSTAL: PartialModel = PartialModel.of(ClockworkMod.asResource("item/wanderwand/crystal"))
    val CRYSTAL_OUTER: PartialModel = PartialModel.of(ClockworkMod.asResource("item/wanderwand/crystal_outer"))
    val CRYSTAL_INNER: PartialModel = PartialModel.of(ClockworkMod.asResource("item/wanderwand/crystal_inner"))

    val GYRO_BASE: PartialModel = block("gyro/base")


    // region Phys Bearing
    val PHYS_NORTH_WING = block("phys_bearing/wing_north")
    val PHYS_SOUTH_WING = block("phys_bearing/wing_south")
    val PHYS_EAST_WING = block("phys_bearing/wing_east")
    val PHYS_WEST_WING = block("phys_bearing/wing_west")
    val PHYS_SHAFT = block("phys_bearing/shaft")
    val PHYS_ATTACHER = block("phys_bearing/attacher")
    // end region


    val GOO = block("slicker/goo")
    val DOINK = block("slicker/doink")
    val BOING = block("boing")

    //PIPE DEATH
    val DUCT_CONN: PartialModel = PartialModel.of(ClockworkMod.asResource("block/duct/connection"))
    val DUCT_RIM: PartialModel = PartialModel.of(ClockworkMod.asResource("block/duct/rim"))
    val DUCT_LEAK: PartialModel = PartialModel.of(ClockworkMod.asResource("block/duct/leak"))

    val DUCT_SMART: PartialModel = PartialModel.of(ClockworkMod.asResource("block/duct/custom/smart"))
    val DUCT_COPPER: PartialModel = PartialModel.of(ClockworkMod.asResource("block/duct/custom/copper"))
    val DUCT_ONEWAY_FORWARD: PartialModel = PartialModel.of(ClockworkMod.asResource("block/duct/custom/oneway_forward"))
    val DUCT_ONEWAY_BACKWARD: PartialModel = PartialModel.of(ClockworkMod.asResource("block/duct/custom/oneway_backward"))
    //END OF PIPE DEATH

    //GAS CRAFTER
    val GAS_CRAFTER_FRAME: PartialModel = PartialModel.of(ClockworkMod.asResource("block/gas_crafter/frame"))
    val GAS_CRAFTER_TUBE: PartialModel = PartialModel.of(ClockworkMod.asResource("block/gas_crafter/tube"))
    val GAS_CRAFTER_MESH: PartialModel = PartialModel.of(ClockworkMod.asResource("block/gas_crafter/mesh"))
    val GAS_CRAFTER_GLOW: PartialModel = PartialModel.of(ClockworkMod.asResource("block/gas_crafter/glow"))




    //GAS CRAFTER

    val VALVE_DUCT_POINTER: PartialModel = PartialModel.of(ClockworkMod.asResource("block/valve_duct/pointer"))

    val COMPRESSOR_AXIS: PartialModel = PartialModel.of(ClockworkMod.asResource("block/compressor/axis"))
    val COMPRESSOR_FABRIC: PartialModel = PartialModel.of(ClockworkMod.asResource("block/compressor/fabric"))
    val COMPRESSOR_TOP: PartialModel = PartialModel.of(ClockworkMod.asResource("block/compressor/top"))

    val PUMP_COG: PartialModel = PartialModel.of(ClockworkMod.asResource("block/pump/cog"))

    val NOZZLE_DIAL: PartialModel = PartialModel.of(ClockworkMod.asResource("block/gas_nozzle/dial"))
    val NOZZLE_AXIS: PartialModel = PartialModel.of(ClockworkMod.asResource("block/gas_nozzle/axis"))

    val HAND_SECOND: PartialModel = PartialModel.of(ClockworkMod.asResource("block/clock/hand_second"))
    val HAND_MINUTE: PartialModel = PartialModel.of(ClockworkMod.asResource("block/clock/hand_minute"))
    val HAND_HOUR: PartialModel = PartialModel.of(ClockworkMod.asResource("block/clock/hand_hour"))
    val CLOCK_FRAME: PartialModel = PartialModel.of(ClockworkMod.asResource("block/clock/clock_ring"))
    // region Delivery cannon
    val CANNON_ANTENNA = PartialModel.of(ClockworkMod.asResource("block/delivery_cannon/antenna"))
    val CANNON_BARREL = PartialModel.of(ClockworkMod.asResource("block/delivery_cannon/cannon_barrel"))
    val CANNON_BASE = PartialModel.of(ClockworkMod.asResource("block/delivery_cannon/cannon_base"))
    val CANNON_MOUNT = PartialModel.of(ClockworkMod.asResource("block/delivery_cannon/mount"))
    // region Extendon
    val EXTENDON_AXIS0 = PartialModel.of(ClockworkMod.asResource("block/extendon/axis0"))
    val EXTENDON_AXIS1 = PartialModel.of(ClockworkMod.asResource("block/extendon/axis1"))
    val EXTENDON_HOSE = PartialModel.of(ClockworkMod.asResource("block/extendon/hose"))

    // endregion

    // region Aeronaut
    val HAT_GOGGLES = PartialModel.of(ClockworkMod.asResource("item/aeronaut_goggles/goggles"))
    val HAT_FLAP_LEFT = PartialModel.of(ClockworkMod.asResource("item/aeronaut_goggles/flap_left"))
    val HAT_FLAP_RIGHT = PartialModel.of(ClockworkMod.asResource("item/aeronaut_goggles/flap_right"))
    val HAT_BASE = PartialModel.of(ClockworkMod.asResource("item/aeronaut_goggles/base"))

    val ALTIMETER_REDSTONE = PartialModel.of(ClockworkMod.asResource("block/alt_meter/redstone"))

    //endregion

    //copter bearing
    val SMART_PROP_TOP: PartialModel = PartialModel.of(ClockworkMod.asResource("block/copter_bearing/top"))
    val SMART_PROP_BASE: PartialModel = PartialModel.of(ClockworkMod.asResource("block/copter_bearing/base"))

    val SMART_PROP_PISTON_NW: PartialModel = PartialModel.of(ClockworkMod.asResource("block/copter_bearing/piston_nw"))
    val SMART_PROP_PISTON_NE: PartialModel = PartialModel.of(ClockworkMod.asResource("block/copter_bearing/piston_ne"))
    val SMART_PROP_PISTON_SW: PartialModel = PartialModel.of(ClockworkMod.asResource("block/copter_bearing/piston_sw"))
    val SMART_PROP_PISTON_SE: PartialModel = PartialModel.of(ClockworkMod.asResource("block/copter_bearing/piston_se"))
    val SMART_PROP_WAFER: PartialModel = PartialModel.of(ClockworkMod.asResource("block/copter_bearing/wafer"))

    private fun block(path: String): PartialModel {
        return PartialModel.of(ClockworkMod.asResource("block/$path"))
    }

    private fun entity(path: String): PartialModel {
        return PartialModel.of(ClockworkMod.asResource("entity/$path"))
    }

    private fun item(path: String): PartialModel {
        return PartialModel.of(ClockworkMod.asResource("item/$path"))
    }

    fun init() {
        // init static fields
    }
}
