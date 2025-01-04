package org.valkyrienskies.clockwork.content.curiosities.sensor.distance

import com.simibubi.create.content.equipment.wrench.IWrenchable
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.sensor.ISensorBlock
import org.valkyrienskies.clockwork.content.curiosities.sensor.ISensorBlock.Companion.POWER
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.util.toDoubles
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.common.world.clipIncludeShips
import java.util.*
import kotlin.math.max
import kotlin.math.min

class DistanceSensorBlock(properties: Properties?): DirectionalBlock(properties), ISensorBlock, IWrenchable {

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

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue(
            DirectionalBlock.FACING,
            rotation.rotate(state.getValue(DirectionalBlock.FACING))
        ) as BlockState
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return state.rotate(mirror.getRotation(state.getValue(DirectionalBlock.FACING)))
    }

    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: Random) {
        val power = this.updatePower(state, level, pos, random)
        if (power != state.getValue(POWER)) {
            level.setBlock(pos, state.setValue(POWER, power) as BlockState, 2)
            level.updateNeighborsAt(pos, this)
        }
        level.scheduleTick(pos, this, 2)
        this.updateNeighborsInFront(level, pos, state)
    }

    override fun updatePower(state: BlockState, level: ServerLevel, pos: BlockPos, random: Random): Int {
        val adjacentState = level.getBlockState(pos.relative(state.getValue(FACING)))
        return if (!adjacentState.isAir && adjacentState.isViewBlocking(level, pos)) {
            0
        } else {
            val ship = level.getShipObjectManagingPos(pos)
            var refPos = Vec3.atCenterOf(pos).add(state.getValue(FACING).normal.toDoubles().multiply(0.5, 0.5, 0.5))
            val distance = (state.getValue(MAX_DISTANCE) * 16) - 1
            var targetPos = Vec3.atCenterOf(pos.relative(state.getValue(FACING), distance))
            if (ship != null) {
                refPos = ship.toWorldCoordinates(refPos)
                targetPos = ship.toWorldCoordinates(targetPos)
            }
            val clipContext = ClipContext(refPos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null)
            val castResult = level.clipIncludeShips(clipContext, true, ship?.id)
            if (castResult.type == HitResult.Type.MISS) {
                0
            } else {
                val dist = refPos.distanceTo(castResult.location)
                min((dist / state.getValue(MAX_DISTANCE).toDouble()).toInt(), 15)
            }
        }
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
                level.playSound(null, pos, ClockworkSounds.WELDER_WHIRR.mainEvent!!, player.soundSource, 1.0f, pitch)
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

    override fun getDirectSignal(state: BlockState, level: BlockGetter?, pos: BlockPos?, direction: Direction?): Int {
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