package org.valkyrienskies.clockwork.content.curiosities.sensor.distance

import com.simibubi.create.content.equipment.wrench.IWrenchable
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.ClockworkTags
import org.valkyrienskies.clockwork.content.curiosities.sensor.ISensorBlock
import org.valkyrienskies.clockwork.content.curiosities.sensor.ISensorBlock.Companion.POWER
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkableBlock
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.world.clipIncludeShips
import java.util.*
import kotlin.math.max

class DistanceSensorBlock(properties: Properties?): DirectionalBlock(properties), ISensorBlock, IThinkableBlock {

    override fun providesThinkable(state: BlockState, side: Direction) = side == state.getValue(FACING).opposite

    override fun thinkableValue(state: BlockState, level: ServerLevel, pos: BlockPos, side: Direction): Double =
        distance(state, level, pos) ?: -1.0

    override fun thinkablePortName(state: BlockState, side: Direction): Component? =
        Component.translatable("thinkable.vs_clockwork.port.distance").takeIf { providesThinkable(state, side) }

    companion object {
        val MAX_DISTANCE = IntegerProperty.create("max_distance", 1, 4)
    }

    init {
        this.registerDefaultState(
            ((this.stateDefinition.any() as BlockState).setValue<Direction, Direction>(
                DirectionalBlock.FACING, Direction.SOUTH
            ) as BlockState).setValue(POWER, 0).setValue(MAX_DISTANCE, 1) as BlockState
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(DirectionalBlock.FACING, POWER, MAX_DISTANCE)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 2)
        }
        super.onPlace(state, level, pos, oldState, isMoving)
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue(
            DirectionalBlock.FACING,
            rotation.rotate(state.getValue(DirectionalBlock.FACING))
        ) as BlockState
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return state.rotate(mirror.getRotation(state.getValue(DirectionalBlock.FACING)))
    }

    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        val power = this.updatePower(state, level, pos, Random())
        if (power != state.getValue(POWER)) {
            level.setBlock(pos, state.setValue(POWER, power) as BlockState, 2)
            level.updateNeighborsAt(pos, this)
        }
        level.scheduleTick(pos, this, 2)
        this.updateNeighborsInFront(level, pos, state)
    }

    override fun updatePower(state: BlockState, level: ServerLevel, pos: BlockPos, random: Random): Int {
        val distance = distance(state, level, pos) ?: return 15
        return (distance / state.getValue(MAX_DISTANCE)).toInt().coerceIn(0, 15)
    }

    fun distance(state: BlockState, level: ServerLevel, pos: BlockPos): Double? {
        val adjacentState = level.getBlockState(pos.relative(state.getValue(FACING)))
        val hasAdjacentLens = adjacentState.`is`(ClockworkTags.AllBlockTags.SENSOR_LENS.tag)
        var spotWater = adjacentState.`is`(Blocks.BLUE_STAINED_GLASS)
        var spotAllFluids = adjacentState.`is`(Blocks.RED_STAINED_GLASS)
        if (!hasAdjacentLens && !adjacentState.isAir && adjacentState.isViewBlocking(level, pos)) return 0.0

        val ship = level.getShipObjectManagingPos(pos)
        val direction = Vec3.atLowerCornerOf(state.getValue(FACING).normal)
        val lensOffset = if (hasAdjacentLens) 1.0 else 0.0
        var origin = Vec3.atCenterOf(pos).add(direction.scale(0.5 + lensOffset))
        var end = Vec3.atCenterOf(pos).add(direction.scale(state.getValue(MAX_DISTANCE) * 16 - 1.0 + lensOffset))
        if (ship != null) {
            origin = ship.toWorldCoordinates(origin)
            end = ship.toWorldCoordinates(end)
        }
        val ray = end.subtract(origin).normalize()
        val range = origin.distanceTo(end)
        var start = origin
        repeat(128) {
            val fluidContext = if (spotAllFluids) ClipContext.Fluid.ANY else if (spotWater) ClipContext.Fluid.WATER else ClipContext.Fluid.NONE
            val hit = level.clipIncludeShips(ClipContext(start, end, ClipContext.Block.COLLIDER, fluidContext, null), true, ship?.id)
            if (hit.type == HitResult.Type.MISS) return null
            val hitState = level.getBlockState(hit.blockPos)
            if (!hitState.`is`(ClockworkTags.AllBlockTags.SENSOR_LENS.tag)) return origin.distanceTo(hit.location)
            if (hitState.`is`(Blocks.BLUE_STAINED_GLASS)) spotWater = true
            if (hitState.`is`(Blocks.RED_STAINED_GLASS)) spotAllFluids = true
            val travelled = max(start.distanceTo(origin) + 0.5, origin.distanceTo(hit.location) + 0.5)
            if (travelled >= range) return null
            start = origin.add(ray.scale(travelled))
        }
        return null
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            if (player.getItemInHand(hand).`is`(ClockworkItems.SCREWDRIVER.get())) {
                val currentMax = state.getValue(MAX_DISTANCE)
                val nextMax = if (currentMax == 4) 1 else currentMax + 1
                level.setBlock(pos, state.setValue(MAX_DISTANCE, nextMax) as BlockState, 2)
                val pitch = level.random.nextFloat() * 0.2f + 0.9f
                level.playSound(null, pos, ClockworkSounds.WAND_WELD.mainEvent!!, player.soundSource, 1.0f, pitch)
                level.scheduleTick(pos, this, 2)
                return InteractionResult.SUCCESS
            }
        }
        return super.use(state, level, pos, player, hand, hit)
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        currentPos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (state.getValue(DirectionalBlock.FACING) == direction && state.getValue(POWER) != 0) {
            this.startSignal(level, currentPos)
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos)
    }

    private fun startSignal(level: LevelAccessor, pos: BlockPos) {
        if (!level.isClientSide && !level.blockTicks.hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 2)
        }
    }

    protected fun updateNeighborsInFront(level: Level, pos: BlockPos, state: BlockState) {
        val direction = state.getValue(DirectionalBlock.FACING)
        val blockPos = pos.relative(direction.opposite)
        level.neighborChanged(blockPos, this, pos)
        level.updateNeighborsAtExceptFromFacing(blockPos, this, direction)
    }

    override fun isSignalSource(state: BlockState): Boolean {
        return true
    }

    override fun getDirectSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        return state.getSignal(level, pos, direction)
    }

    override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        if (state.getValue(POWER) != 0 && state.getValue(DirectionalBlock.FACING) == direction) {
            return state.getValue(POWER)
        }
        return 0
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return this.defaultBlockState()
            .setValue<Direction, Direction>(DirectionalBlock.FACING, context.nearestLookingDirection.opposite.opposite)
    }
}
