package org.valkyrienskies.clockwork.content.kinetics.universal_shaft

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.foundation.block.IBE
import net.createmod.catnip.data.Iterate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkShapes
import org.valkyrienskies.clockwork.ClockworkSounds

class UniversalShaftBlock(properties: Properties?) : DirectionalKineticBlock(properties), IBE<UniversalShaftBlockEntity> {
    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(BlockStateProperties.FACING).axis
    }

    override fun getBlockEntityClass(): Class<UniversalShaftBlockEntity> {
        return UniversalShaftBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out UniversalShaftBlockEntity> {
        return ClockworkBlockEntities.UNIVERSAL_SHAFT.get()
    }

    override fun getShape(
        state: BlockState,
        worldIn: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return ClockworkShapes.UNIVERSAL_SHAFT.get(state.getValue<Direction>(FACING).opposite)
    }

    override fun getRenderShape(pState: BlockState?): RenderShape {
        return RenderShape.ENTITYBLOCK_ANIMATED
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        return face == state.getValue(BlockStateProperties.FACING)
    }

    override fun getPreferredFacing(context: BlockPlaceContext): Direction? {
        var prefferedSide: Direction? = null
        for (side in Iterate.directions) {
            val blockState = context.getLevel()
                .getBlockState(
                    context.getClickedPos()
                        .relative(side)
                )
            if (blockState.getBlock() is IRotate) {
                if ((blockState.getBlock() as IRotate).hasShaftTowards(
                        context.getLevel(), context.getClickedPos()
                            .relative(side), blockState, side.getOpposite()
                    )
                ) if (prefferedSide != null && prefferedSide.getAxis() !== side.getAxis()) {
                    prefferedSide = null
                    break
                } else {
                    prefferedSide = side.opposite
                }
            }
        }
        return prefferedSide
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        val be = level.getBlockEntity(pos) as? UniversalShaftBlockEntity ?: return InteractionResult.PASS
        if (player.isShiftKeyDown && player.getItemInHand(InteractionHand.MAIN_HAND) == ItemStack.EMPTY) {
            if (be.connectedBe != null) be.connectedBe!!.disconnect()
            be.disconnect()
            level.playSound(null, be.blockPos, ClockworkSounds.HOSE_RELEASE.mainEvent, SoundSource.BLOCKS, 1.0f, 1.0f)
            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }
}
