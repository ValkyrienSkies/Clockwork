package org.valkyrienskies.clockwork.content.logistics.heat.pipe

import com.simibubi.create.content.contraptions.ITransformableBlock
import com.simibubi.create.content.contraptions.StructureTransform
import com.simibubi.create.content.equipment.wrench.IWrenchable
import com.simibubi.create.foundation.block.IBE
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.PipeBlock
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class HeatPipeBlock(properties: Properties?) :
    PipeBlock(6 / 16f, properties), SimpleWaterloggedBlock,
    IWrenchable,
    IBE<HeatPipeBlockEntity>, ITransformableBlock {
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, BlockStateProperties.WATERLOGGED)
        super.createBlockStateDefinition(builder)
    }

    override fun transform(state: BlockState, transform: StructureTransform): BlockState? {
        return null
    }

    override fun getBlockEntityClass(): Class<HeatPipeBlockEntity> {
        return HeatPipeBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out HeatPipeBlockEntity> {
        return ClockworkBlockEntities.HEAT_PIPE.get()
    }

    override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult {
        return super<IWrenchable>.onWrenched(state, context)
    }
}