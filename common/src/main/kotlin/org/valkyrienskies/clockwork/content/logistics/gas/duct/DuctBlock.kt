package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.google.common.collect.ImmutableMap
import com.simibubi.create.AllSoundEvents
import com.simibubi.create.content.equipment.wrench.IWrenchable
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.Util
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.TextComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Vector2f
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.tools.screwdriver.IScrewdrivable
import org.valkyrienskies.clockwork.content.logistics.gas.INodeBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.DOWN_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.EAST_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.NORTH_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.SOUTH_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.UP_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.WEST_CONNECTION
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createPipeEdge
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createPipeNode
import org.valkyrienskies.clockwork.util.MathFunctions.isWithin
import org.valkyrienskies.clockwork.util.MathFunctions.removeAxis
import org.valkyrienskies.kelvin.util.GasHeatLevel
import org.valkyrienskies.kelvin.util.IEdgeBlock
import org.valkyrienskies.kelvin.util.IHeatableBlock
import org.valkyrienskies.kelvin.util.IHeatableBlock.Companion.GAS_HEAT_LEVEL
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.util.toJOMLD


class DuctBlock(properties: Properties) : Block(properties), INodeBlock, IDuct, IBE<DuctBlockEntity>, SimpleWaterloggedBlock, IWrenchable,
    IScrewdrivable, IHeatableBlock {

    //credit to NEEPMeat for the pipe implementation idea :3dsmile:

    private val shapes: HashMap<BlockState, VoxelShape> = HashMap()



    val DIR_SHAPES: Map<Direction, VoxelShape> = ImmutableMap.Builder<Direction, VoxelShape>()
        .put(Direction.NORTH, box(3.0, 3.0, 0.0, 13.0, 13.0, 3.0))
        .put(Direction.EAST, box(13.0, 3.0, 3.0, 16.0, 13.0, 13.0))
        .put(Direction.SOUTH, box(3.0, 3.0, 13.0, 13.0, 13.0, 16.0))
        .put(Direction.WEST, box(0.0, 3.0, 3.0, 3.0, 13.0, 13.0))
        .put(Direction.UP, box(3.0, 13.0, 3.0, 13.0, 16.0, 13.0))
        .put(Direction.DOWN, box(3.0, 0.0, 3.0, 13.0, 3.0, 13.0)).build()


    init {
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false)
            .setValue(NORTH_CONNECTION, DuctConnectionType.NONE)
            .setValue(EAST_CONNECTION, DuctConnectionType.NONE)
            .setValue(SOUTH_CONNECTION, DuctConnectionType.NONE)
            .setValue(WEST_CONNECTION, DuctConnectionType.NONE)
            .setValue(UP_CONNECTION, DuctConnectionType.NONE)
            .setValue(DOWN_CONNECTION, DuctConnectionType.NONE)
            .setValue(GAS_HEAT_LEVEL, GasHeatLevel.COOL))

        for (state: BlockState in this.stateDefinition.possibleStates)
        {
            shapes[state] = getShapeForState(state)
        }
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(
            NORTH_CONNECTION,
            EAST_CONNECTION,
            SOUTH_CONNECTION,
            WEST_CONNECTION,
            UP_CONNECTION,
            DOWN_CONNECTION,
            GAS_HEAT_LEVEL,
            BlockStateProperties.WATERLOGGED
        )

        super.createBlockStateDefinition(builder)
    }



    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }



    override fun createNode(pos: DuctNodePos): DuctNode {
        return createPipeNode(pos)
    }

    override fun getPistonPushReaction(state: BlockState): PushReaction {
        return PushReaction.DESTROY
    }

    override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult {
        if (!context.level.isClientSide) {
            val direction = context.clickedFace

            val hitPos = context.clickLocation

            val changeDirection = getUseDirection(direction, context.clickedPos, hitPos)
            val connected = state.getValue(DIR_TO_CONNECTION[changeDirection]!!) == DuctConnectionType.SIDE
            val newState = state.setValue(DIR_TO_CONNECTION[changeDirection]!!, if (connected) DuctConnectionType.FORCED else DuctConnectionType.SIDE)
            context.level.setBlockAndUpdate(context.clickedPos, newState)
            context.level.playSound(null, context.clickedPos, connectionChangeSound(), SoundSource.BLOCKS, 1f, 1f)
            onConnectionUpdate(context.level, state, newState, context.clickedPos)

            return InteractionResult.SUCCESS
        }
        return InteractionResult.SUCCESS
    }


    override fun canBeReplaced(state: BlockState, fluid: Fluid): Boolean {
        return false
    }


    fun onConnectionUpdate(
        world: Level,
        state: BlockState,
        newState: BlockState,
        pos: BlockPos
    ) {
        if (world.isClientSide) return

        val dimension = world.dimension().location()

        for (direction in Direction.values()) {
            if (state.getValue(DIR_TO_CONNECTION[direction]!!) != newState.getValue(DIR_TO_CONNECTION[direction]!!)) {
                val adjPos = pos.relative(direction)
                val adjState = world.getBlockState(adjPos)
                if (adjState.block is DuctBlock) {
                    if (newState.getValue(DIR_TO_CONNECTION[direction]!!).isConnected && adjState.getValue(DIR_TO_CONNECTION[direction.opposite]!!).isConnected) {
                        ClockworkMod.getKelvin().addEdge(pos.toDuctNodePos(dimension), adjPos.toDuctNodePos(dimension), createPipeEdge(pos.toDuctNodePos(dimension), adjPos.toDuctNodePos(dimension)))
                        withBlockEntityDo(world, pos) { blockEntity ->
                            blockEntity.setEdgeType(direction, ConnectionType.PIPE, clientPacket = false, silent = true)
                            if (world.getBlockEntity(adjPos) is DuctBlockEntity) {
                                (world.getBlockEntity(adjPos) as DuctBlockEntity).setEdgeType(
                                    direction.opposite,
                                    ConnectionType.PIPE,
                                    clientPacket = false,
                                    silent = true
                                )
                            }
                        }
                    } else {
                        ClockworkMod.getKelvin().removeEdge(pos.toDuctNodePos(dimension), adjPos.toDuctNodePos(dimension))
                        withBlockEntityDo(world, pos) { blockEntity ->
                            blockEntity.setEdgeType(direction, ConnectionType.NONE, clientPacket = false, silent = true)
                            if (world.getBlockEntity(adjPos) is DuctBlockEntity) {
                                (world.getBlockEntity(adjPos) as DuctBlockEntity).setEdgeType(
                                    direction.opposite,
                                    ConnectionType.NONE,
                                    clientPacket = false,
                                    silent = true
                                )
                            }
                        }
                    }
                } else if (adjState.block is IAxisAlignedDuct) {
                    if (direction.axis == (adjState.block as IAxisAlignedDuct).getAxis(adjState)) {
                        if (newState.getValue(DIR_TO_CONNECTION[direction]!!).isConnected && (adjState.block as IAxisAlignedDuct).canConnectTo(adjPos, pos, direction.opposite, world)) {
                            ClockworkMod.getKelvin().addEdge(pos.toDuctNodePos(dimension), adjPos.toDuctNodePos(dimension), createPipeEdge(pos.toDuctNodePos(dimension), adjPos.toDuctNodePos(dimension)))
                        } else {
                            ClockworkMod.getKelvin().removeEdge(pos.toDuctNodePos(dimension), adjPos.toDuctNodePos(dimension))
                        }
                    }
                } else if (adjState.block is IDuct) {
                    if (newState.getValue(DIR_TO_CONNECTION[direction]!!).isConnected && (adjState.block as IDuct).canConnectTo(adjPos, pos, direction.opposite, world)) {
                        ClockworkMod.getKelvin().addEdge(pos.toDuctNodePos(dimension), adjPos.toDuctNodePos(dimension), createPipeEdge(pos.toDuctNodePos(dimension), adjPos.toDuctNodePos(dimension)))
                    } else {
                        ClockworkMod.getKelvin().removeEdge(pos.toDuctNodePos(dimension), adjPos.toDuctNodePos(dimension))
                    }
                } else if (adjState.block is IEdgeBlock) {
                    if (newState.getValue(DIR_TO_CONNECTION[direction]!!).isConnected) {
                        (adjState.block as IEdgeBlock).tryConnectEdge(world, pos)
                    } else {
                        (adjState.block as IEdgeBlock).tryDisconnectEdge(world, pos)
                    }
                }

            }
        }
    }

    fun getUseDirection(direction: Direction, pos: BlockPos, hitPos: Vec3): Direction {
        val relative: Vector2f = removeAxis(direction.axis, hitPos.subtract(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()))
        var changeDirection = direction
        if (!relative.isWithin(0.5f, 0.5f, 0.25f)) {
            // X axis case
            if (relative.y > 0.75) changeDirection = Direction.SOUTH
            if (relative.y < 0.25) changeDirection = Direction.NORTH
            if (relative.x < 0.25) changeDirection = Direction.DOWN
            if (relative.x > 0.75) changeDirection = Direction.UP
            when (direction.axis) {
                Direction.Axis.Y -> changeDirection = changeDirection.getClockWise(Direction.Axis.Z)
                Direction.Axis.Z -> {
                    changeDirection = when (changeDirection.getClockWise(Direction.Axis.Y)) {
                        Direction.UP -> {
                            Direction.EAST
                        }
                        Direction.DOWN -> {
                            Direction.WEST
                        }
                        Direction.EAST -> {
                            Direction.DOWN
                        }
                        Direction.WEST -> {
                            Direction.UP
                        }
                        else -> {
                            Direction.NORTH
                        }
                    }
                }

                else -> {}
            }
        }
        return changeDirection
    }

    override fun onSneakWrenched(state: BlockState?, context: UseOnContext?): InteractionResult {
        return super.onSneakWrenched(state, context)
    }

    fun connectionChangeSound(): SoundEvent {
        return AllSoundEvents.WRENCH_ROTATE.mainEvent
    }

    protected fun getCenterShape(): VoxelShape {
        return box(3.0, 3.0, 3.0, 13.0, 13.0, 13.0)
    }

    fun getShapeForState(state: BlockState): VoxelShape {
        var shape: VoxelShape = getCenterShape()
        for (direction in Direction.values()) {
            if (state.getValue(DIR_TO_CONNECTION[direction]!!) === DuctConnectionType.SIDE) {
                shape = Shapes.or(shape, DIR_SHAPES[direction]!!)
            }
        }
        return shape
    }

    override fun getVisualShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return shapes[state]!!
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return shapes[state]!!
    }



    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? {
        val level: Level = ctx.level
        val pos: BlockPos = ctx.clickedPos
        return this.getConnectedState(level, this.defaultBlockState(), pos)
    }


    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        currentPos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (state.getValue(BlockStateProperties.WATERLOGGED))
        {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level))
        }

        val finalConnection = getConnectionType(state,currentPos,neighborState,neighborPos,direction,level)


        return state.setValue(DIR_TO_CONNECTION.get(direction)!!, finalConnection)
    }



    protected fun getConnectedState(level: BlockGetter, state: BlockState, pos: BlockPos): BlockState? {
        var state = state
        for (direction in Direction.values()) {
            val adjPos = pos.relative(direction)
            val connectionType = getConnectionType(state,pos,level.getBlockState(adjPos),adjPos,direction,level)
            state = state.setValue(
                DIR_TO_CONNECTION[direction]!!,
                connectionType
            )
        }
        return state.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).type === Fluids.WATER)



    }

    fun getConnectionType(state: BlockState, currentPos: BlockPos, neighborState: BlockState, neighborPos: BlockPos, direction: Direction, level: BlockGetter): DuctConnectionType {

        
        val type: DuctConnectionType = state.getValue(DIR_TO_CONNECTION[direction]!!)
        var forced = type == DuctConnectionType.FORCED
        var otherConnected = false

        val isConnectedEdgeBlock = neighborState.block is IEdgeBlock && (neighborState.block as IEdgeBlock).canConnectTo(level as Level,neighborPos,currentPos)

        val canConnect = canConnectTo(currentPos, neighborPos, direction.getOpposite(), level as Level) && level.getBlockState(neighborPos).block is IDuct

        if (neighborState.block is DuctBlock)
        {
            forced = forced || neighborState.getValue(DIR_TO_CONNECTION[direction.opposite]!!) == DuctConnectionType.FORCED
            otherConnected = neighborState.getValue(DIR_TO_CONNECTION[direction.opposite]!!).canBeChanged()

        } else if (neighborState.block is IDuct) {
            otherConnected =  (neighborState.block as IDuct).canConnectTo(neighborPos, currentPos, direction.opposite, level)
        }

        val finalConnection: DuctConnectionType = if (otherConnected && canConnect || isConnectedEdgeBlock) {
            DuctConnectionType.SIDE
        } else if (forced) {
            DuctConnectionType.FORCED
        } else {
            DuctConnectionType.NONE
        }

        if (level.isClientSide) return finalConnection

        val dimension = level.dimension().location()

        if (finalConnection.isConnected) {
            ClockworkMod.getKelvin().addEdge(currentPos.toDuctNodePos(dimension), neighborPos.toDuctNodePos(dimension), createPipeEdge(currentPos.toDuctNodePos(dimension), neighborPos.toDuctNodePos(dimension)))
            withBlockEntityDo(level, currentPos) { blockEntity ->
                blockEntity.setEdgeType(direction, ConnectionType.PIPE, clientPacket = false, silent = true)
            }
        } else {
            ClockworkMod.getKelvin().removeEdge(currentPos.toDuctNodePos(dimension), neighborPos.toDuctNodePos(dimension))
            withBlockEntityDo(level, currentPos) { blockEntity ->
                blockEntity.setEdgeType(direction, ConnectionType.NONE, clientPacket = false, silent = true)
            }
        }


        return finalConnection
    }

    override fun getBlockEntityClass(): Class<DuctBlockEntity> {
        return DuctBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out DuctBlockEntity> {
        return ClockworkBlockEntities.DUCT.get()
    }

    override fun onScrewdrived(state: BlockState, context: UseOnContext): InteractionResult {
        if (!context.level.isClientSide) {
            val direction = context.clickedFace

            val hitPos = context.clickLocation

            val changeDirection = getUseDirection(direction, context.clickedPos, hitPos)
            val connected = state.getValue(DIR_TO_CONNECTION[changeDirection]!!) == DuctConnectionType.SIDE

            if (!connected) return InteractionResult.SUCCESS
            playScrewSound(context.level, context.clickedPos)

            val dimension = context.level.dimension().location()
            val foundEdge = ClockworkMod.getKelvin().getEdgeBetween(context.clickedPos.toDuctNodePos(dimension), context.clickedPos.relative(changeDirection).toDuctNodePos(dimension))

            println(foundEdge?.type)
            foundEdge?.interact(context.player as ServerPlayer)

            return InteractionResult.SUCCESS
        }
        return InteractionResult.SUCCESS
    }

    override fun onSneakScrewdrived(state: BlockState, context: UseOnContext): InteractionResult {
        val direction = context.clickedFace

        val hitPos = context.clickLocation

        val changeDirection = getUseDirection(direction, context.clickedPos, hitPos)
        val connected = state.getValue(DIR_TO_CONNECTION[changeDirection]!!) == DuctConnectionType.SIDE

        if (!connected) return InteractionResult.FAIL
        if (context.level.isClientSide) return InteractionResult.SUCCESS


        playScrewSound(context.level, context.clickedPos)
        withBlockEntityDo(context.level, context.clickedPos) { blockEntity ->
            val type = blockEntity.cycleEdgeType(changeDirection)
            println(type.name)
            if (context.level.getBlockEntity(context.clickedPos.relative(changeDirection)) is DuctBlockEntity) {
                (context.level.getBlockEntity(context.clickedPos.relative(changeDirection)) as DuctBlockEntity).setEdgeType(
                    changeDirection.opposite,
                    type,
                    clientPacket = false,
                    silent = true
                )
            }
        }

        return InteractionResult.SUCCESS

    }

    companion object {
        val DIR_TO_CONNECTION: Map<Direction, EnumProperty<DuctConnectionType>> =
            ImmutableMap.builder<Direction, EnumProperty<DuctConnectionType>>()
                .put(Direction.NORTH, NORTH_CONNECTION)
                .put(Direction.EAST, EAST_CONNECTION)
                .put(Direction.SOUTH, SOUTH_CONNECTION)
                .put(Direction.WEST, WEST_CONNECTION)
                .put(Direction.DOWN, DOWN_CONNECTION)
                .put(Direction.UP, UP_CONNECTION).build()

        fun connectInDirection(world: BlockGetter, pos: BlockPos, state: BlockState, direction: Direction): Boolean {
            return state.getValue(DIR_TO_CONNECTION[direction]!!).canBeChanged()
        }
    }

}