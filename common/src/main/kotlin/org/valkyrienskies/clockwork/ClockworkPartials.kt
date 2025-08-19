package org.valkyrienskies.clockwork

import com.jozufozu.flywheel.core.PartialModel
import com.simibubi.create.AllPartialModels

object ClockworkPartials {

    val BEARING_TOP_VSIFIED = block("vstop")
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
    // endregion

    val CRYSTAL: PartialModel = PartialModel(ClockworkMod.asResource("item/wanderwand/crystal"))
    val CRYSTAL_OUTER: PartialModel = PartialModel(ClockworkMod.asResource("item/wanderwand/crystal_outer"))
    val CRYSTAL_INNER: PartialModel = PartialModel(ClockworkMod.asResource("item/wanderwand/crystal_inner"))

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
    val DUCT_CORE: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/core"))
    val DUCT_CORE_WARM: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/core/warm"))
    val DUCT_CORE_HOT: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/core/hot"))
    val DUCT_CORE_VERY_HOT: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/core/very_hot"))
    val DUCT_CORE_SUPER_HOT: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/core/super_hot"))
    val DUCT_CORE_MOLTEN: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/core/molten"))

    val DUCT_CONN: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/connection"))
    val DUCT_CONN_WARM: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/connection/warm"))
    val DUCT_CONN_HOT: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/connection/hot"))
    val DUCT_CONN_VERY_HOT: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/connection/very_hot"))
    val DUCT_CONN_SUPER_HOT: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/connection/super_hot"))
    val DUCT_CONN_MOLTEN: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/connection/molten"))

    val DUCT_RIM: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/rim"))
    val DUCT_RIM_WARM: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/rim/warm"))
    val DUCT_RIM_HOT: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/rim/hot"))
    val DUCT_RIM_VERY_HOT: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/rim/very_hot"))
    val DUCT_RIM_SUPER_HOT: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/rim/super_hot"))
    val DUCT_RIM_MOLTEN: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/rim/molten"))

    val DUCT_LEAK: PartialModel = PartialModel(ClockworkMod.asResource("block/duct/leak"))
    //END OF PIPE DEATH

    val VALVE_DUCT_POINTER: PartialModel = PartialModel(ClockworkMod.asResource("block/valve_duct/pointer"))

    val COMPRESSOR_AXIS: PartialModel = PartialModel(ClockworkMod.asResource("block/compressor/axis"))
    val COMPRESSOR_FABRIC: PartialModel = PartialModel(ClockworkMod.asResource("block/compressor/fabric"))
    val COMPRESSOR_TOP: PartialModel = PartialModel(ClockworkMod.asResource("block/compressor/top"))

    val PUMP_COG: PartialModel = PartialModel(ClockworkMod.asResource("block/pump/cog"))

    val NOZZLE_DIAL: PartialModel = PartialModel(ClockworkMod.asResource("block/gas_nozzle/dial"))
    val NOZZLE_AXIS: PartialModel = PartialModel(ClockworkMod.asResource("block/gas_nozzle/axis"))

    val HAND_SECOND: PartialModel = PartialModel(ClockworkMod.asResource("block/clock/hand_second"))
    val HAND_MINUTE: PartialModel = PartialModel(ClockworkMod.asResource("block/clock/hand_minute"))
    val HAND_HOUR: PartialModel = PartialModel(ClockworkMod.asResource("block/clock/hand_hour"))
    val CLOCK_FRAME: PartialModel = PartialModel(ClockworkMod.asResource("block/clock/clock_ring"))
    // region Delivery cannon
    val CANNON_ANTENNA = PartialModel(ClockworkMod.asResource("block/delivery_cannon/antenna"))
    val CANNON_BARREL = PartialModel(ClockworkMod.asResource("block/delivery_cannon/cannon_barrel"))
    val CANNON_BASE = PartialModel(ClockworkMod.asResource("block/delivery_cannon/cannon_base"))
    val CANNON_MOUNT = PartialModel(ClockworkMod.asResource("block/delivery_cannon/mount"))
    // region Extendon
    val EXTENDON_AXIS0 = PartialModel(ClockworkMod.asResource("block/extendon/axis0"))
    val EXTENDON_AXIS1 = PartialModel(ClockworkMod.asResource("block/extendon/axis1"))
    val EXTENDON_HOSE = PartialModel(ClockworkMod.asResource("block/extendon/hose"))

    // endregion

    val ALTIMETER_REDSTONE = PartialModel(ClockworkMod.asResource("block/alt_meter/redstone"))

    private fun block(path: String): PartialModel {
        return PartialModel(ClockworkMod.asResource("block/$path"))
    }

    private fun entity(path: String): PartialModel {
        return PartialModel(ClockworkMod.asResource("entity/$path"))
    }

    private fun item(path: String): PartialModel {
        return PartialModel(ClockworkMod.asResource("item/$path"))
    }

    fun init() {
        // init static fields
    }
}
