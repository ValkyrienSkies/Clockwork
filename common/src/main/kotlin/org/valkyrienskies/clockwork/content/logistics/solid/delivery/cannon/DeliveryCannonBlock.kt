package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import com.simibubi.create.content.kinetics.belt.BeltBlock
import com.simibubi.create.content.logistics.chute.ChuteBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.Axis
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotGlobals

class DeliveryCannonBlock(properties: Properties) : HorizontalDirectionalBlock(properties), IBE<DeliveryCannonBlockEntity> {


    init {
        this.registerDefaultState(

            ((stateDefinition.any() as BlockState).setValue(
                HorizontalDirectionalBlock.FACING,
                Direction.NORTH
            ) as BlockState)
        )
    }


    override fun getBlockEntityClass(): Class<DeliveryCannonBlockEntity> {
        return DeliveryCannonBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out DeliveryCannonBlockEntity> {
        return ClockworkBlockEntities.DELIVERY_CANNON.get()
    }

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {

        // This is a really stupid way to do it, but neither == ALlBlocks.Depot nor anything else seems to work
        val desc = level.getBlockState(pos.below()).block.descriptionId
        return desc == "block.create.depot" || desc == "block.create.belt" || desc == "block.create.chute"
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        currentPos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (!canSurvive(state,level,currentPos))  {
            return Blocks.AIR.defaultBlockState()
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos)
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {

        return FrequencySlotGlobals.use(state, level, pos, player, hand, hit)
    }


    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(
            FACING
        )
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {

        val underBlock = context.level.getBlockState(context.clickedPos.below())
        if (underBlock.block.descriptionId=="block.create.belt") {
            val blockBelt = underBlock.block as BeltBlock
            val axis = blockBelt.getRotationAxis(underBlock)

            if (axis.isHorizontal) { // This forces the block to be aligned with belts
                if (axis == Axis.X) {
                    if (context.horizontalDirection == Direction.EAST || context.horizontalDirection == Direction.WEST) {
                        return defaultBlockState().setValue(
                            FACING,
                            Direction.EAST
                        ) as BlockState
                    }
                }
                else if (axis == Axis.Z) {
                    if (context.horizontalDirection == Direction.NORTH || context.horizontalDirection == Direction.SOUTH) {
                        return defaultBlockState().setValue(
                            FACING,
                            Direction.NORTH
                        ) as BlockState
                    }
                }
            }
        }

        return defaultBlockState().setValue(
            FACING,
            context.horizontalDirection.clockWise // The model is made in a weird way, so i have to do this
        ) as BlockState
    }

}