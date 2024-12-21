package org.valkyrienskies.clockwork.content.logistics.gas.duct

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.properties.EnumProperty
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock.Companion.connectInDirection
import org.valkyrienskies.kelvin.util.IHeatableBlock

interface IDuct: IHeatableBlock {
    companion object {
        val NORTH_CONNECTION: EnumProperty<DuctConnectionType> = EnumProperty.create("north", DuctConnectionType::class.java)
        val SOUTH_CONNECTION: EnumProperty<DuctConnectionType> = EnumProperty.create("south", DuctConnectionType::class.java)
        val EAST_CONNECTION: EnumProperty<DuctConnectionType> = EnumProperty.create("east", DuctConnectionType::class.java)
        val WEST_CONNECTION: EnumProperty<DuctConnectionType> = EnumProperty.create("west", DuctConnectionType::class.java)
        val UP_CONNECTION: EnumProperty<DuctConnectionType> = EnumProperty.create("up", DuctConnectionType::class.java)
        val DOWN_CONNECTION: EnumProperty<DuctConnectionType> = EnumProperty.create("down", DuctConnectionType::class.java)
    }

    fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        if (self.distSqr(other) > 1.0) return false
        val selfState = level.getBlockState(self)
        val otherState = level.getBlockState(other)


        return true
    }
}