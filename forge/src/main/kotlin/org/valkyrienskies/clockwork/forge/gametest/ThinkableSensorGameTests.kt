package org.valkyrienskies.clockwork.forge.gametest

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING
import net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE
import net.minecraft.world.level.block.state.properties.SlabType
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.sensor.impact.ImpactSensorBlock
import org.valkyrienskies.clockwork.content.curiosities.sensor.rotation.GyroscopicSensorBlock
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableItem
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableRelayBlockEntity
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore
import java.util.Random
import kotlin.math.abs

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class ThinkableSensorGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private const val BATCH = "thinkable_sensors"
        private val ships = mutableListOf<ServerShip>()
        private val chunks = mutableSetOf<ChunkPos>()

        @JvmStatic @AfterBatch(batch = BATCH)
        fun cleanup(level: ServerLevel) {
            ships.forEach { ShipAssembler.deleteShip(level, it, true, false) }
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }
            ships.clear()
            chunks.clear()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH)
        fun peepotronPreservesFractionsThroughCableAndAdjacency(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            val state = ClockworkBlocks.DISTANCE_SENSOR.defaultState.setValue(FACING, Direction.UP)
            level.setBlockAndUpdate(pos, state)
            level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState())
            level.setBlockAndUpdate(pos.above(2), Blocks.AIR.defaultBlockState())
            level.setBlockAndUpdate(pos.above(3), Blocks.OAK_SLAB.defaultBlockState().setValue(SLAB_TYPE, SlabType.TOP))
            val adjacent = relay(level, pos.below(), Direction.UP)
            val cable = relay(level, pos.east(2), Direction.UP)
            val source = WandAnchor.at(level, pos, Direction.DOWN)
            val sink = WandAnchor.at(level, cable.blockPos, Direction.UP)
            val device = ThinkableConnections.device(level, source)!!
            helper.assertTrue(level.getBlockEntity(pos) == null, "Peepotron gained a block entity")
            helper.assertTrue(device.thinkableValue(Direction.DOWN) == 2.5, "Distance lost its fractional part")
            helper.assertTrue(device.thinkableValue(Direction.UP) == null, "Sensing face became an output")
            helper.assertTrue(ClockworkBlocks.DISTANCE_SENSOR.get().updatePower(state, level, pos, Random()) == 2, "Redstone was not quantized")
            helper.assertTrue(adjacent.thinkableValue(Direction.EAST) == 2.5, "Adjacent relay lost precise block output")
            helper.assertTrue(ThinkableConnections.connect(level, source, sink), "Block-only source could not connect")
            helper.assertTrue(cable.thinkableValue(Direction.EAST) == 2.5, "Cable lost precise block output")
            level.setBlockAndUpdate(pos, state.setValue(FACING, Direction.NORTH))
            helper.assertTrue(!device.providesThinkable(Direction.DOWN) && cable.thinkableValue(Direction.EAST) == null,
                "Rotated block kept a stale output face")
            level.removeBlock(pos, false)
            helper.assertTrue(device.thinkableValue(Direction.SOUTH) == null && ThinkableItem.source(level, source) == null,
                "Removed sensor kept supplying data")
            helper.succeed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH)
        fun peepotronReportsNoHitAndMeasuresThroughLenses(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            val state = ClockworkBlocks.DISTANCE_SENSOR.defaultState.setValue(FACING, Direction.UP)
            level.setBlockAndUpdate(pos, state)
            for (dy in 1..16) level.setBlockAndUpdate(pos.above(dy), Blocks.AIR.defaultBlockState())
            val source = WandAnchor.at(level, pos, Direction.DOWN)
            helper.assertTrue(ThinkableItem.source(level, source) == -1.0, "No hit was not distinguishable from a near hit")
            helper.assertTrue(ClockworkBlocks.DISTANCE_SENSOR.get().updatePower(state, level, pos, Random()) == 15, "No-hit redstone changed")
            level.setBlockAndUpdate(pos.above(2), Blocks.GLASS.defaultBlockState())
            level.setBlockAndUpdate(pos.above(4), Blocks.OAK_SLAB.defaultBlockState().setValue(SLAB_TYPE, SlabType.TOP))
            helper.assertTrue(ThinkableItem.source(level, source) == 3.5, "Lens reset the measured distance")
            level.setBlockAndUpdate(pos.above(), Blocks.BLUE_STAINED_GLASS.defaultBlockState())
            level.setBlockAndUpdate(pos.above(2), Blocks.AIR.defaultBlockState())
            level.setBlockAndUpdate(pos.above(3), Blocks.WATER.defaultBlockState())
            val water = ThinkableItem.source(level, source)
            helper.assertTrue(water != null && abs(water - 1.0) < 1e-5, "Blue lens did not detect water: $water")
            level.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState())
            helper.assertTrue(ThinkableItem.source(level, source) == 0.0, "Obstructed sensor did not report zero distance")
            helper.succeed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
        fun spinotronReportsSignedDegreesOnBothPortsWithoutBlockEntity(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 4, 3))
            level.setBlockAndUpdate(pos, ClockworkBlocks.GYROSCOPIC_SENSOR.defaultState
                .setValue(FACING, Direction.SOUTH).setValue(GyroscopicSensorBlock.REFERENCE_AXIS, Direction.Axis.X))
            val ship = assemble(level, pos)
            val sensor = ship.shipAABB!!.let { BlockPos(it.minX(), it.minY(), it.minZ()) }
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.getShipObjectManagingPos(sensor) != null, "Sensor ship is not loaded")
            }.thenExecute {
                helper.assertTrue(level.getBlockEntity(sensor) == null, "Spinotron gained a block entity")
                for (degrees in listOf(17.25, -23.75, 180.0)) {
                    val halfAngle = Math.toRadians(degrees) / 2.0
                    level.shipObjectWorld.teleportShip(ship, vsCore.newShipTeleportData(Vector3d(ship.transform.positionInWorld),
                        Quaterniond(kotlin.math.sin(halfAngle), 0.0, 0.0, kotlin.math.cos(halfAngle)), Vector3d(), Vector3d()))
                    for (side in listOf(Direction.EAST, Direction.WEST)) {
                        val value = ThinkableItem.source(level, WandAnchor.at(level, sensor, side))
                        helper.assertTrue(value != null && abs(value - degrees) < 1e-5,
                            "Signed degrees lost precision: expected=$degrees, actual=$value, rotation=${ship.transform.rotation}")
                    }
                }
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH)
        fun impactotronExposesPredictionScoreWithoutBlockEntity(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 3, 3))
            val state = ClockworkBlocks.IMPACT_SENSOR.defaultState.setValue(FACING, Direction.UP)
                .setValue(ImpactSensorBlock.PREDICTIVENESS, 2)
            level.setBlockAndUpdate(pos, state)
            for (dx in -1..2) for (dy in 0..3) for (dz in -1..2) {
                val candidate = pos.offset(dx, dy, dz)
                if (candidate != pos) level.setBlockAndUpdate(candidate, Blocks.AIR.defaultBlockState())
            }
            val source = WandAnchor.at(level, pos, Direction.DOWN)
            helper.assertTrue(level.getBlockEntity(pos) == null, "Impactotron gained a block entity")
            helper.assertTrue(ThinkableItem.source(level, source) == 0.0,
                "Empty prediction area reported an obstacle: ${ThinkableItem.source(level, source)}")
            level.setBlockAndUpdate(pos.above(2), Blocks.STONE.defaultBlockState())
            helper.assertTrue(ThinkableItem.source(level, source) == 14.0, "Prediction setting was ignored")
            level.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState())
            helper.assertTrue(ThinkableItem.source(level, source) == 15.0, "Immediate obstruction did not reach maximum")
            helper.succeed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
        fun impactotronKeepsFractionalScoreOnScaledShip(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 5, 3))
            level.setBlockAndUpdate(pos, ClockworkBlocks.IMPACT_SENSOR.defaultState.setValue(FACING, Direction.UP)
                .setValue(ImpactSensorBlock.PREDICTIVENESS, 2))
            val ship = assemble(level, pos, 1.25)
            val sensor = ship.shipAABB!!.let { BlockPos(it.minX(), it.minY(), it.minZ()) }
            helper.runAtTickTime(10) {
                val world = ship.shipToWorld.transformPosition(Vector3d(sensor.x + 0.5, sensor.y + 0.5, sensor.z + 0.5))
                level.setBlockAndUpdate(BlockPos.containing(world.x, world.y, world.z).above(2), Blocks.STONE.defaultBlockState())
                val value = ThinkableItem.source(level, WandAnchor.at(level, sensor, Direction.DOWN))
                helper.assertTrue(value != null && abs(value - 13.5) < 1e-5, "Impact score lost fractional precision: $value")
                helper.assertTrue(ClockworkBlocks.IMPACT_SENSOR.get().updatePower(level.getBlockState(sensor), level, sensor, Random()) == 13,
                    "Impact redstone was not quantized")
                helper.succeed()
            }
        }

        private fun relay(level: ServerLevel, pos: BlockPos, input: Direction): ThinkableRelayBlockEntity {
            level.setBlockAndUpdate(pos, ClockworkBlocks.THINKABLE_RELAY.defaultState.setValue(FACING, input))
            return level.getBlockEntity(pos) as ThinkableRelayBlockEntity
        }

        private fun assemble(level: ServerLevel, pos: BlockPos, scale: Double = 1.0): ServerShip =
            ShipAssembler.assembleToShip(level, setOf(pos.mutable()), scale).also {
                it.isStatic = true
                ships.add(it)
                val bounds = it.shipAABB!!
                for (x in (bounds.minX() shr 4)..(bounds.maxX() shr 4)) for (z in (bounds.minZ() shr 4)..(bounds.maxZ() shr 4))
                    if (level.setChunkForced(x, z, true)) chunks.add(ChunkPos(x, z))
            }
    }
}
