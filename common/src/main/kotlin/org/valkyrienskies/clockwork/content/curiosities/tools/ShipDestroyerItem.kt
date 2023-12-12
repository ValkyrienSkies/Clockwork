package org.valkyrienskies.clockwork.content.curiosities.tools

import com.google.common.collect.Sets
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import org.joml.AxisAngle4d
import org.joml.Matrix4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.platform.CWItem
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.networking.simple.sendToClient
import org.valkyrienskies.mod.common.*
import org.valkyrienskies.mod.common.networking.PacketRestartChunkUpdates
import org.valkyrienskies.mod.common.networking.PacketStopChunkUpdates
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.util.relocateBlock
import org.valkyrienskies.mod.util.updateBlock
import kotlin.math.*

class ShipDestroyerItem(properties: Properties) : CWItem(properties) {

    override fun useOn(context: UseOnContext): InteractionResult {
        context.player ?: return InteractionResult.FAIL
        val world = context.level
        if (world.isClientSide) {
            return InteractionResult.PASS
        }

        val blockPos = context.clickedPos

        val chunkX = blockPos.x shr 4
        val chunkZ = blockPos.z shr 4
        val ship: LoadedServerShip? = world.shipObjectWorld.loadedShips.getByChunkPos(chunkX, chunkZ, world.dimensionId) as LoadedServerShip?

        val invRotation = ship!!.transform.shipToWorldRotation.invert(Quaterniond())
        val invRotationAxisAngle = AxisAngle4d(invRotation)
        val alignTarget = ClockworkUtils.from2DDataValue(floor((invRotationAxisAngle.angle / (PI * 0.5)) + 4.5).toInt() % 4)

        unfillShip(
            world as ServerLevel,
            ship,
            alignTarget
        )

        return super.useOn(context)
    }


companion object {

    private fun roundToNearestMultipleOf(number: Double, multiple: Double) = multiple * round(number / multiple)


    private fun snapRotation(direction: AxisAngle4d): AxisAngle4d {
        val x = abs(direction.x)
        val y = abs(direction.y)
        val z = abs(direction.z)
        val angle = roundToNearestMultipleOf(direction.angle, PI / 2)

        return if (x > y && x > z) {
            direction.set(angle, direction.x.sign, 0.0, 0.0)
        } else if (y > x && y > z) {
            direction.set(angle, 0.0, direction.y.sign, 0.0)
        } else {
            direction.set(angle, 0.0, 0.0, direction.z.sign)
        }
    }

    fun unfillShip(level: ServerLevel, ship: ServerShip, direction: Direction) {
        ship.isStatic = true

        // ship's rotation rounded to nearest 90*
        val shipToWorld = ship.transform.run {
            Matrix4d()
                .translate(positionInWorld)
                .rotate(snapRotation(AxisAngle4d(shipToWorldRotation)))
                .scale(shipToWorldScaling)
                .translate(-positionInShip.x(), -positionInShip.y(), -positionInShip.z())
        }

        val alloc0 = Vector3d()

        // Direction comes from direction ship is aligning to
        // We can assume that the ship in shipspace is always facing north, because it has to be
        val rotation: Rotation = when (direction) {
            Direction.SOUTH -> Rotation.CLOCKWISE_180 // Bug in Direction.from2DDataValue() can return south/north as opposite
            Direction.NORTH -> Rotation.NONE
            Direction.EAST -> Rotation.COUNTERCLOCKWISE_90
            Direction.WEST -> Rotation.CLOCKWISE_90
            else -> {
                Rotation.NONE
            }
        }

        val chunksToBeUpdated = mutableMapOf<ChunkPos, Pair<ChunkPos, ChunkPos>>()

        ship.activeChunksSet.forEach { chunkX, chunkZ ->
            chunksToBeUpdated[ChunkPos(chunkX, chunkZ)] =
                Pair(ChunkPos(chunkX, chunkZ), ChunkPos(chunkX, chunkZ))
        }

        val chunkPairs = chunksToBeUpdated.values.toList()
        val chunkPoses = chunkPairs.flatMap { it.toList() }
        val chunkPosesJOML = chunkPoses.map { it.toJOML() }

        // Send a list of all the chunks that we plan on updating to players, so that they
        // defer all updates until assembly is finished
        level.players().forEach { player ->
            PacketStopChunkUpdates(chunkPosesJOML).sendToClient(player.playerWrapper)
        }

        val toUpdate = Sets.newHashSet<Triple<BlockPos, BlockPos, BlockState>>()

        ship.activeChunksSet.forEach { chunkX, chunkZ ->
            val chunk = level.getChunk(chunkX, chunkZ)
            for (section in chunk.sections) {
                if (section == null || section.hasOnlyAir()) continue
                for (x in 0..15) {
                    for (y in 0..15) {
                        for (z in 0..15) {
                            val state = section.getBlockState(x, y, z)
                            if (state.isAir) continue

                            val realX = (chunkX shl 4) + x
                            val realY = chunk.minBuildHeight + y
                            val realZ = (chunkZ shl 4) + z

                            val inWorldPos =
                                shipToWorld.transformPosition(alloc0.set(realX + 0.5, realY + 0.5, realZ + 0.5)).floor()

                            val inWorldBlockPos =
                                BlockPos(inWorldPos.x.toInt(), inWorldPos.y.toInt(), inWorldPos.z.toInt())
                            val inShipPos = BlockPos(realX, realY, realZ)

                            toUpdate.add(Triple(inShipPos, inWorldBlockPos, state))
                            level.relocateBlock(inShipPos, inWorldBlockPos, false, null, rotation)
                        }
                    }
                }
            }
        }
        // We update the blocks after they're set to prevent blocks from breaking
        for (triple in toUpdate) {
            updateBlock(level, triple.first, triple.second, triple.third)
        }

        level.server.executeIf(
            // This condition will return true if all modified chunks have been both loaded AND
            // chunk update packets were sent to players
            { chunkPoses.all(level::isTickingChunk) }
        ) {
            // Once all the chunk updates are sent to players, we can tell them to restart chunk updates
            level.players().forEach { player ->
                PacketRestartChunkUpdates(chunkPosesJOML).sendToClient(player.playerWrapper)
            }
        }
    }
}

}