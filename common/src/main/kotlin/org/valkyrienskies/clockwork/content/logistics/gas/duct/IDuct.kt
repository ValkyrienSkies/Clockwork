package org.valkyrienskies.clockwork.content.logistics.gas.duct

import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockState

interface IDuct {
    fun canConnectTo(self: BlockPos, other: BlockPos, level: BlockGetter): Boolean
}