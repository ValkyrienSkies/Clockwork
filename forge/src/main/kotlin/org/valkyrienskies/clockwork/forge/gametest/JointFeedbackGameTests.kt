package org.valkyrienskies.clockwork.forge.gametest

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.BeforeBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.contraptions.phys.gimbal.GimbalBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.gimbal.GimbalMode
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeAssembly
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeLinks
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonBlockEntity
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore
import kotlin.math.abs

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class JointFeedbackGameTests {
    companion object {
        private const val BATCH = "joint_feedback"
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private val ships = mutableSetOf<ServerShip>()
        private val blocks = mutableSetOf<BlockPos>()
        private val chunks = mutableSetOf<ChunkPos>()
        private val observers = mutableListOf<ShipTestObserver>()
        private var previousSync = false

        @JvmStatic @BeforeBatch(batch = BATCH)
        fun prepare(level: ServerLevel) {
            previousSync = VSCoreConfig.SERVER.pt.synchronizePhysics
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
        }

        @JvmStatic @AfterBatch(batch = BATCH)
        fun cleanup(level: ServerLevel) {
            blocks.forEach { level.removeBlock(it, false) }
            ships.forEach { ShipAssembler.deleteShip(level, it, true, false) }
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }
            observers.forEach(ShipTestObserver::close)
            blocks.clear(); ships.clear(); chunks.clear(); observers.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousSync
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 120)
        fun extendonMeasuresTheMovingAnchors(helper: GameTestHelper) {
            val level = helper.level
            val base = fixture(helper)
            val state = ClockworkBlocks.EXTENDON.defaultState
            level.setBlockAndUpdate(base, state.setValue(FACING, Direction.UP))
            level.setBlockAndUpdate(base.above(8), state.setValue(FACING, Direction.DOWN))
            val child = ShipAssembler.assembleToShip(level, setOf(base.above(8)), 1.0)
            child.isStatic = true
            val end = remember(level, child)
            val extendon = level.getBlockEntity(base) as ExtendonBlockEntity
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(child.id) != null, "Extendon payload not loaded")
            }.thenExecute {
                helper.assertTrue(extendon.tryConnect(level, end), "Extendon failed to connect")
                expect(helper, base, Direction.NORTH, "length", 8.0)
                expect(helper, end, Direction.SOUTH, "target_length", 1.5)
                level.shipObjectWorld.teleportShip(child,
                    vsCore.newShipTeleportData(newPos = Vector3d(child.transform.positionInWorld).add(0.0, 2.75, 0.0)))
            }.thenWaitUntil {
                expect(helper, base, Direction.NORTH, "length", 10.75)
                expect(helper, end, Direction.SOUTH, "length_error", -9.25)
            }.thenExecute {
                extendon.disconnect()
                helper.assertTrue(read(helper, base, Direction.NORTH, "length") == null, "Disconnected Extendon reported a length")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 120)
        fun hingeReportsSignedAngleAtBothEnds(helper: GameTestHelper) {
            val level = helper.level
            val base = fixture(helper)
            level.setBlockAndUpdate(base, ClockworkBlocks.HINGE.defaultState.setValue(FACING, Direction.UP))
            helper.assertTrue(HingeAssembly.assemble(level, base) == null, "Hinge failed to assemble")
            val link = HingeLinks.get(level).at(base)!!
            val child = level.shipObjectWorld.allShips.getById(link.b.shipId)!!
            child.isStatic = true
            remember(level, child)
            force(level, link.b.pos)
            helper.startSequence().thenWaitUntil {
                expect(helper, base, Direction.NORTH, "angle", 0.0)
            }.thenExecute {
                val axis = HingeLinks.worldRotation(level, link.a).transform(Vector3d(1.0, 0.0, 0.0))
                val pivot = HingeLinks.worldPoint(level, link.a)!!
                val delta = Quaterniond().rotationAxis(Math.toRadians(-27.5), axis.x, axis.y, axis.z)
                rotate(level, child, delta, Vector3d(pivot.x, pivot.y, pivot.z))
            }.thenWaitUntil {
                expect(helper, base, Direction.NORTH, "angle", -27.5)
                expect(helper, link.b.pos, Direction.SOUTH, "angle", -27.5)
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 120)
        fun unpoweredGimbalMeasuresBothTiltDirections(helper: GameTestHelper) {
            val level = helper.level
            val base = fixture(helper)
            level.setBlockAndUpdate(base, ClockworkBlocks.GIMBAL_BEARING.defaultState.setValue(FACING, Direction.UP))
            level.setBlockAndUpdate(base.above(), Blocks.STONE.defaultBlockState())
            val gimbal = level.getBlockEntity(base) as GimbalBearingBlockEntity
            gimbal.modeBehaviour!!.setValue(GimbalMode.UNLOCKED.ordinal)
            gimbal.assembleNextTick = true
            helper.startSequence().thenWaitUntil {
                helper.assertTrue(gimbal.jointID >= 0, "Gimbal did not create a joint")
            }.thenExecute {
                val child = level.shipObjectWorld.allShips.getById(gimbal.connectedShipId)!!
                child.isStatic = true
                remember(level, child)
                val target = Quaterniond().rotationTo(Vector3d(0.0, 1.0, 0.0), Vector3d(0.25, 1.0, -0.5).normalize())
                    .mul(gimbal.getRenderHostToSubRotation().invert())
                val delta = target.mul(child.transform.shipToWorldRotation.invert(Quaterniond()))
                rotate(level, child, delta, Vector3d(gimbal.joint!!.pose1.pos))
            }.thenWaitUntil {
                expect(helper, base, Direction.NORTH, "tilt", Math.toDegrees(kotlin.math.atan(kotlin.math.hypot(0.25, 0.5))))
                expect(helper, base, Direction.NORTH, "tilt_1", Math.toDegrees(kotlin.math.atan(0.5)))
                expect(helper, base, Direction.NORTH, "tilt_2", Math.toDegrees(kotlin.math.atan(0.25)))
                helper.assertTrue(read(helper, base, Direction.NORTH, "target_tilt") == null, "Unlocked gimbal reported a position target")
            }.thenSucceed()
        }

        private fun fixture(helper: GameTestHelper): BlockPos = helper.absolutePos(BlockPos(2, 0, 2)).atY(220).also {
            blocks.add(it)
            observers.add(ShipTestObserver(helper.level, it))
        }

        private fun remember(level: ServerLevel, ship: ServerShip): BlockPos {
            ships.add(ship)
            val bounds = ship.shipAABB!!
            return BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()).also { force(level, it) }
        }

        private fun force(level: ServerLevel, pos: BlockPos) {
            val chunk = ChunkPos(pos)
            if (level.setChunkForced(chunk.x, chunk.z, true)) chunks.add(chunk)
        }

        private fun rotate(level: ServerLevel, ship: ServerShip, delta: Quaterniond, pivot: Vector3d) {
            val position = Vector3d(ship.transform.positionInWorld).sub(pivot).rotate(delta).add(pivot)
            val rotation = delta.mul(ship.transform.shipToWorldRotation, Quaterniond())
            level.shipObjectWorld.teleportShip(ship, vsCore.newShipTeleportData(position, rotation, Vector3d(), Vector3d()))
        }

        private fun read(helper: GameTestHelper, pos: BlockPos, side: Direction, reading: String): Double? =
            ThinkableConnections.device(helper.level, pos)?.thinkableValue(side, reading)

        private fun expect(helper: GameTestHelper, pos: BlockPos, side: Direction, reading: String, expected: Double) {
            val actual = read(helper, pos, side, reading)
            helper.assertTrue(actual != null && abs(actual - expected) < 0.1, "$reading was $actual, expected $expected")
        }
    }
}
