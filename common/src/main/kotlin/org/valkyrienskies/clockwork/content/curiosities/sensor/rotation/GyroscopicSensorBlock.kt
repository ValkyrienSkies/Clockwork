package org.valkyrienskies.clockwork.content.curiosities.sensor.rotation

import com.simibubi.create.content.equipment.wrench.IWrenchable
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.*
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.phys.BlockHitResult
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.sensor.ISensorBlock
import org.valkyrienskies.clockwork.content.curiosities.sensor.ISensorBlock.Companion.POWER
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.*
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.acos
import kotlin.math.min
import kotlin.math.roundToInt

class GyroscopicSensorBlock(properties: Properties) : DirectionalBlock(properties), ISensorBlock {

    companion object {
        val REFERENCE_AXIS = EnumProperty.create("reference_axis", Axis::class.java)
        val NEGATIVE = BooleanProperty.create("negative")

        fun getOutputDirections(facing: Direction): Pair<Direction, Direction> {
            val targetAxis = when (facing) {
                UP, DOWN, NORTH, SOUTH -> Axis.X
                else -> Axis.Z
            }
            val positive = get(AxisDirection.POSITIVE, targetAxis)
            val negative = positive.opposite
            return Pair(positive, negative)
        }
    }

    init {
        this.registerDefaultState(
            ((this.stateDefinition.any() as BlockState).setValue<Direction, Direction>(
                FACING, SOUTH
            ) as BlockState).setValue(POWER, 0).setValue(REFERENCE_AXIS, SOUTH.clockWise.axis).setValue(
                NEGATIVE, false) as BlockState
        )
    }

    fun directionToLodefocus(state: BlockState, level: ServerLevel, pos: BlockPos): Vector3d {

        val lodefocusPos = pos.relative(state.getValue(FACING))

        val lodefocusBlockEntity = level.getBlockEntity(lodefocusPos) as? LodefocusBlockEntity ?: return state.getValue(
            FACING
        ).normal.toJOMLD()

        val targetPos = lodefocusBlockEntity.getWorldspaceTargetPosition()?.toJOMLD()
            ?: return state.getValue(FACING).normal.toJOMLD()

        val worldPos = level.toWorldCoordinates(pos.toJOMLD())

        return targetPos.sub(worldPos, Vector3d()).normalize()
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 2)
        }
        super.onPlace(state, level, pos, oldState, isMoving)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING, POWER, REFERENCE_AXIS, NEGATIVE)
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue(
            FACING,
            rotation.rotate(state.getValue(FACING))
        ).setValue(REFERENCE_AXIS, state.getValue(FACING).clockWise.axis) as BlockState
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return state.rotate(mirror.getRotation(state.getValue(FACING)))
    }

    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        val power = this.updatePower(state, level, pos, Random())
        val negative = power < 0
        if (power.absoluteValue != state.getValue(POWER) || negative != state.getValue(NEGATIVE)) {
            var newState = state.setValue(POWER, power.absoluteValue) as BlockState
            if (negative != state.getValue(NEGATIVE)) {
                newState = newState.setValue(NEGATIVE, negative) as BlockState
            }
            level.setBlock(pos, newState, 2)
            level.updateNeighborsAt(pos, this)
        }
        level.scheduleTick(pos, this, 2)
        this.updateNeighborsInFront(level, pos, state)
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        currentPos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (state.getValue(FACING) == direction && state.getValue(POWER) != 0) {
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
        val direction = state.getValue(FACING)
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
        if (state.getValue(POWER) != 0 && getOutputDirections(state.getValue(FACING)).first.axis == direction.axis) {
            if (state.getValue(NEGATIVE)) {
                if (direction == getOutputDirections(state.getValue(FACING)).second) {
                    return state.getValue(POWER)
                }
            } else {
                if (direction == getOutputDirections(state.getValue(FACING)).first) {
                    return state.getValue(POWER)
                }
            }
        }
        return 0
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return this.defaultBlockState()
            .setValue<Direction, Direction>(FACING, context.nearestLookingDirection.opposite.opposite)
    }

    override fun updatePower(state: BlockState, level: ServerLevel, pos: BlockPos, random: Random): Int {
        val hasLodefocus = level.getBlockEntity(pos.relative(state.getValue(FACING))) is LodefocusBlockEntity

        val originalDirection = state.getValue(FACING).normal.toJOMLD()
        val targetDirection = if (!hasLodefocus) originalDirection else directionToLodefocus(state, level, pos)

        val ship = level.getShipObjectManagingPos(pos) ?: return 0

        val referenceAxis = state.getValue(REFERENCE_AXIS)
        val referenceDir = ship.shipToWorld.transformDirection(get(AxisDirection.POSITIVE, referenceAxis).normal.toJOMLD())

        val transformedDirection = ship.shipToWorld.transformDirection(originalDirection, Vector3d())
        if(!transformedDirection.isFinite) return 0

        val difference = targetDirection.axialDistanceTo(transformedDirection, referenceDir)

        if(difference.isNaN()) return 0
        if((difference * 15.0).isNaN()) return 0

        return Mth.clamp((difference * 15.0).roundToInt(), -15, 15)
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
                val currentAxis = state.getValue(REFERENCE_AXIS)
                val nextAxis = when (currentAxis) {
                    Axis.X -> Axis.Y
                    Axis.Y -> Axis.Z
                    Axis.Z -> Axis.X
                    else -> Axis.X
                }
                level.setBlock(pos, state.setValue(REFERENCE_AXIS, nextAxis) as BlockState, 2)
                level.playSound(null, pos, ClockworkSounds.WELDER_WHIRR.mainEvent!!, player.soundSource, 1.0f, 1.0f)
                return InteractionResult.SUCCESS
            }
        }
        return super.use(state, level, pos, player, hand, hit)
    }

    fun Vector3d.axialDistanceTo(rotated: Vector3d, axis: Vector3d): Double {
        val axisNorm = axis.normalize(Vector3d())

        val originalProj = this.projectOntoPlane(axisNorm)
        val rotatedProj = rotated.projectOntoPlane(axisNorm)

        originalProj.normalize()
        rotatedProj.normalize()

        val dot = originalProj.dot(rotatedProj)

        val cross = originalProj.cross(rotatedProj, Vector3d())
        val sign = cross.dot(axisNorm)

        val mul = if (sign > 0.0) 1.0 else if (sign < 0.0) -1.0 else 0.0

        if(abs(dot) > 1.0) return 0.0
        return (acos(dot) / Math.PI) * mul
    }

    fun Vector3d.projectOntoPlane(axis: Vector3d): Vector3d {
        val dotProduct = this.dot(axis)
        return this.sub(Vector3d(axis).mul(dotProduct), Vector3d())
    }
}