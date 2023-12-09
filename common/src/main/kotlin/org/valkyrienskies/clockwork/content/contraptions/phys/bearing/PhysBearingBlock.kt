package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import com.simibubi.create.AllShapes
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import java.util.function.Consumer

class PhysBearingBlock(properties: Properties) : BearingBlock(properties), IBE<PhysBearingBlockEntity> {

    override fun use(state: BlockState, worldIn: Level, pos: BlockPos, player: Player, handIn: InteractionHand, hit: BlockHitResult): InteractionResult {
        if (!player.mayBuild()) return InteractionResult.FAIL
        if (player.isShiftKeyDown) return InteractionResult.FAIL
        if (player.getItemInHand(handIn)
                .isEmpty
        ) {
            if (worldIn.isClientSide) return InteractionResult.SUCCESS
            withBlockEntityDo(worldIn, pos, Consumer withBlockEntityDo@{ te: PhysBearingBlockEntity ->
                if (te.isRunning) {
                    // te.disassemble();
                    return@withBlockEntityDo
                }
                te.assembleNextTick = true
            })
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }

    override fun getBlockEntityClass(): Class<PhysBearingBlockEntity> {
        return PhysBearingBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out PhysBearingBlockEntity> {
        return ClockworkBlockEntities.PHYS_BEARING.get()
    }

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(FACING).axis
    }

    override fun getShape(state: BlockState, worldIn: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return AllShapes.MECHANICAL_PISTON[state.getValue(FACING)]
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        return face == state.getValue(FACING).opposite
    }

    companion object {
        fun getLight(state: BlockState?): Int {
            return 8
        }
    }
}
