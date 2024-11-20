package org.valkyrienskies.clockwork.content.logistics.gas.creative

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct

open class AbstractInfiniteSourceBlock(properties: Properties): Block(properties), IDuct, IHeatableBlock {
    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        if (level.getBlockEntity(other) is IDuct && (direction != Direction.UP && direction != Direction.DOWN)) {
            return true
        }
        return false
    }
}