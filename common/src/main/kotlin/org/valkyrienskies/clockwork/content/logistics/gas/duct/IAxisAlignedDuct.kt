package org.valkyrienskies.clockwork.content.logistics.gas.duct

import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState

interface IAxisAlignedDuct: IDuct {
    fun getAxisOf(state: BlockState): Direction.Axis? {
        return if (state.block is IAxisAlignedDuct) (state.block as IAxisAlignedDuct).getAxis(state) else null
    }

    fun getAxis(state: BlockState?): Direction.Axis?
}