package org.valkyrienskies.clockwork.content.logistics.gas.generation.coal_burner

import com.simibubi.create.foundation.block.IBE
import dev.architectury.registry.fuel.FuelRegistry
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.gas.INodeBlock


class CoalBurnerBlock(properties: Properties) : HorizontalDirectionalBlock(properties), INodeBlock, IBE<CoalBurnerBlockEntity> {



    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LIT, false))
    }


    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        val be = level.getBlockEntity(pos) as CoalBurnerBlockEntity? ?: return  InteractionResult.PASS
        val item = player.getItemInHand(hand)

        if (player.isShiftKeyDown) return InteractionResult.PASS


        if (item == ItemStack.EMPTY && !be.storedFuelStack.isEmpty) {
            player.setItemInHand(hand, be.storedFuelStack)
            be.storedFuelStack = ItemStack.EMPTY
            return InteractionResult.SUCCESS
        }
        if (FuelRegistry.get(item)>0 && !player.isShiftKeyDown) {
            if (be.storedFuelStack.isEmpty) {
                be.storedFuelStack = item.copy()
                if (!player.isCreative) player.setItemInHand(hand, ItemStack.EMPTY)
            } else {
                if (be.storedFuelStack.item.equals(item.item)) {
                    if (be.storedFuelStack.count + item.count <= item.maxStackSize) {
                        be.storedFuelStack = item.copy()
                        if (!player.isCreative) player.setItemInHand(hand, ItemStack.EMPTY)
                    } else {
                        val copy = item.copy()
                        copy.count = item.maxStackSize
                        item.count = be.storedFuelStack.count + item.count - item.maxStackSize
                        be.storedFuelStack = copy


                        if (!player.isCreative) player.setItemInHand(hand, item)
                    }
                }
            }


            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }






    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }


    override fun getBlockEntityClass(): Class<CoalBurnerBlockEntity> {
        return  CoalBurnerBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out CoalBurnerBlockEntity> {
        return ClockworkBlockEntities.COAL_BURNER.get()
    }


    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, LIT)

        println()
        super.createBlockStateDefinition(builder)
    }


    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? {
        val level: Level = ctx.level
        val pos: BlockPos = ctx.clickedPos
        return this.defaultBlockState().setValue(
            FACING, ctx .horizontalDirection
                .opposite
        )
    }

    companion object {
        val LIT = BlockStateProperties.LIT;
    }
}