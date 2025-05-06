package org.valkyrienskies.clockwork.content.contraptions.flap

import com.simibubi.create.content.contraptions.ITransformableBlock
import com.simibubi.create.content.contraptions.StructureTransform
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import java.util.function.Consumer

open class FlapBearingBlock(properties: Properties?) : BearingBlock(properties), IBE<FlapBearingBlockEntity>,
    ITransformableBlock {


    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)
        super.createBlockStateDefinition(builder)
    }

    protected open fun getFacingForPlacement(context: BlockPlaceContext): Direction? {
        var facing = context.nearestLookingDirection.opposite
        if (context.player != null && context.player!!.isShiftKeyDown) facing = facing.opposite
        return facing
    }

    protected open fun getAxisAlignmentForPlacement(context: BlockPlaceContext): Boolean {
        return context.horizontalDirection.axis === Direction.Axis.X
    }

    override fun use(
        state: BlockState,
        worldIn: Level,
        pos: BlockPos,
        player: Player,
        handIn: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!player.mayBuild()) return InteractionResult.FAIL
        if (player.isShiftKeyDown) return InteractionResult.FAIL
        if (player.getItemInHand(handIn).isEmpty) {
            if (!worldIn.isClientSide) {
                withBlockEntityDo(
                    worldIn,
                    pos,
                    Consumer withBlockEntityDo@{ te: FlapBearingBlockEntity ->
                        if (te.isRunning) {
                            te.disassemble()
                            return@withBlockEntityDo
                        }
                        te.assembleNextTick = true
                    }
                )
            }
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val facing = getFacingForPlacement(context)
        val faceAxis = facing!!.axis

        val alongFirst: Boolean
        if (faceAxis.isHorizontal) alongFirst = faceAxis === Direction.Axis.Z
        else alongFirst = getAxisAlignmentForPlacement(context)

        println(alongFirst)

        return defaultBlockState()
            .setValue(FACING, facing)
            .setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, alongFirst)
    }


    override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult {
        val resultType = super.onWrenched(state, context)
        if (!context.level.isClientSide && resultType.consumesAction()) withBlockEntityDo(
            context.level,
            context.clickedPos,
            FlapBearingBlockEntity::disassemble
        )
        return resultType
    }

    override fun transform(state: BlockState, transform: StructureTransform): BlockState {
        var state = state
        if (transform.mirror != null) {
            state = mirror(state, transform.mirror)
        }

        if (transform.rotationAxis === Direction.Axis.Y) {
            return rotate(state, transform.rotation)
        }

        val newFacing = transform.rotateFacing(state.getValue(FACING))
        if (transform.rotationAxis === newFacing.axis && transform.rotation.ordinal % 2 == 1) {
            state = state.cycle(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)
        }
        return state.setValue(FACING, newFacing)
    }

    override fun getBlockEntityClass(): Class<FlapBearingBlockEntity> {
        return FlapBearingBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out FlapBearingBlockEntity> {
        return ClockworkBlockEntities.FLAP_BEARING.get()
    }
}
