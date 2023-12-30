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
    val PHYSFLAP_EAST = block("phys_bearing/flapeast")
    val PHYSFLAP_WEST = block("phys_bearing/flapwest")
    val PHYSFLAP_NORTH = block("phys_bearing/flapnorth")
    val PHYSFLAP_SOUTH = block("phys_bearing/flapsouth")
    val PHYSCORNER_NE = block("phys_bearing/cornerne")
    val PHYSCORNER_NW = block("phys_bearing/cornernw")
    val PHYSCORNER_SE = block("phys_bearing/cornerse")
    val PHYSCORNER_SW = block("phys_bearing/cornersw")
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

    val CRYSTAL: PartialModel = PartialModel(ClockworkMod.asResource("item/auric_designator/crystal"))
    val CRYSTAL_OUTER: PartialModel = PartialModel(ClockworkMod.asResource("item/auric_designator/crystal_outer"))
    val CRYSTAL_INNER: PartialModel = PartialModel(ClockworkMod.asResource("item/auric_designator/crystal_inner"))

    val GYRO_BASE: PartialModel = block("gyro/base")



    //PIPE DEATH
    val CORE_X: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/core_x"))
    val CORE_Y: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/core_y"))
    val CORE_Z: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/core_z"))

    val CORE_LU_X: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/lu_x"))
    val CORE_RU_X: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/ru_x"))
    val CORE_LD_X: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/ld_x"))
    val CORE_RD_X: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/rd_x"))
    val CORE_UD_X: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/ud_x"))
    val CORE_U_X: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/u_x"))
    val CORE_D_X: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/d_x"))
    val CORE_LR_X: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/lr_x"))
    val CORE_L_X: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/l_x"))
    val CORE_R_X: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/r_x"))

    val CORE_LU_Y: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/lu_y"))
    val CORE_RU_Y: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/ru_y"))
    val CORE_LD_Y: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/ld_y"))
    val CORE_RD_Y: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/rd_y"))
    val CORE_UD_Y: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/ud_y"))
    val CORE_U_Y: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/u_y"))
    val CORE_D_Y: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/d_y"))
    val CORE_LR_Y: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/lr_y"))
    val CORE_L_Y: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/l_y"))
    val CORE_R_Y: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/r_y"))

    val CORE_LU_Z: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/lu_z"))
    val CORE_RU_Z: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/ru_z"))
    val CORE_LD_Z: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/ld_z"))
    val CORE_RD_Z: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/rd_z"))
    val CORE_UD_Z: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/ud_z"))
    val CORE_U_Z: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/u_z"))
    val CORE_D_Z: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/d_z"))
    val CORE_LR_Z: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/lr_z"))
    val CORE_L_Z: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/l_z"))
    val CORE_R_Z: PartialModel = PartialModel(ClockworkMod.asResource("block/heat_pipe/r_z"))
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