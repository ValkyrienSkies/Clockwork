package org.valkyrienskies.clockwork.content.physicalities.speed_gauge

import com.simibubi.create.content.kinetics.base.KineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.block.state.properties.Property
import org.valkyrienskies.clockwork.content.physicalities.ballast.BallastBlockEntity
import org.valkyrienskies.clockwork.platform.SharedValues

class SpeedGaugeBlock(properties: Properties?): BaseEntityBlock(properties), IBE<SpeedGaugeBlockEntity> {

    override fun getBlockEntityClass(): Class<SpeedGaugeBlockEntity> {
        return SpeedGaugeBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out SpeedGaugeBlockEntity> {
        return ClockworkBlockEntities.SPEED_GAUGE.get()
    }



    init {
        this.registerDefaultState(
            ((stateDefinition.any() as BlockState).setValue(
                HorizontalDirectionalBlock.FACING,
                Direction.NORTH
            ) as BlockState)
        )
    }

    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(
            HorizontalDirectionalBlock.FACING,
            context.horizontalDirection.opposite
        ) as BlockState
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue(
            HorizontalDirectionalBlock.FACING,
            rotation.rotate(state.getValue(HorizontalDirectionalBlock.FACING) as Direction)
        ) as BlockState
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return state.rotate(mirror.getRotation(state.getValue(HorizontalDirectionalBlock.FACING) as Direction))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(
            *arrayOf<Property<*>>(
                HorizontalDirectionalBlock.FACING
            )
        )
    }


}