package org.valkyrienskies.clockwork.content.logistics.gas

import net.minecraft.world.level.block.state.properties.EnumProperty

interface IHeatableBlock {
    companion object {
        val GAS_HEAT_LEVEL: EnumProperty<GasHeatLevel> = EnumProperty.create("gas_heat_level", GasHeatLevel::class.java)
    }
}