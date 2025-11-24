package org.valkyrienskies.clockwork.content.curiosities.sensor.impact

import com.simibubi.create.content.equipment.wrench.IWrenchable
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.primitives.AABBd
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.ClockworkTags
import org.valkyrienskies.clockwork.content.curiosities.sensor.ISensorBlock
import org.valkyrienskies.clockwork.content.curiosities.sensor.ISensorBlock.Companion.POWER
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.transformToNearbyShipsAndWorld
import org.valkyrienskies.mod.common.util.toDoubles
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.common.world.clipIncludeShips
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

//todo use collision events
class ImpactSensorBlock(properties: Properties?): DirectionalBlock(properties), ISensorBlock, IWrenchable {

    companion object {
        val PREDICTIVENESS = IntegerProperty.create("predictiveness", 0, 3)
    }

    init {
        this.registerDefaultState(
            ((this.stateDefinition.any() as BlockState).setValue<Direction, Direction>(
                DirectionalBlock.FACING, Direction.SOUTH
            ) as BlockState).setValue(POWER, 0).setValue(PREDICTIVENESS, 1) as BlockState
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(DirectionalBlock.FACING, POWER, PREDICTIVENESS)
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

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 2)
        }
        super.onPlace(state, level, pos, oldState, isMoving)
    }

    override fun updatePower(state: BlockState, level: ServerLevel, pos: BlockPos, random: Random): Int {
        val adjacentState = level.getBlockState(pos.relative(state.getValue(FACING)))
        val ship = level.getShipObjectManagingPos(pos)

        return if (!adjacentState.isAir && adjacentState.isViewBlocking(level, pos)) {
            15
        } else {
            val targetPositions = mutableSetOf<Vec3>()
            targetPositions.add(level.toWorldCoordinates(Vec3.atCenterOf(pos.relative(state.getValue(DirectionalBlock.FACING)))))
            val predictiveness = state.getValue(PREDICTIVENESS)
            if (ship != null) {
                val predictPos = Vec3.atCenterOf(pos).toJOML().sub(ship.transform.positionInShip)
                if (predictiveness > 0) {
                    for (prediction in 1..predictiveness) {
                        val shipVel = ship.velocity
                        val shipAngVel = ship.angularVelocity

                        val shipPos = ship.transform.positionInWorld
                        val shipRot = ship.transform.rotation

                        val predictivePos = shipPos.add(
                            shipVel, Vector3d()
                        )
                        val deltaRot = Quaterniond().fromAxisAngleRad(shipAngVel.normalize(Vector3d()), shipAngVel.length() * (prediction.toDouble()/20.0))
                        val predictiveRot = shipRot.mul(deltaRot, Quaterniond())

                        val rotatedPos = predictPos.rotate(predictiveRot, Vector3d())

                        val targetPos = level.toWorldCoordinates(predictivePos.add(rotatedPos, Vector3d()).toMinecraft())
                        if (!targetPos.length().isNaN() && targetPos.length().isFinite()) targetPositions.add(targetPos)
                    }
                }
            }
            var found = 0
            val positions = hashSetOf<Vector3dc>()
            positions.add(targetPositions.first().toJOML())

            targetPositions.forEach { positions.addAll(level.transformToNearbyShipsAndWorld(it.x, it.y, it.z, 1.5)) }

            for (position in positions) {
                val posShip = level.getShipObjectManagingPos(BlockPos.containing(position.toMinecraft()))
                if (posShip != null && ship != null && posShip.id == ship.id) {
                    continue
                }
                if (!level.getBlockState(BlockPos.containing(position.toMinecraft())).isAir) {
                    found = max(
                        min(max(16 - (level.toWorldCoordinates(position.toMinecraft()).distanceTo(level.toWorldCoordinates(Vec3.atCenterOf(pos))).absoluteValue * predictiveness.toDouble()).toInt(), 0), 15),
                        found
                    )
                }
                val searchAABB = AABB(BlockPos.containing(position.toMinecraft())).inflate(1.0)
                level.getBlockStates(searchAABB).forEach { state ->
                    if (!state.isAir) {
                        found = max(
                            min(max(16 - (level.toWorldCoordinates(position.toMinecraft()).distanceTo(level.toWorldCoordinates(Vec3.atCenterOf(pos))).absoluteValue * predictiveness.toDouble()).toInt(), 0), 15),
                            found
                        )
                        return@forEach
                    }
                }
                if (found == 15) {
                    break
                }
            }

            found
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
                val currentMax = state.getValue(PREDICTIVENESS)
                val nextMax = if (currentMax == 3) 0 else currentMax + 1
                level.setBlock(pos, state.setValue(PREDICTIVENESS, nextMax) as BlockState, 2)
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
