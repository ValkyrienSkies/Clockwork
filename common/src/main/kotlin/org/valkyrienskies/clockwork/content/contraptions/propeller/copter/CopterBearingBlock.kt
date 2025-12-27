package org.valkyrienskies.clockwork.content.contraptions.propeller.copter

import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING
import com.simibubi.create.foundation.block.IBE
import net.createmod.catnip.data.Couple
import net.createmod.catnip.lang.Lang
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.StringRepresentable
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlock.Companion.SPIN_DIRECTION
import java.util.function.Consumer

class CopterBearingBlock(properties: Properties) : BearingBlock(properties), IBE<CopterBearingBlockEntity> {
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(SPIN_DIRECTION)
        super.createBlockStateDefinition(builder)
    }


    override fun use(
        state: BlockState, worldIn: Level, pos: BlockPos, player: Player, handIn: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!player.mayBuild()) return InteractionResult.FAIL
        if (player.isShiftKeyDown) return InteractionResult.FAIL
        if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty) {
            if (!worldIn.isClientSide) {
                withBlockEntityDo(
                    worldIn,
                    pos,
                    Consumer<CopterBearingBlockEntity> withBlockEntityDo@{ te: CopterBearingBlockEntity ->
                        if (te.running) {
                            te.shutDown()
                            return@withBlockEntityDo
                        }
                        if (te.assembleCooldown <= 0) {
                            te.assembleNextTick = true
                        }
                    })
            }
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }

    override fun getBlockEntityClass(): Class<CopterBearingBlockEntity> {
        return CopterBearingBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out CopterBearingBlockEntity> {
        return ClockworkBlockEntities.COPTER_BEARING.get()
    }

    override fun hasShaftTowards(
        world: LevelReader,
        pos: BlockPos,
        state: BlockState,
        face: Direction
    ): Boolean {
        return face == state.getValue(FACING).opposite
    }

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(FACING).axis
    }
}
