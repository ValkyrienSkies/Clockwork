package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.PipeBlock
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class DuctBlock(properties: Properties) : PipeBlock(4/16f, properties), IDuct, IBE<DuctBlockEntity>, SimpleWaterloggedBlock {

    init {
        registerDefaultState(super.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false))
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, level: BlockGetter): Boolean {
        if (self.distSqr(other) > 1.0) return false
        val selfState = level.getBlockState(self)
        val otherState = level.getBlockState(other)

        if (otherState.block !is IDuct) return false

        if (otherState.block is IAxisAlignedDuct) return (otherState.block as IAxisAlignedDuct).canConnectTo(other, self, level)

        return true
    }

    override fun getBlockEntityClass(): Class<DuctBlockEntity> {
        return DuctBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out DuctBlockEntity> {
        return ClockworkBlockEntities.DUCT.get()
    }
}