package org.valkyrienskies.clockwork.content.logistics.gas.filter

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.gas.duct.INodeBlock
import org.valkyrienskies.clockwork.kelvin.api.DuctNetwork
import org.valkyrienskies.clockwork.kelvin.api.DuctNode
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.NodeBehaviorType
import org.valkyrienskies.clockwork.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.clockwork.kelvin.api.nodes.PumpDuctNode

class FilterDuctBlock(properties: Properties) : RotatedPillarBlock(properties), IBE<FilterDuctBlockEntity>, INodeBlock {
    override fun getBlockEntityClass(): Class<FilterDuctBlockEntity> {
        return FilterDuctBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out FilterDuctBlockEntity> {
        return ClockworkBlockEntities.FILTER_DUCT.get()
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {

        if (direction.axis==level.getBlockState(self).getValue(BlockStateProperties.AXIS)) return true
        return false
    }
}