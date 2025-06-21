package org.valkyrienskies.clockwork

import com.simibubi.create.AllSpriteShifts
import com.simibubi.create.foundation.block.connected.AllCTTypes
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry
import com.simibubi.create.foundation.block.connected.CTSpriteShifter
import com.simibubi.create.foundation.block.connected.CTType
import com.simibubi.create.foundation.block.render.SpriteShiftEntry
import com.simibubi.create.foundation.block.render.SpriteShifter

object ClockworkSpriteShifts {
    val BALLOON_CASING = omni("balloon_casing")



    val DUCT_TANK: CTSpriteShiftEntry = getCT(AllCTTypes.RECTANGLE, "duct_tank")
    val DUCT_TANK_TOP: CTSpriteShiftEntry = getCT(AllCTTypes.RECTANGLE, "duct_tank_top")


    //
    private fun omni(name: String): CTSpriteShiftEntry {
        return getCT(AllCTTypes.OMNIDIRECTIONAL, name)
    }

    private fun horizontal(name: String): CTSpriteShiftEntry {
        return getCT(AllCTTypes.HORIZONTAL, name)
    }

    private fun vertical(name: String): CTSpriteShiftEntry {
        return getCT(AllCTTypes.VERTICAL, name)
    }

    //
    private operator fun get(originalLocation: String, targetLocation: String): SpriteShiftEntry {
        return SpriteShifter.get(ClockworkMod.asResource(originalLocation), ClockworkMod.asResource(targetLocation))
    }

    private fun getCT(type: CTType, blockTextureName: String, connectedTextureName: String): CTSpriteShiftEntry {
        return CTSpriteShifter.getCT(
            type,
            ClockworkMod.asResource("block/$blockTextureName"),
            ClockworkMod.asResource("block/" + connectedTextureName + "_connected")
        )
    }

    private fun getCT(type: CTType, blockTextureName: String): CTSpriteShiftEntry {
        return getCT(type, blockTextureName, blockTextureName)
    }
}