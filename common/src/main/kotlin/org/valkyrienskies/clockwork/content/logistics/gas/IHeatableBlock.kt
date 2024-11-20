package org.valkyrienskies.clockwork.content.logistics.gas

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.state.properties.EnumProperty
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.mod.common.util.toJOMLD

interface IHeatableBlock {
    companion object {
        val GAS_HEAT_LEVEL: EnumProperty<GasHeatLevel> = EnumProperty.create("gas_heat_level", GasHeatLevel::class.java)
    }
}