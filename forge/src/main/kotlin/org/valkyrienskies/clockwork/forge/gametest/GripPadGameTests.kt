package org.valkyrienskies.clockwork.forge.gametest

import com.mojang.authlib.GameProfile
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.BeforeBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.Connection
import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.handshake.ClientIntentionPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import net.minecraftforge.network.NetworkHooks
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.grippad.GripPadBlock
import org.valkyrienskies.clockwork.content.curiosities.grippad.GripPadBlockEntity
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class GripPadGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private var previousSync = false
        private val ships = mutableListOf<ServerShip>()
        private val players = mutableListOf<ServerPlayer>()
        private val channels = mutableListOf<EmbeddedChannel>()
        private val chunks = mutableSetOf<ChunkPos>()

        @JvmStatic @BeforeBatch(batch = "grip_pads")
        fun prepare(level: ServerLevel) {
            previousSync = VSCoreConfig.SERVER.pt.synchronizePhysics
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
        }

        @JvmStatic @AfterBatch(batch = "grip_pads")
        fun cleanup(level: ServerLevel) {
            ships.forEach { level.shipObjectWorld.deleteShip(it) }; ships.clear()
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }; chunks.clear()
            players.forEach { level.server.playerList.remove(it) }; players.clear()
            channels.forEach { it.finishAndReleaseAll() }; channels.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousSync
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "grip_pads", timeoutTicks = 200)
        fun passivePadHasTractionAndContactOutput(helper: GameTestHelper) = exercise(helper, "passive")

        @JvmStatic @GameTest(template = TEMPLATE, batch = "grip_pads", timeoutTicks = 200)
        fun poweredPadReleasesOnPowerLoss(helper: GameTestHelper) = exercise(helper, "power")

        @JvmStatic @GameTest(template = TEMPLATE, batch = "grip_pads", timeoutTicks = 200)
        fun poweredPadReleasesWhenLifted(helper: GameTestHelper) = exercise(helper, "lift")

        @JvmStatic @GameTest(template = TEMPLATE, batch = "grip_pads", timeoutTicks = 200)
        fun poweredPadTracksMovingShipSurface(helper: GameTestHelper) = exercise(helper, "moving")

        @JvmStatic @GameTest(template = TEMPLATE, batch = "grip_pads", timeoutTicks = 40)
        fun redstoneControlsGripMode(helper: GameTestHelper) {
            val pos = helper.absolutePos(BlockPos(1, 2, 1))
            val level = helper.level
            level.setBlockAndUpdate(pos, ClockworkBlocks.GRIP_PAD.getDefaultState())
            helper.startSequence().thenExecute {
                level.setBlockAndUpdate(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState())
            }.thenWaitUntil {
                helper.assertTrue(level.getBlockState(pos).getValue(GripPadBlock.POWERED), "Redstone did not enable active mode")
            }.thenExecute {
                level.removeBlock(pos.east(), false)
            }.thenWaitUntil {
                helper.assertTrue(!level.getBlockState(pos).getValue(GripPadBlock.POWERED), "Removing redstone did not disable active mode")
            }.thenSucceed()
        }

        private fun exercise(helper: GameTestHelper, mode: String) {
            val level = helper.level
            val base = helper.absolutePos(BlockPos(2, 0, 2)).let { BlockPos(it.x, 180, it.z) }
            val connection = object : Connection(PacketFlow.SERVERBOUND) {
                override fun send(packet: Packet<*>, listener: PacketSendListener?) { listener?.onSuccess() }
            }
            val channel = EmbeddedChannel()
            channel.pipeline().addLast("packet_handler", connection)
            channel.pipeline().fireChannelActive()
            channels.add(channel)
            NetworkHooks.registerServerLoginChannel(connection, ClientIntentionPacket("localhost", 0, ConnectionProtocol.LOGIN))
            val player = ServerPlayer(level.server, level, GameProfile(UUID.randomUUID(), "grip-pad-test"))
            level.server.playerList.placeNewPlayer(connection, player)
            player.setPos(base.x.toDouble(), base.y + 5.0, base.z.toDouble())
            player.setNoGravity(true); player.abilities.flying = true
            players.add(player)
            fun assemble(blocks: Set<BlockPos>): ServerShip =
                ShipAssembler.assembleToShip(level, blocks.map { it.mutable() }.toSet(), 1.0).also {
                    it.isStatic = true; ships.add(it)
                    val bounds = it.shipAABB!!
                    for (x in (bounds.minX() shr 4)..(bounds.maxX() shr 4)) for (z in (bounds.minZ() shr 4)..(bounds.maxZ() shr 4)) {
                        if (level.setChunkForced(x, z, true)) chunks.add(ChunkPos(x, z))
                    }
                }
            val floor = (-1..2).flatMap { x -> (-1..2).map { z -> base.offset(x, 0, z) } }.toSet()
            floor.forEach { level.setBlockAndUpdate(it, Blocks.STONE.defaultBlockState()) }
            val platform = if (mode == "moving") assemble(floor) else null
            val footBlocks = (0..1).flatMap { x -> (0..1).map { z -> base.offset(x, 2, z) } }.toSet()
            val padState = ClockworkBlocks.GRIP_PAD.getDefaultState().setValue(DirectionalBlock.FACING, Direction.DOWN)
                .setValue(GripPadBlock.POWERED, mode != "passive")
            footBlocks.forEach { level.setBlockAndUpdate(it, padState) }
            val foot = assemble(footBlocks)
            fun pads(): List<GripPadBlockEntity> {
                val bounds = foot.shipAABB!!
                return BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())
                    .mapNotNull { level.getBlockEntity(it) as? GripPadBlockEntity }
            }
            var start = Vector3d()
            var platformStart = Vector3d()
            val acceleration = AtomicReference(Vector3d())
            helper.startSequence().thenIdle(15).thenExecute {
                helper.assertTrue(pads().size == 4, "Foot lost its pads during assembly")
                pads().forEach { level.setBlock(it.blockPos, it.blockState.setValue(GripPadBlock.POWERED, mode != "passive"), 2) }
                foot.isStatic = false
            }.thenWaitUntil {
                helper.assertTrue(pads().all { it.contact && (mode == "passive" || it.gripping) }, "Pads have not contacted the floor")
            }.thenIdle(15).thenExecute {
                helper.assertTrue(pads().all { it.blockState.getAnalogOutputSignal(level, it.blockPos) == 15 }, "Contact did not produce comparator output")
                if (mode == "passive") helper.assertTrue(pads().none { it.gripping }, "Unpowered pad engaged active grip")
                start = Vector3d(foot.transform.positionInWorld)
                platformStart = Vector3d(platform?.transform?.positionInWorld ?: start)
                if (platform != null) platform.isStatic = false
                val bodyId = (platform ?: foot).id
                val until = System.nanoTime() + 10_000_000_000L
                acceleration.set(if (platform != null) Vector3d(2.0, 10.0, 0.0)
                    else Vector3d(if (mode == "passive") 5.0 else 30.0, 0.0, 0.0))
                ClockworkMod.physTickOnce(level.dimensionId) { physics, _, again ->
                    val body = physics.getBodyById(bodyId)
                    if (body != null && System.nanoTime() < until) {
                        body.applyWorldForce(Vector3d(acceleration.get()).mul(body.inertiaData.mass))
                        again()
                    }
                }
            }.thenExecuteFor(20) {
                helper.assertTrue(foot.angularVelocity.length() < 10.0, "Grip made the foot spin uncontrollably")
            }.thenExecute {
                if (platform != null) {
                    val travel = platform.transform.positionInWorld.x() - platformStart.x
                    helper.assertTrue(travel > 0.25, "Platform did not move")
                    helper.assertTrue(abs((foot.transform.positionInWorld.x() - start.x) - travel) < 0.3, "Grip pinned the foot to world space")
                } else helper.assertTrue(foot.transform.positionInWorld.distance(start) < 0.4,
                    "Pad failed to hold its load: movement=${foot.transform.positionInWorld.distance(start)}, grip=${pads().map { it.gripping }}")
                if (mode == "power") pads().forEach { level.setBlock(it.blockPos, it.blockState.setValue(GripPadBlock.POWERED, false), 2) }
                acceleration.set(when (mode) {
                    "power" -> Vector3d(30.0, 0.0, 0.0)
                    "lift" -> Vector3d(0.0, 40.0, 0.0)
                    "moving" -> Vector3d(0.0, 10.0, 0.0)
                    else -> Vector3d()
                })
                start = Vector3d(foot.transform.positionInWorld)
            }.thenIdle(20).thenExecute {
                acceleration.set(Vector3d())
                if (mode == "power") {
                    helper.assertTrue(pads().none { it.gripping }, "Pad retained grip without power")
                    helper.assertTrue(foot.transform.positionInWorld.x() - start.x > 0.3,
                        "Unpowered foot remained locked: movement=${foot.transform.positionInWorld.x() - start.x}")
                }
                if (mode == "lift") {
                    helper.assertTrue(foot.transform.positionInWorld.y() - start.y > 1.0, "Grip resisted lifting away")
                    helper.assertTrue(pads().none { it.contact || it.gripping }, "Lifted pad retained its contact")
                    helper.assertTrue(pads().all { it.blockState.getAnalogOutputSignal(level, it.blockPos) == 0 }, "Lifted pad retained comparator output")
                }
            }.thenSucceed()
        }
    }
}
