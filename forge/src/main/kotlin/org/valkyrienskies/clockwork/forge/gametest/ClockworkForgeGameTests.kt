package org.valkyrienskies.clockwork.forge.gametest

import com.mojang.authlib.GameProfile
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.BeforeBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.Connection
import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.handshake.ClientIntentionPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.network.NetworkHooks
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity.RotationDirection
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.copter.CopterBearingBlockEntity
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsPipeline
import java.util.UUID


@Suppress("unused")
@GameTestHolder(ClockworkMod.MOD_ID)
class ClockworkForgeGameTests {
    companion object {
        private var previousSynchronizePhysics = false
        private val testShips = mutableListOf<ServerShip>()
        private val testChunks = mutableSetOf<ChunkPos>()
        private val testPlayers = mutableListOf<ServerPlayer>()
        private val testConnections = mutableListOf<EmbeddedChannel>()
        private val runwayBlocks = mutableMapOf<BlockPos, BlockState>()
        private var savedPropellerConfig = doubleArrayOf()

        @JvmStatic
        @BeforeBatch(batch = "props")
        fun preparePropTests(level: ServerLevel) {
            ClockworkMod.LOGGER.info("Prop test physics: backend={}, dummy={}, running={}",
                level.server.vsPipeline.getPhysicsBackendType(), level.server.vsPipeline.isUsingDummyPhysics,
                level.server.vsPipeline.arePhysicsRunning)
            previousSynchronizePhysics = VSCoreConfig.SERVER.pt.synchronizePhysics
            // GameTest ticks run faster than wall time; give each one its corresponding physics steps.
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
            savedPropellerConfig = doubleArrayOf(ClockworkConfig.SERVER.forceMulPerSailInPropeller,
                ClockworkConfig.SERVER.propellerMaxForce, ClockworkConfig.SERVER.propellerMaxTorque,
                ClockworkConfig.SERVER.sailPropellerForceMultiplier)
            val defaults = ClockworkConfig.Server()
            ClockworkConfig.SERVER.forceMulPerSailInPropeller = defaults.forceMulPerSailInPropeller
            ClockworkConfig.SERVER.sailPropellerForceMultiplier = defaults.sailPropellerForceMultiplier
            ClockworkConfig.SERVER.propellerMaxForce = defaults.propellerMaxForce
            ClockworkConfig.SERVER.propellerMaxTorque = defaults.propellerMaxTorque
        }

        @JvmStatic
        @AfterBatch(batch = "props")
        fun cleanupPropTests(level: ServerLevel) {
            for (ship in testShips) level.shipObjectWorld.deleteShip(ship)
            testShips.clear()
            for (chunk in testChunks) level.setChunkForced(chunk.x, chunk.z, false)
            testChunks.clear()
            for (player in testPlayers) level.server.playerList.remove(player)
            testPlayers.clear()
            for (connection in testConnections) connection.finishAndReleaseAll()
            testConnections.clear()
            for ((pos, state) in runwayBlocks) level.setBlockAndUpdate(pos, state)
            runwayBlocks.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousSynchronizePhysics
            ClockworkConfig.SERVER.forceMulPerSailInPropeller = savedPropellerConfig[0]
            ClockworkConfig.SERVER.propellerMaxForce = savedPropellerConfig[1]
            ClockworkConfig.SERVER.propellerMaxTorque = savedPropellerConfig[2]
            ClockworkConfig.SERVER.sailPropellerForceMultiplier = savedPropellerConfig[3]
        }

        @JvmStatic
        @GameTest(timeoutTicks = 40, batch = "copter_controls", template = "bladetestpositive")
        fun copterRotationSettingKeepsTiltControls(helper: GameTestHelper) {
            val pos = BlockPos(1, 1, 1)
            for (facing in Direction.values()) {
                helper.setBlock(pos, ClockworkBlocks.COPTER_BEARING.get().defaultBlockState()
                    .setValue(BlockStateProperties.FACING, facing))
                val bearing = helper.getBlockEntity(pos) as CopterBearingBlockEntity
                bearing.powerOne = 8
                bearing.powerTwo = -3
                for (rpm in listOf(64f, -64f, 0f)) {
                    bearing.speed = rpm
                    bearing.rotationDirection.setValue(RotationDirection.NORMAL.ordinal)
                    val expectedScale = if (rpm == 0f || rpm * facing.axisDirection.step > 0f) 1f else -1f
                    helper.assertTrue(bearing.getDirectionScale() == expectedScale, "Wrong input-RPM sign for $facing at $rpm RPM")
                    bearing.applyPowerEffect()
                    val normalOffset = Vector3d(bearing.desiredLocalOffset)

                    bearing.rotationDirection.setValue(RotationDirection.INVERTED.ordinal)
                    helper.assertTrue(bearing.isInverted(), "Reversed setting was not applied")
                    helper.assertTrue(bearing.getDirectionScale() == expectedScale,
                        "Reversed rotor setting changed the copter tilt target for $facing at $rpm RPM")
                    bearing.applyPowerEffect()
                    helper.assertTrue(normalOffset.distance(bearing.desiredLocalOffset) < 1e-12,
                        "Reversed rotor setting changed redstone tilt for $facing at $rpm RPM")
                }
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(timeoutTicks = 200, setupTicks = 12, batch = "props", template = "")
        // Template Location at 'data/vs_clockwork/structures/clockworkforgegametests.proptestpositive'
        fun propTestPositive(helper: GameTestHelper) {
            generalPropellerTest(helper, Direction.SOUTH)
        }

        @JvmStatic
        @GameTest(timeoutTicks = 200, setupTicks = 12, batch = "props")
        // Template Location at 'data/vs_clockwork/structures/clockworkforgegametests.proptestnegative'
        fun propTestNegative(helper: GameTestHelper) {
            generalPropellerTest(helper, Direction.NORTH)
        }

        @JvmStatic
        @GameTest(timeoutTicks = 200, setupTicks = 12, batch = "props")
        // Template Location at 'data/vs_clockwork/structures/clockworkforgegametests.bladetestnegative'
        fun bladeTestNegative(helper: GameTestHelper) {
            generalPropellerTest(helper, Direction.SOUTH)
        }

        @JvmStatic
        @GameTest(timeoutTicks = 200, setupTicks = 12, batch = "props")
        // Template Location at 'data/vs_clockwork/structures/clockworkforgegametests.bladetestpositive'
        fun bladeTestPositive(helper: GameTestHelper) {
            generalPropellerTest(helper, Direction.NORTH)
        }

        @JvmStatic
        @GameTest(timeoutTicks = 240, setupTicks = 12, batch = "props", template = "proptestpositive")
        fun sailPropAtLowRPM(helper: GameTestHelper) {
            generalPropellerTest(helper, Direction.SOUTH, rpm = 64)
        }

        @JvmStatic
        @GameTest(timeoutTicks = 240, setupTicks = 12, batch = "props", template = "bladetestpositive")
        fun bladePropAtLowRPM(helper: GameTestHelper) {
            generalPropellerTest(helper, Direction.NORTH, rpm = 64)
        }

        fun generalPropellerTest(helper: GameTestHelper, direction: Direction, rpm: Int? = null) {
            // Extend the fixture's ice floor; otherwise its edge tests terrain traversal, not propulsion.
            for (z in (-16..-1) + (7..22)) for (x in 1..5) for (y in 1..6) {
                val pos = helper.absolutePos(BlockPos(x, y, z))
                runwayBlocks.putIfAbsent(pos, helper.level.getBlockState(pos))
                helper.level.setBlockAndUpdate(pos, if (y == 1) Blocks.PACKED_ICE.defaultBlockState() else Blocks.AIR.defaultBlockState())
            }
            // Supply a nearby observer so VS loads and tracks the shipyard chunks in a headless run.
            // Forge/VS inspect the network pipeline during login, so the vanilla mock's null channel is insufficient.
            val connection = object : Connection(PacketFlow.SERVERBOUND) {
                // VS sends from its network thread too; EmbeddedChannel is not thread-safe.
                // This observer has no client, so acknowledge outbound packets without queueing them.
                override fun send(packet: Packet<*>, listener: PacketSendListener?) {
                    listener?.onSuccess()
                }
            }
            val channel = EmbeddedChannel()
            channel.pipeline().addLast("packet_handler", connection)
            channel.pipeline().fireChannelActive()
            testConnections.add(channel)
            NetworkHooks.registerServerLoginChannel(connection, ClientIntentionPacket("localhost", 0, ConnectionProtocol.LOGIN))
            val player = ServerPlayer(helper.level.server, helper.level, GameProfile(UUID.randomUUID(), "prop-test"))
            try {
                helper.level.server.playerList.placeNewPlayer(connection, player)
            } catch (e: Exception) {
                ClockworkMod.LOGGER.error("Could not add the physics test observer", e)
                throw e
            }
            val observerPos = helper.absolutePos(BlockPos(3, 20, 3))
            player.setPos(observerPos.x.toDouble(), observerPos.y.toDouble(), observerPos.z.toDouble())
            player.setNoGravity(true)
            player.abilities.flying = true
            testPlayers.add(player)
            val from = helper.absolutePos(BlockPos(2, 2, 2))
            val to = helper.absolutePos(BlockPos(4, 4, 5))
            val ship = ShipAssembler.assembleToShip(
                helper.level,
                BlockPos.betweenClosed(from, to).map { it.mutable() }.toSet(),
                1.0
            )
            testShips.add(ship)

            helper.runAfterDelay(5) {
                val aabb = ship.shipAABB
                helper.assertTrue(aabb != null, "Ship aabb was null")

                val corner = BlockPos(aabb!!.minX(), aabb.minY(), aabb.minZ())
                val bearing = corner.offset(1, 1, 2)
                // A fixed point on the hull avoids counting COM shifts during rotor assembly as travel.
                val trackedPoint = Vector3d(bearing.x + 0.5, bearing.y + 0.5, bearing.z + 0.5)
                val initialPosition = ship.transform.shipToWorld.transformPosition(trackedPoint, Vector3d())
                for (x in (aabb.minX() shr 4)..(aabb.maxX() shr 4)) {
                    for (z in (aabb.minZ() shr 4)..(aabb.maxZ() shr 4)) {
                        val chunk = ChunkPos(x, z)
                        if (helper.level.setChunkForced(x, z, true)) testChunks.add(chunk)
                    }
                }
                ClockworkMod.LOGGER.info("Prop test fixture: ship={}, direction={}, bearing={}, initial={}, aabb={}",
                    ship.id, direction, bearing, initialPosition, aabb)

                helper.runAfterDelay(10) {
                    if (rpm != null) {
                        val motor = helper.level.getBlockEntity(bearing.relative(Direction.NORTH)) as CreativeMotorBlockEntity
                        motor.generatedSpeed.setValue(rpm)
                    }
                    helper.useShipyardBlock(bearing)
                }

                helper.succeedWhen {
                    val be = helper.level.getBlockEntity(bearing) as PropellerBearingBlockEntity
                    helper.assertTrue(helper.level.shipObjectWorld.loadedShips.getById(ship.id) != null, "Test ship is not loaded into physics")
                    helper.assertTrue(be.running && be.active && kotlin.math.abs(be.currentOmega) > 1.0,
                        "Propeller is not running: active=${be.active}, input=${be.speed}, omega=${be.currentOmega}")
                    val currentPosition = ship.transform.shipToWorld.transformPosition(trackedPoint, Vector3d())
                    val travel = (currentPosition.z() - initialPosition.z) * direction.stepZ
                    helper.assertTrue(travel >= 5.0,
                        "Ship traveled $travel blocks toward $direction; expected at least 5. " +
                            "ship=${ship.id}, position=$currentPosition, velocity=${ship.velocity}, " +
                            "angularVelocity=${ship.angularVelocity}, static=${ship.isStatic}, rotorSpeed=${be.currentOmega}")
                    ClockworkMod.LOGGER.info("Propulsion test passed: {} RPM, {} blocks toward {}", be.speed, travel, direction)
                }
            }
        }
    }
}

/**
 * [GameTestHelper.useBlock] assumes local coordinates, which doesn't work with shipyard coordinates.
 * Even if we try to reverse transform the shipyard coordinate to local, precision issues occur.
 * So this method simply bypasses the local coordinate entirely and expects a world coordinate to interact with.
 */
fun GameTestHelper.useShipyardBlock(blockPos: BlockPos) {
    val player = this.makeMockPlayer()
    val blockState: BlockState = this.level.getBlockState(blockPos)
    val result = BlockHitResult(Vec3.atCenterOf(blockPos), Direction.NORTH, blockPos, true)
    val interactionResult = blockState.use(this.level, player, InteractionHand.MAIN_HAND, result)
    if (!interactionResult.consumesAction()) {
        val useOnContext = UseOnContext(player, InteractionHand.MAIN_HAND, result)
        player.getItemInHand(InteractionHand.MAIN_HAND).useOn(useOnContext)
    }
}
