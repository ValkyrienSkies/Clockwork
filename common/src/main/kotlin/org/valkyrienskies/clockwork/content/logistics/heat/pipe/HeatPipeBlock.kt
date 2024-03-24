package org.valkyrienskies.clockwork.content.logistics.heat.pipe


import com.simibubi.create.content.contraptions.ITransformableBlock
import com.simibubi.create.content.contraptions.StructureTransform
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour
import com.simibubi.create.content.equipment.wrench.IWrenchable
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockRotation
import com.simibubi.create.content.fluids.pipes.VanillaFluidTargets
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.Iterate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.PipeBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.ticks.TickPriority
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import java.util.Arrays

class HeatPipeBlock(properties: Properties) :
    PipeBlock(4 / 16f, properties),
    SimpleWaterloggedBlock,
    IWrenchable,
    IBE<HeatPipeBlockEntity>,
    ITransformableBlock {

    init {
        registerDefaultState(super.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false))
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.INVISIBLE
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, BlockStateProperties.WATERLOGGED)
        super.createBlockStateDefinition(builder)
    }

    override fun transform(state: BlockState?, transform: StructureTransform?): BlockState {
        return FluidPipeBlockRotation.transform(state, transform)
    }

    override fun getBlockEntityClass(): Class<HeatPipeBlockEntity> {
        return HeatPipeBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out HeatPipeBlockEntity> {
        return ClockworkBlockEntities.HEAT_PIPE.get()
    }

    override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult {
        return super.onWrenched(state, context)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val FluidState = context.level
            .getFluidState(context.clickedPos)
        return updateBlockState(
            defaultBlockState(), context.nearestLookingDirection, null, context.level,
            context.clickedPos
        ).setValue(BlockStateProperties.WATERLOGGED, FluidState.type === Fluids.WATER)
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighbourState: BlockState,
        world: LevelAccessor,
        pos: BlockPos,
        neighbourPos: BlockPos
    ): BlockState {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) world.scheduleTick(
            pos,
            Fluids.WATER,
            Fluids.WATER.getTickDelay(world)
        )
        if (isOpenAt(state, direction) && neighbourState.hasProperty(
                BlockStateProperties.WATERLOGGED
            )
        ) world.scheduleTick(pos, this, 1, TickPriority.HIGH)

        withBlockEntityDo(world, pos) { it.markConnectionsDirty() }
        return updateBlockState(state, direction, direction.opposite, world, pos)
    }

    fun updateBlockState(
        state: BlockState, preferredDirection: Direction, ignore: Direction?,
        world: BlockAndTintGetter, pos: BlockPos
    ): BlockState {
        var state = state
        val bracket = BlockEntityBehaviour.get(world, pos, BracketedBlockEntityBehaviour.TYPE)
        if (bracket != null && bracket.isBracketPresent) return state
        val prevState = state
        val prevStateSides = Arrays.stream(Iterate.directions)
            .map { key: Direction? ->
                PROPERTY_BY_DIRECTION[key]
            }
            .filter { property: BooleanProperty? ->
                prevState.getValue(
                    property
                )
            }
            .count().toInt()

        // Update sides that are not ignored
        for (d in Iterate.directions) if (d != ignore) {
            val shouldConnect = canConnectTo(world, pos.relative(d), world.getBlockState(pos.relative(d)), d)
            state = state.setValue(PROPERTY_BY_DIRECTION[d], shouldConnect)
        }

        // See if it has enough connections
        var connectedDirection: Direction? = null
        for (d in Iterate.directions) {
            if (isOpenAt(state, d)) {
                if (connectedDirection != null) return state
                connectedDirection = d
            }
        }

        // Add opposite end if only one connection
        if (connectedDirection != null) return state.setValue(PROPERTY_BY_DIRECTION[connectedDirection.opposite], true)

        // If we can't connect to anything and weren't connected before, do nothing
        return if (prevStateSides == 2) prevState else state.setValue(PROPERTY_BY_DIRECTION[preferredDirection], true)
            .setValue(PROPERTY_BY_DIRECTION[preferredDirection.opposite], true)

        // Use preferred
    }

    fun isOpenAt(state: BlockState, direction: Direction?): Boolean {
        return state.getValue(PROPERTY_BY_DIRECTION[direction])
    }

    fun canConnectTo(world: BlockAndTintGetter?, neighbourPos: BlockPos?, neighbour: BlockState?, direction: Direction): Boolean {
        if (VanillaFluidTargets.shouldPipesConnectTo(neighbour)) return true
        val bracket = BlockEntityBehaviour.get(world, neighbourPos, BracketedBlockEntityBehaviour.TYPE)
        return if (isPipe(neighbour)) bracket == null else false
    }

    fun isPipe(neighbour: BlockState?): Boolean {
        return neighbour!!.getBlock() is HeatPipeBlock
    }
}
