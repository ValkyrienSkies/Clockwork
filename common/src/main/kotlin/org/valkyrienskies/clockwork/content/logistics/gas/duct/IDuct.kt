package org.valkyrienskies.clockwork.content.logistics.gas.duct

import net.minecraft.world.level.block.state.properties.EnumProperty
import org.valkyrienskies.kelvin.util.INodeBlock

interface IDuct: INodeBlock {
    companion object {
        val NORTH_CONNECTION: EnumProperty<DuctConnectionType> = EnumProperty.create("north", DuctConnectionType::class.java)
        val SOUTH_CONNECTION: EnumProperty<DuctConnectionType> = EnumProperty.create("south", DuctConnectionType::class.java)
        val EAST_CONNECTION: EnumProperty<DuctConnectionType> = EnumProperty.create("east", DuctConnectionType::class.java)
        val WEST_CONNECTION: EnumProperty<DuctConnectionType> = EnumProperty.create("west", DuctConnectionType::class.java)
        val UP_CONNECTION: EnumProperty<DuctConnectionType> = EnumProperty.create("up", DuctConnectionType::class.java)
        val DOWN_CONNECTION: EnumProperty<DuctConnectionType> = EnumProperty.create("down", DuctConnectionType::class.java)
    }

}