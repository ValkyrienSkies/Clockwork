package org.valkyrienskies.clockwork

import com.jozufozu.flywheel.core.PartialModel
import com.simibubi.create.AllPartialModels

object ClockworkPartials {

    val BEARING_TOP_VSIFIED = block("vstop")
    val BEARING_TOP_FLAP = block("flap_bearing/top")
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
    val PROPELLER_PISTON_TOP_LEFT = block("propeller_bearing/pistontopl")
    val PROPELLER_PISTON_TOP_RIGHT = block("propeller_bearing/pistontopr")
    val PROPELLER_PISTON_BOTTOM_LEFT = block("propeller_bearing/pistonbotl")
    val PROPELLER_PISTON_BOTTOM_RIGHT = block("propeller_bearing/pistonbotr")
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

    //PIPE DEATH
    val DUCT_CORE: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/core"))

    val DUCT_CONN_UP: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/connection/up"))
    val DUCT_CONN_DOWN: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/connection/down"))
    val DUCT_CONN_NORTH: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/connection/north"))
    val DUCT_CONN_SOUTH: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/connection/south"))
    val DUCT_CONN_EAST: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/connection/east"))
    val DUCT_CONN_WEST: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/connection/west"))

    val DUCT_RIM_UP: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/rim/up"))
    val DUCT_RIM_DOWN: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/rim/down"))
    val DUCT_RIM_NORTH: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/rim/north"))
    val DUCT_RIM_SOUTH: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/rim/south"))
    val DUCT_RIM_EAST: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/rim/east"))
    val DUCT_RIM_WEST: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/rim/west"))
    //END OF PIPE DEATH



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