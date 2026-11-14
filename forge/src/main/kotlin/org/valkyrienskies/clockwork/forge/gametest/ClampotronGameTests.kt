package org.valkyrienskies.clockwork.forge.gametest

import com.simibubi.create.AllBlocks
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.BeforeBatch
import net.minecraft.gametest.framework.AfterBatch
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
import org.valkyrienskies.clockwork.content.curiosities.clampotron.ClampotronBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.clampotron.ClampotronController
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.shipObjectWorld

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class ClampotronGameTests {
    companion object {
        private val ships = mutableListOf<ServerShip>()
        private val chunks = mutableSetOf<ChunkPos>()
        private val joints = mutableListOf<Int>()
        private var previousSync = false

        @JvmStatic @BeforeBatch(batch = "clampotron")
        fun prepare(level: ServerLevel) {
            previousSync = VSCoreConfig.SERVER.pt.synchronizePhysics
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
        }

        @JvmStatic @AfterBatch(batch = "clampotron")
        fun cleanup(level: ServerLevel) {
            joints.forEach { level.gtpa.removeJoint(it) }
            joints.clear()
            ships.forEach { ShipAssembler.deleteShip(level, it, true, false) }
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }
            ships.clear(); chunks.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousSync
        }

        @JvmStatic
        @GameTest(template = "clockworkforgegametests.bladetestpositive", batch = "clampotron", timeoutTicks = 160)
        fun lightCarrierHoldsHeavyCargo(helper: GameTestHelper) = movingCarrier(helper, false)

        @JvmStatic
        @GameTest(template = "clockworkforgegametests.bladetestpositive", batch = "clampotron", timeoutTicks = 160)
        fun movingCarrierHoldsHingedCargo(helper: GameTestHelper) = movingCarrier(helper, true)

        private fun movingCarrier(helper: GameTestHelper, jointed: Boolean) {
            val level = helper.level
            val base = helper.absolutePos(BlockPos(2, 2, 2)).let { BlockPos(it.x, 220, it.z) }
            fun assemble(pos: BlockPos, heavy: Boolean): ServerShip {
                level.setBlockAndUpdate(pos, (if (heavy) Blocks.NETHERITE_BLOCK else Blocks.OAK_LOG).defaultBlockState())
                val ship = ShipAssembler.assembleToShip(level, setOf(pos), 1.0)
                ship.isStatic = true
                ships.add(ship)
                val box = ship.shipAABB!!
                val chunk = ChunkPos(BlockPos(box.minX(), box.minY(), box.minZ()))
                if (level.setChunkForced(chunk.x, chunk.z, true)) chunks.add(chunk)
                return ship
            }
            val carrier = assemble(base, false)
            val cargo = assemble(base.east(6), !jointed)
            val payload = if (jointed) assemble(base.east(6).below(3), true) else null
            var jointId: Int? = null
            var command: ClampotronController.Grip? = null
            var controller: ClampotronController? = null
            helper.startSequence().thenIdle(15).thenExecute {
                if (payload != null) {
                    val point = Vector3d(cargo.transform.positionInWorld)
                    level.gtpa.addJoint(VSRevoluteJoint(cargo.id,
                        VSJointPose(cargo.worldToShip.transformPosition(point, Vector3d()), Quaterniond()), payload.id,
                        VSJointPose(payload.worldToShip.transformPosition(point, Vector3d()), Quaterniond()))) { id ->
                        level.server.execute { jointId = id; joints.add(id) }
                    }
                }
            }.thenWaitUntil {
                helper.assertTrue(!jointed || jointId != null, "Waiting for hinged payload")
            }.thenExecute {
                val point = Vector3d(cargo.transform.positionInWorld).add(0.0, 0.4, 0.0)
                command = ClampotronController.Grip(base.asLong(), carrier.id,
                    cargo.worldToShip.transformPosition(point, Vector3d()),
                    carrier.worldToShip.transformPosition(Vector3d(point).add(0.0, 0.35, 0.0)),
                    Quaterniond(carrier.transform.shipToWorldRotation).invert().mul(cargo.transform.shipToWorldRotation), 10_240_000.0)
                carrier.isStatic = false
                cargo.isStatic = false
                payload?.isStatic = false
                controller = ClampotronController.getOrCreate(level.shipObjectWorld.loadedShips.getById(cargo.id)!!)
            }.thenExecuteFor(60) {
                controller!!.grip = command!!.copy(refreshed = System.nanoTime())
                for (ship in listOfNotNull(carrier, cargo, payload)) {
                    helper.assertTrue(ship.velocity.isFinite && ship.angularVelocity.isFinite && ship.angularVelocity.length() < 8.0,
                        "Ship-mounted grip caused runaway rotation: ${ship.angularVelocity}")
                }
                helper.assertTrue(controller!!.error < 2.0, "Ship-mounted grip diverged: ${controller!!.error}")
            }.thenExecute {
                helper.assertTrue(controller!!.error < 0.25, "Ship-mounted grip did not settle: ${controller!!.error}")
                controller!!.grip = null
            }.thenSucceed()
        }

        @JvmStatic
        @GameTest(template = "clockworkforgegametests.bladetestpositive", batch = "clampotron", timeoutTicks = 120)
        fun capturedShipReleasesImmediatelyWhenNetworkOverstresses(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(5, 5, 5))
            level.setBlockAndUpdate(pos.below(), AllBlocks.CREATIVE_MOTOR.defaultState.setValue(FACING, Direction.UP))
            val motor = level.getBlockEntity(pos.below()) as CreativeMotorBlockEntity
            motor.generatedSpeed.setValue(64)
            level.setBlockAndUpdate(pos, ClockworkBlocks.CLAMPOTRON.defaultState)
            level.setBlockAndUpdate(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState())
            level.setBlockAndUpdate(pos.above(2), Blocks.STONE.defaultBlockState())
            val ship = ShipAssembler.assembleToShip(level, setOf(pos.above(2)), 1.0)
            ships.add(ship)
            ship.isStatic = true
            val bounds = ship.shipAABB!!
            val chunk = ChunkPos(BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()))
            if (level.setChunkForced(chunk.x, chunk.z, true)) chunks.add(chunk)
            helper.startSequence().thenIdle(10).thenExecute {
                val be = level.getBlockEntity(pos) as ClampotronBlockEntity
                helper.assertTrue(be.getSpeed() == 64f, "Shaft did not power the Clampotron")
                ship.isStatic = false
            }.thenWaitUntil {
                val be = level.getBlockEntity(pos) as ClampotronBlockEntity
                helper.assertTrue(be.heldShip == ship.id, "Clampotron failed to acquire the ship in its beam")
            }.thenExecute {
                val be = level.getBlockEntity(pos) as ClampotronBlockEntity
                val bounds = ship.shipAABB!!
                helper.assertTrue(be.anchor.x >= bounds.minX() - 0.01 && be.anchor.x <= bounds.maxX() + 1.01 &&
                    be.anchor.y >= bounds.minY() - 0.01 && be.anchor.y <= bounds.maxY() + 1.01 &&
                    be.anchor.z >= bounds.minZ() - 0.01 && be.anchor.z <= bounds.maxZ() + 1.01,
                    "Grab anchor must be in ship-local coordinates: ${be.anchor}")
            }.thenIdle(40).thenExecute {
                val be = level.getBlockEntity(pos) as ClampotronBlockEntity
                helper.assertTrue(be.heldShip == ship.id, "Clampotron dropped its load during a sustained hold")
                helper.assertTrue(be.positionError < 0.2, "Held object did not settle at the commanded reach: ${be.positionError}")
            }.thenExecute {
                val be = level.getBlockEntity(pos) as ClampotronBlockEntity
                val loaded = level.shipObjectWorld.loadedShips.getById(ship.id)!!
                helper.assertTrue(loaded.getAttachment<ClampotronController>()?.active() == true, "No active holding controller")
                helper.assertTrue(be.calculateStressApplied() > 1f, "Held mass did not increase kinetic stress")
                be.updateFromNetwork(0f, 100000f, 2)
                helper.assertTrue(!be.enabled && be.heldShip < 0, "Overstress retained the held ship")
                helper.assertTrue(loaded.getAttachment<ClampotronController>()?.grip == null, "Overstress left physics forces active")
                level.removeBlock(pos, false)
                level.removeBlock(pos.east(), false)
                level.removeBlock(pos.below(), false)
            }.thenSucceed()
        }

        @JvmStatic
        @GameTest(template = "clockworkforgegametests.bladetestpositive", batch = "clampotron")
        fun overstressDisablesPoweredClampotron(helper: GameTestHelper) {
            val pos = helper.absolutePos(BlockPos(3, 4, 3))
            val level = helper.level
            level.setBlockAndUpdate(pos, ClockworkBlocks.CLAMPOTRON.defaultState)
            level.setBlockAndUpdate(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState())
            helper.runAtTickTime(3) {
                val be = level.getBlockEntity(pos) as ClampotronBlockEntity
                be.setSpeed(64f)
                be.updateFromNetwork(4096f, 64f, 2)
                be.tick()
                helper.assertTrue(be.enabled, "Powered rotating Clampotron did not activate")
                be.updateFromNetwork(32f, 64f, 2)
                helper.assertTrue(be.isOverStressed, "Test network did not overstress")
                helper.assertTrue(!be.enabled, "Overstress did not immediately shut off the field")
                be.tick()
                helper.assertTrue(!be.enabled && be.heldShip < 0, "Redstone reactivated the overstressed Clampotron")
                be.updateFromNetwork(4096f, 64f, 2)
                be.tick()
                helper.assertTrue(be.enabled, "Clampotron did not recover with sufficient network capacity")
                level.removeBlock(pos, false)
                level.removeBlock(pos.east(), false)
                helper.succeed()
            }
        }
    }
}
