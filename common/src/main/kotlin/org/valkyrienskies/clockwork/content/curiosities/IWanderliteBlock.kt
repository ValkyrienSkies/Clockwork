package org.valkyrienskies.clockwork.content.curiosities

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.content.forces.WanderShipControl
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.config.MassDatapackResolver
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.common.util.transformPosition

interface IWanderliteBlock {

    fun addToShip(level: ServerLevel, ship: LoadedServerShip, pos: BlockPos) {
        val weight = MassDatapackResolver.getBlockStateMass(level.getBlockState(pos)) ?: return

        WanderShipControl.getOrCreate(ship).addBlock(pos, weight)
    }
    fun removeFromShip(ship: LoadedServerShip, pos: BlockPos) {
        WanderShipControl.getOrCreate(ship).removeBlock(pos)
    }
    fun collectBlockPositions(worldIn: Level, pos: BlockPos, depth: Int, collectedPositions: MutableList<BlockPos> = mutableListOf()): MutableList<BlockPos> {
        // Base case: If the depth is 0, return an empty collection
        if (depth == 0) {
            return mutableListOf()
        }

        // Add the current position to the set
        collectedPositions.add(pos)

        // Create a set to store block positions for the current iteration
        val positionsInThisIteration = mutableListOf(pos)

        // Iterate through each direction
        for (direction in Direction.entries) {
            // Get the neighboring block position in the current direction
            val neighborPos = pos.relative(direction)
            // Check if the block position is valid and not already collected
            if (worldIn.isInWorldBounds(neighborPos) && !collectedPositions.contains(neighborPos) && worldIn.getBlockState(neighborPos).block is IWanderliteBlock) {
                // Recursively collect block positions for the neighboring block
                positionsInThisIteration.addAll(collectBlockPositions(worldIn, neighborPos, depth - 1, collectedPositions))
            }
        }

        // Return the set of collected positions for this iteration
        return positionsInThisIteration
    }

    fun shipifyBlock(level: ServerLevel, blockPos: BlockPos)  {
        val blockList = collectBlockPositions(level, blockPos, 4)

        val notAllAir = blockList.any { !level.getBlockState(it).isAir }
        if (!notAllAir) {
            return
        }

        val ship = ShipAssembler.assembleToShip(level, blockList, true)

        for (pos in blockList) {
            // Our old world-space position is now BlockState{air}
            val shipBlockPos = BlockPos.containing(ship.transform.worldToShip.transformPosition(pos.center))

            val weight = MassDatapackResolver.getBlockStateMass(level.getBlockState(shipBlockPos)) ?: continue
            ClockworkUtils.wanderliteNodesToAdd[BlockPos.containing(ship.worldToShip.transformPosition(pos.toJOMLD()).toMinecraft())] = weight
            //addToShip(realConnectedShip, BlockPos(realConnectedShip.worldToShip.transformPosition(Vector3d(pos)).toMinecraft()), 2.0)
        }

    }

}
