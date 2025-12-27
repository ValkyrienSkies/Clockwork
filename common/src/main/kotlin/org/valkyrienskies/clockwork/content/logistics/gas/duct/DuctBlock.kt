package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.google.common.collect.ImmutableMap
import com.simibubi.create.AllSoundEvents
import com.simibubi.create.content.equipment.wrench.IWrenchable
import com.simibubi.create.foundation.block.IBE
import net.minecraft.ChatFormatting
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Explosion
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
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Vector2f
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.ClockworkSoundScapes
import org.valkyrienskies.clockwork.content.curiosities.tools.screwdriver.IScrewdrivable
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.DOWN_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.EAST_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.NORTH_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.SOUTH_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.UP_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.WEST_CONNECTION
import org.valkyrienskies.clockwork.util.KelvinParticleHelper
import org.valkyrienskies.clockwork.util.MathFunctions.isWithin
import org.valkyrienskies.clockwork.util.MathFunctions.removeAxis
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.kelvin.util.IEdgeBlock
import org.valkyrienskies.kelvin.util.INodeBlock
import org.valkyrienskies.kelvin.util.INodeBlockEntity
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.util.toJOMLD


class DuctBlock(properties: Properties) : Block(properties), INodeBlock, IDuct, IBE<DuctBlockEntity>,
    SimpleWaterloggedBlock, IWrenchable,
    IScrewdrivable, IHaveDuctStats {

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
        registerDefaultState(
            defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false)
                .setValue(NORTH_CONNECTION, DuctConnectionType.NONE)
                .setValue(EAST_CONNECTION, DuctConnectionType.NONE)
                .setValue(SOUTH_CONNECTION, DuctConnectionType.NONE)
                .setValue(WEST_CONNECTION, DuctConnectionType.NONE)
                .setValue(UP_CONNECTION, DuctConnectionType.NONE)
                .setValue(DOWN_CONNECTION, DuctConnectionType.NONE)
        )

        for (state: BlockState in this.stateDefinition.possibleStates) {
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
            BlockStateProperties.WATERLOGGED
        )

        super.createBlockStateDefinition(builder)
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        fromPos: BlockPos,
        isMoving: Boolean
    ) {
        val neighborState = level.getBlockState(fromPos)

        val delta = fromPos.subtract(pos)
        val direction = Direction.fromDelta(delta.x, delta.y, delta.z)
        if (neighborState.block is INodeBlock || neighborState.block is IEdgeBlock) {
            val finalConnection = getConnection(state, pos, neighborState, fromPos, direction!!, level)
            val newState = state.setValue(DIR_TO_CONNECTION[direction]!!, finalConnection)
            level.setBlockAndUpdate(pos, newState)
        } // If neighbor was an INode or IEdge and then turned into something else, set ConnectionType to None
        else if ((block is INodeBlock || block is IEdgeBlock) && state.getValue(DIR_TO_CONNECTION[direction]!!) != DuctConnectionType.FORCED_OFF) {
            val newState = state.setValue(DIR_TO_CONNECTION[direction]!!, DuctConnectionType.NONE)
            level.setBlockAndUpdate(pos, newState)
        }

        super.neighborChanged(state, level, pos, block, fromPos, isMoving)
    }

    override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult {

        val direction = context.clickedFace

        val hitPos = context.clickLocation

        val changeDirection = getUseDirection(direction, context.clickedPos, hitPos)
        val value = state.getValue(DIR_TO_CONNECTION[changeDirection]!!)

        val neighborBlock = context.level.getBlockState(context.clickedPos.relative(changeDirection)).block
        if (neighborBlock !is INodeBlock && neighborBlock !is IEdgeBlock) return InteractionResult.FAIL
        if (context.level.isClientSide) return InteractionResult.SUCCESS

        // I really didn't want to add TEMP_ON, but I had to, due to `neighborChanged()` getting called whenever a block is set.
        // Otherwise, I'd be able to update both ducts at once to set them both to SIDE
        var newValue = when (value) {
            DuctConnectionType.FORCED_OFF -> DuctConnectionType.TEMP_ON
            DuctConnectionType.SIDE -> DuctConnectionType.FORCED_OFF
            DuctConnectionType.LEAK -> DuctConnectionType.NONE
            else -> DuctConnectionType.SIDE
        }

        if (newValue == DuctConnectionType.TEMP_ON && context.level.getBlockState(
                context.clickedPos.relative(
                    changeDirection
                )
            ).block !is DuctBlock
        )
            newValue = DuctConnectionType.SIDE

        val newState = state.setValue(DIR_TO_CONNECTION[changeDirection]!!, newValue)
        context.level.setBlockAndUpdate(context.clickedPos, newState)

        context.level.playSound(null, context.clickedPos, connectionChangeSound(), SoundSource.BLOCKS, 1f, 1f)


        return InteractionResult.SUCCESS
    }


    fun getConnectedState(level: BlockGetter, state: BlockState, pos: BlockPos): BlockState? {
        var state = state
        for (direction in Direction.entries) {
            val adjPos = pos.relative(direction)
            val connectionType = getConnection(state, pos, level.getBlockState(adjPos), adjPos, direction, level)
            state = state.setValue(DIR_TO_CONNECTION[direction]!!, connectionType)
        }
        return state.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).type === Fluids.WATER)
    }

    fun getConnection(
        state: BlockState,
        currentPos: BlockPos,
        neighborState: BlockState,
        neighborPos: BlockPos,
        direction: Direction,
        level: BlockGetter
    ): DuctConnectionType {

        val type: DuctConnectionType = state.getValue(DIR_TO_CONNECTION[direction]!!)
        var forced = type == DuctConnectionType.FORCED_OFF
        var temp = type == DuctConnectionType.TEMP_ON
        var leak = type == DuctConnectionType.LEAK
        var otherConnected = false

        val isConnectedEdgeBlock =
            neighborState.block is IEdgeBlock && (neighborState.block as IEdgeBlock).canConnectTo(
                level as Level,
                neighborPos,
                currentPos
            )
        if (neighborState.block is DuctBlock) {
            val neighborValue = neighborState.getValue(DIR_TO_CONNECTION[direction.opposite]!!)
            forced = forced || neighborValue == DuctConnectionType.FORCED_OFF
            temp = temp || neighborValue == DuctConnectionType.TEMP_ON
            otherConnected = neighborValue.canBeChanged()
        } else if (neighborState.block is INodeBlock)
            otherConnected =
                (neighborState.block as INodeBlock).canConnectTo(neighborPos, currentPos, direction.opposite, level)

        otherConnected = otherConnected || isConnectedEdgeBlock

        val finalConnection: DuctConnectionType = when {
            temp && (!otherConnected || forced) -> DuctConnectionType.TEMP_ON
            leak -> DuctConnectionType.LEAK
            forced -> DuctConnectionType.FORCED_OFF
            otherConnected -> DuctConnectionType.SIDE
            else -> DuctConnectionType.NONE
        }

        if ((level as? Level)?.isClientSide != false || isConnectedEdgeBlock) return finalConnection

        val blockEntity = level.getBlockEntity(currentPos) as? DuctBlockEntity ?: return finalConnection


        val neighborBe = level.getBlockEntity(neighborPos)
        if (neighborBe !is INodeBlockEntity) {
            blockEntity.clearEdgeType(direction)
            return finalConnection
        }

        val neighborDuctNodePos = neighborBe.getDuctNodePosition()
        val storedType = blockEntity.DIR_TO_CONNECTION_TYPE[direction] ?: DuctEdgeType.PIPE
        val connectionType = if (finalConnection.isConnected) (if (storedType != DuctEdgeType.NONE) storedType else DuctEdgeType.PIPE) else DuctEdgeType.NONE

        //if (type != finalConnection)
        blockEntity.setEdgeType(direction, neighborDuctNodePos, connectionType, clientPacket = false, silent = false, forced = true)


        return finalConnection
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)

        level.setBlockAndUpdate(pos, this.getConnectedState(level, state, pos) ?: return)

        //println("placed duct at $pos in ${level.dimension().location()}")
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun createNode(pos: DuctNodePos): DuctNode {
        return  DuctPipeNode(pos = pos, volume = 0.25, maxPressure = 16375049.0, maxTemperature = 1478.0)
    }

    override fun canBeReplaced(state: BlockState, fluid: Fluid): Boolean {
        return false
    }

    fun getUseDirection(direction: Direction, pos: BlockPos, hitPos: Vec3): Direction {
        val relative: Vector2f =
            removeAxis(direction.axis, hitPos.subtract(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()))
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
                        Direction.UP -> Direction.EAST
                        Direction.DOWN -> Direction.WEST
                        Direction.EAST -> Direction.DOWN
                        Direction.WEST -> Direction.UP
                        else -> Direction.NORTH
                    }
                }

                else -> {}
            }
        }
        return changeDirection
    }

    fun connectionChangeSound(): SoundEvent {
        return AllSoundEvents.WRENCH_ROTATE.mainEvent
    }

    protected fun getCenterShape(): VoxelShape {
        return box(3.0, 3.0, 3.0, 13.0, 13.0, 13.0)
    }

    fun getShapeForState(state: BlockState): VoxelShape {
        var shape: VoxelShape = getCenterShape()
        for (direction in Direction.entries) {
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

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        currentPos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) level.scheduleTick(
            currentPos,
            Fluids.WATER,
            Fluids.WATER.getTickDelay(level)
        )

        return state
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        return super.getStateForPlacement(context)
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
            val leaking = state.getValue(DIR_TO_CONNECTION[changeDirection]!!) == DuctConnectionType.LEAK

            if (leaking) {
                playScrewSound(context.level, context.clickedPos)
                context.level.setBlockAndUpdate(
                    context.clickedPos,
                    state.setValue(DIR_TO_CONNECTION[changeDirection]!!, DuctConnectionType.NONE)
                )
                return InteractionResult.SUCCESS
            }

            if (!connected) return InteractionResult.SUCCESS
            playScrewSound(context.level, context.clickedPos)

            val dimension = context.level.dimension().location()
            val foundEdge = ClockworkMod.getKelvin().getEdgeBetween(
                context.clickedPos.toDuctNodePos(dimension),
                context.clickedPos.relative(changeDirection).toDuctNodePos(dimension)
            )

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


        val otherDuctNodePos =
            (context.level.getBlockEntity(context.clickedPos.relative(changeDirection)) as? INodeBlockEntity)?.getDuctNodePosition()
                ?: return InteractionResult.FAIL



        playScrewSound(context.level, context.clickedPos)
        withBlockEntityDo(context.level, context.clickedPos) { blockEntity ->
            val type = blockEntity.cycleEdgeType(changeDirection)
            if (context.level.getBlockEntity(context.clickedPos.relative(changeDirection)) is DuctBlockEntity) {
                (context.level.getBlockEntity(context.clickedPos.relative(changeDirection)) as DuctBlockEntity).setEdgeType(
                    changeDirection.opposite,
                    otherDuctNodePos,
                    type,
                    clientPacket = false,
                    silent = true
                )
            }
        }

        return InteractionResult.SUCCESS

    }

    fun randomPos(deviation: Double, random: RandomSource): Double {
        return (0.5-deviation/2.0)+random.nextDouble()*deviation
    }

    override fun animateTick(state: BlockState, level: Level, pos: BlockPos, random: RandomSource) {

        val be = level.getBlockEntity(pos) as? DuctBlockEntity ?: return
        val ductNodePos = be.getDuctNodePosition()
        val pressure = ClockworkModClient.getKelvin().getPressureAt(ductNodePos)
        if (pressure < 1000) return

        // Handle hissing sound
        val maxPressure = 16375049.0
        if (pressure > 0.7 * maxPressure) {
            val pitch = 2 * pressure / maxPressure
            val scape = ClockworkSoundScapes.AmbienceGroup.GAS_HISS
            ClockworkSoundScapes.play(scape, pos, pitch.toFloat())
        }

        // Handle leaking particles
        for (dir in Direction.entries) {
            if (state.getValue(DIR_TO_CONNECTION[dir]!!) != DuctConnectionType.LEAK) continue
            val normal = dir.normal.toJOMLD()
            val speed = normal.add(Vector3d(random.nextDouble()-0.5, random.nextDouble()-0.5, random.nextDouble()-0.5)).mul(0.1)
            val position = pos.toJOMLD().add(Vector3d(0.5+normal.x, 0.5+normal.y, 0.5+normal.z))

            for (i in 0..5)
            KelvinParticleHelper.spawnParticleWithRatio(level as ClientLevel, ductNodePos, position, speed )
        }


        // Handle gas movement particles
        if (!ClockworkConfig.CLIENT.renderDuctParticles) return

        val network = ClockworkModClient.getKelvin()
        val node = network.nodeInfo.get(ductNodePos) ?: return
        if (node.currentPressure - node.previousPressure < 250) return

        val speed = Vector3d(random.nextDouble()-0.5, random.nextDouble()-0.5, random.nextDouble()-0.5).mul(0.1)
        KelvinParticleHelper.spawnParticleWithRatio(level as ClientLevel, ductNodePos, pos.toJOMLD().add(Vector3d(0.5, 0.5, 0.5)), speed )

    }

    override fun wasExploded(level: Level, pos: BlockPos, explosion: Explosion) {
        Direction.entries.forEach {
            val state = level.getBlockState(pos.relative(it))
            if (state.block is DuctBlock) {
                val newState = state.setValue(DIR_TO_CONNECTION[it.opposite]!!, DuctConnectionType.LEAK)
                level.setBlockAndUpdate(pos.relative(it), newState)
            }
        }
    }

    override fun getInternalVolume(): Double {
        return 0.25
    }

    override fun getAdditionalInfoLines(): List<Component> {
        return listOf(
            Component.translatable("vs_clockwork.duct.function1").withStyle(ChatFormatting.GRAY).withStyle(
            ChatFormatting.ITALIC),

            Component.translatable("vs_clockwork.duct.function2").withStyle(ChatFormatting.GRAY).withStyle(
            ChatFormatting.ITALIC),
        )
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
