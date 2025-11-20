package org.valkyrienskies.clockwork.content.logistics.gas.generation.coal_burner

import com.simibubi.create.foundation.block.IBE
import dev.architectury.registry.fuel.FuelRegistry
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.kelvin.util.INodeBlock
import java.util.*


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
            } else if (be.storedFuelStack.item.equals(item.item)) {

                if (be.storedFuelStack.count + item.count <= item.maxStackSize) {
                    val copy = item.copy()
                    copy.count += be.storedFuelStack.count
                    be.storedFuelStack = copy
                    if (!player.isCreative) player.setItemInHand(hand, ItemStack.EMPTY)
                } else {
                    val copy = item.copy()
                    copy.count = item.maxStackSize
                    be.storedFuelStack = copy


                    if (!player.isCreative) item.count = be.storedFuelStack.count + item.count - item.maxStackSize
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

    override fun animateTick(state: BlockState, level: Level, pos: BlockPos, random: RandomSource) {
        if (state.getValue(AbstractFurnaceBlock.LIT) as Boolean) {
            val d = pos.x.toDouble() + 0.5
            val e = pos.y.toDouble() + 0.25
            val f = pos.z.toDouble() + 0.5
            if (random.nextDouble() < 0.1) {
                level.playLocalSound(d, e, f, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0f, 1.0f, false)
            }

            val direction = state.getValue(AbstractFurnaceBlock.FACING) as Direction
            val axis = direction.axis
            val g = 0.52
            val h = random.nextDouble() * 0.6 - 0.3
            val i = if (axis === Direction.Axis.X) direction.stepX.toDouble() * 0.52 else h
            val j = random.nextDouble() * 6.0 / 16.0
            val k = if (axis === Direction.Axis.Z) direction.stepZ.toDouble() * 0.52 else h
            level.addParticle(ParticleTypes.SMOKE, d + i, e + j, f + k, 0.0, 0.0, 0.0)
            level.addParticle(ParticleTypes.FLAME, d + i, e + j, f + k, 0.0, 0.0, 0.0)
        }
    }

    companion object {
        val LIT = BlockStateProperties.LIT;
    }
}