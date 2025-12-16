package org.valkyrienskies.clockwork.content.logistics.gas.heater

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock
import com.simibubi.create.content.processing.basin.BasinBlockEntity
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel
import com.simibubi.create.foundation.block.IBE
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.kelvin.util.INodeBlock

class GasHeaterBlock(properties: Properties) : HorizontalDirectionalBlock(properties), IBE<GasHeaterBlockEntity>,
    INodeBlock, IHaveDuctStats {

    init {
        registerDefaultState(defaultBlockState().setValue(HEAT_LEVEL, HeatLevel.NONE).setValue(FACING, Direction.NORTH))

    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(HEAT_LEVEL)
        builder.add(FACING)
        super.createBlockStateDefinition(builder)

    }

    override fun getBlockEntityClass(): Class<GasHeaterBlockEntity> {
        return GasHeaterBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GasHeaterBlockEntity> {
        return ClockworkBlockEntities.GAS_HEATER.get()
    }


    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)

        if (level.isClientSide) return
        val blockEntity = level.getBlockEntity(pos.above()) as? BasinBlockEntity? ?: return
        blockEntity.notifyChangeOfContents()
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState()
            .setValue(
                HorizontalKineticBlock.HORIZONTAL_FACING, context.horizontalDirection
                    .opposite
            )
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        if (direction.axis != level.getBlockState(self).getValue(FACING).axis && direction != Direction.DOWN) return false

        return super.canConnectTo(self, other, direction, level)
    }

    override fun getAdditionalInfoLines(): List<Component> {
        return listOf(Component.translatable("vs_clockwork.gas_heater.function").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC))
    }

}
