package org.valkyrienskies.clockwork.content.contraptions.phys.gimbal

import com.simibubi.create.AllShapes
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import java.util.function.Consumer

class GimbalBearingBlock(properties: Properties) : BearingBlock(properties), IBE<GimbalBearingBlockEntity> {

    override fun use(state: BlockState, worldIn: Level, pos: BlockPos, player: Player, handIn: InteractionHand, hit: BlockHitResult): InteractionResult {
        if (!player.mayBuild()) return InteractionResult.FAIL
        if (player.isShiftKeyDown) return InteractionResult.FAIL
        if (handIn == InteractionHand.OFF_HAND) return InteractionResult.FAIL
        if (!player.getItemInHand(handIn).isEmpty) return InteractionResult.PASS
        if (worldIn.isClientSide) return InteractionResult.SUCCESS

        withBlockEntityDo(worldIn, pos, Consumer { be: GimbalBearingBlockEntity ->
            when (be.isRunning) {
                true -> be.disassemble()
                false -> be.assembleNextTick = true
            }
        })
        return InteractionResult.SUCCESS
    }

    override fun onWrenched(state: BlockState?, context: UseOnContext): InteractionResult {
        if (context.level.isClientSide) return super.onWrenched(state, context)
        val be = context.level.getBlockEntity(context.clickedPos) as? GimbalBearingBlockEntity ?: return InteractionResult.FAIL
        if (be.isRunning) return InteractionResult.FAIL
        return super.onWrenched(state, context)
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, fromPos: BlockPos, isMoving: Boolean) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving)
        if (level.isClientSide) return
        val be = level.getBlockEntity(pos) as? GimbalBearingBlockEntity ?: return
        be.onNeighborChanged()
    }

    override fun getBlockEntityClass(): Class<GimbalBearingBlockEntity> = GimbalBearingBlockEntity::class.java
    override fun getBlockEntityType(): BlockEntityType<out GimbalBearingBlockEntity> = ClockworkBlockEntities.GIMBAL_BEARING.get()
    override fun getRotationAxis(state: BlockState): Direction.Axis = state.getValue(FACING).axis

    override fun getShape(state: BlockState, worldIn: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return AllShapes.MECHANICAL_PISTON[state.getValue(FACING)]
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        return face == state.getValue(FACING).opposite
    }
}
