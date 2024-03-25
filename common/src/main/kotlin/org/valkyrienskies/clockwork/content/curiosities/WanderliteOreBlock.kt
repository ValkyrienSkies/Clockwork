package org.valkyrienskies.clockwork.content.curiosities

import com.simibubi.create.content.materials.ExperienceBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.joml.Vector3i
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.forces.WanderShipControl
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.*
import org.valkyrienskies.mod.common.assembly.createNewShipWithBlocks
import org.valkyrienskies.mod.common.util.toJOML

class WanderliteOreBlock(properties: Properties) : ExperienceBlock(properties) {

    override fun use(state: BlockState,
                     level: Level,
                     pos: BlockPos,
                     player: Player,
                     hand: InteractionHand,
                     hit: BlockHitResult): InteractionResult {

        if (level is ServerLevel && !isAlreadyShip(level, pos)) {
            shipifyBlock(level, pos)
            return InteractionResult.SUCCESS
        }

        return InteractionResult.FAIL;
    }

    override fun attack(state: BlockState, level: Level, pos: BlockPos, player: Player) {
        if (level is ServerLevel && !isAlreadyShip(level, pos)) {
            shipifyBlock(level, pos)
        }
        super.attack(state, level, pos, player)
    }

    override fun stepOn(level: Level, pos: BlockPos, state: BlockState, entity: Entity) {
        if (entity is Player && !entity.isSteppingCarefully && level is ServerLevel && !isAlreadyShip(level, pos)) {
            shipifyBlock(level, pos)
        }

        super.stepOn(level, pos, state, entity)
    }

    fun isAlreadyShip(level: ServerLevel, blockPos: BlockPos): Boolean {
        return level.isBlockInShipyard(blockPos.x, blockPos.y, blockPos.z)
    }

    fun shipifyBlock(level: ServerLevel, blockPos: BlockPos)  {
        val dense = DenseBlockPosSet()
        //dense.add(blockPos.x, blockPos.y, blockPos.z)
        var list: MutableSet<Vector3i> = mutableSetOf()
        collectBlockPositions(level, blockPos, 4, list)
        dense.addAll(list)

        val connectedShip = createNewShipWithBlocks(blockPos, dense, level)
        WanderShipControl.getOrCreate(connectedShip)!!.aurics += dense.size
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        if (level is ServerLevel) {
            val ship = level.getShipManagingPos(pos)
            if (ship != null) {
                WanderShipControl.getOrCreate(ship)!!.aurics ++
            }
        }
        super.onPlace(state, level, pos, oldState, movedByPiston)
    }



    override fun destroy(level: LevelAccessor, pos: BlockPos, state: BlockState) {
        if (level is ServerLevel) {
            val ship = level.getShipManagingPos(pos)
            if (ship != null) {
                WanderShipControl.getOrCreate(ship)!!.aurics --
            }
        }
        super.destroy(level, pos, state)
    }

    private fun collectBlockPositions(worldIn: Level, pos: BlockPos, depth: Int, collectedPositions: MutableSet<Vector3i>): Collection<Vector3i> {
        // Base case: If the depth is 0, return an empty collection
        if (depth == 0) {
            return emptySet()
        }

        // Add the current position to the set
        collectedPositions.add(pos.toJOML())

        // Create a set to store block positions for the current iteration
        val positionsInThisIteration = mutableSetOf<Vector3i>()

        // Iterate through each direction
        for (direction in Direction.values()) {
            // Get the neighboring block position in the current direction
            val neighborPos = pos.relative(direction)

            // Check if the block position is valid and not already collected
            if (worldIn.isInWorldBounds(neighborPos) && !collectedPositions.contains(neighborPos.toJOML()) && worldIn.getBlockState(neighborPos).block is WanderliteOreBlock) {
                // Recursively collect block positions for the neighboring block
                positionsInThisIteration.addAll(collectBlockPositions(worldIn, neighborPos, depth - 1, collectedPositions))
            }
        }

        // Return the set of collected positions for this iteration
        return positionsInThisIteration
    }
}