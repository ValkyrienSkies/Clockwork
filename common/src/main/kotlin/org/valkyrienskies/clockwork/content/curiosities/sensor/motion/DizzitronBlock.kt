package org.valkyrienskies.clockwork.content.curiosities.sensor.motion

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.api.thinkable.ThinkableReading
import org.valkyrienskies.clockwork.content.curiosities.sensor.ISensorBlock
import org.valkyrienskies.clockwork.content.curiosities.sensor.ISensorBlock.Companion.POWER
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkableBlock
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.Random
import kotlin.math.atan2
import kotlin.math.hypot

class DizzitronBlock(properties: Properties) : DirectionalBlock(properties), ISensorBlock, IThinkableBlock {
    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWER, 0))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, POWER)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(FACING, context.nearestLookingDirection)

    override fun rotate(state: BlockState, rotation: Rotation): BlockState =
        state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

    override fun mirror(state: BlockState, mirror: Mirror): BlockState =
        state.rotate(mirror.getRotation(state.getValue(FACING)))

    override fun providesThinkable(state: BlockState, side: Direction) = side == state.getValue(FACING).opposite

    override fun thinkablePortName(state: BlockState, side: Direction): Component? =
        Component.translatable("thinkable.vs_clockwork.port.readings").takeIf { providesThinkable(state, side) }

    override fun thinkableReadings(state: BlockState, side: Direction) =
        if (providesThinkable(state, side)) READINGS else emptyList()

    override fun thinkableValue(state: BlockState, level: ServerLevel, pos: BlockPos, side: Direction): Double? =
        thinkableValue(state, level, pos, side, "speed")

    override fun thinkableValue(state: BlockState, level: ServerLevel, pos: BlockPos, side: Direction, reading: String): Double? {
        if (!providesThinkable(state, side)) return null
        val ship = level.getLoadedShipManagingPos(pos)
        if (ship == null && level.isBlockInShipyard(pos)) return null
        if (reading.isEmpty() || reading == "speed") return (ship?.velocity?.length() ?: 0.0).takeIf(Double::isFinite)

        val facing = state.getValue(FACING)
        val modelUp = when (facing) {
            Direction.UP -> Direction.SOUTH
            Direction.DOWN -> Direction.NORTH
            else -> Direction.UP
        }
        val rotation = ship?.transform?.shipToWorldRotation ?: Quaterniond()
        val forward = rotation.transform(facing.normal.toJOMLD())
        val up = rotation.transform(modelUp.normal.toJOMLD())
        val right = forward.cross(up, Vector3d())
        val velocity = ship?.velocity ?: Vector3d()
        val angularVelocity = ship?.angularVelocity ?: Vector3d()
        return when (reading) {
            "forward_velocity" -> velocity.dot(forward)
            "right_velocity" -> velocity.dot(right)
            "up_velocity" -> velocity.dot(up)
            "pitch_rate" -> Math.toDegrees(angularVelocity.dot(right))
            "yaw_rate" -> Math.toDegrees(-angularVelocity.dot(up))
            "roll_rate" -> Math.toDegrees(angularVelocity.dot(forward))
            "pitch" -> Math.toDegrees(atan2(forward.y, hypot(forward.x, forward.z)))
            "roll" -> if (hypot(right.y, up.y) > 1e-8) Math.toDegrees(atan2(-right.y, up.y)) else null
            else -> null
        }?.takeIf(Double::isFinite)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        if (!level.isClientSide) level.scheduleTick(pos, this, UPDATE_INTERVAL)
    }

    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        val power = speedPower(level, pos)
        if (power != state.getValue(POWER)) {
            level.setBlock(pos, state.setValue(POWER, power), UPDATE_CLIENTS)
            level.updateNeighborsAt(pos, this)
            updateRearNeighbors(state, level, pos)
        }
        level.scheduleTick(pos, this, UPDATE_INTERVAL)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        super.onRemove(state, level, pos, newState, isMoving)
        if (!level.isClientSide && state.getValue(POWER) != 0 && !newState.`is`(this)) updateRearNeighbors(state, level, pos)
    }

    private fun updateRearNeighbors(state: BlockState, level: Level, pos: BlockPos) {
        val facing = state.getValue(FACING)
        val rear = pos.relative(facing.opposite)
        level.updateNeighborsAtExceptFromFacing(rear, this, facing)
    }

    override fun isSignalSource(state: BlockState) = true

    override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int =
        if (direction == state.getValue(FACING)) state.getValue(POWER) else 0

    override fun getDirectSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction) =
        getSignal(state, level, pos, direction)

    override fun updatePower(state: BlockState, level: ServerLevel, pos: BlockPos, random: Random) = speedPower(level, pos)

    private fun speedPower(level: ServerLevel, pos: BlockPos): Int =
        level.getLoadedShipManagingPos(pos)?.velocity?.length()?.takeIf(Double::isFinite)?.toInt()?.coerceIn(0, 15) ?: 0

    companion object {
        private const val UPDATE_INTERVAL = 2
        private val READINGS = listOf("speed", "forward_velocity", "right_velocity", "up_velocity",
            "pitch_rate", "yaw_rate", "roll_rate", "pitch", "roll").map {
            ThinkableReading(it, Component.translatable("thinkable.vs_clockwork.reading.dizzitron.$it"))
        }
    }
}
