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
import net.minecraft.world.phys.Vec3
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import net.minecraftforge.network.NetworkHooks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID
import kotlin.math.abs

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class RopeTensionGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private var previousSync = false
        private val ships = mutableListOf<ServerShip>()
        private val players = mutableListOf<ServerPlayer>()
        private val channels = mutableListOf<EmbeddedChannel>()
        private val chunks = mutableSetOf<ChunkPos>()
        private val links = mutableListOf<WandLinks.Link>()

        @JvmStatic @BeforeBatch(batch = "rope_tension")
        fun prepare(level: ServerLevel) {
            previousSync = VSCoreConfig.SERVER.pt.synchronizePhysics
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
        }

        @JvmStatic @AfterBatch(batch = "rope_tension")
        fun cleanup(level: ServerLevel) {
            links.forEach { WandLinks.get(level).remove(level, it) }; links.clear()
            ships.forEach { level.shipObjectWorld.deleteShip(it) }; ships.clear()
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }; chunks.clear()
            players.forEach { level.server.playerList.remove(it) }; players.clear()
            channels.forEach { it.finishAndReleaseAll() }; channels.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousSync
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun staticFirstDoesNotPullSlackRope(helper: GameTestHelper) = slack(helper, false, true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun staticSecondDoesNotPullSlackRope(helper: GameTestHelper) = slack(helper, false, false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun staticFirstDoesNotPropUpShip(helper: GameTestHelper) = slack(helper, true, true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun staticSecondDoesNotPropUpShip(helper: GameTestHelper) = slack(helper, true, false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun staticFirstSupportsHangingShip(helper: GameTestHelper) = slack(helper, true, true, hanging = true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun staticSecondSupportsHangingShip(helper: GameTestHelper) = slack(helper, true, false, hanging = true)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun magicalRopeDoesNotPropUpShip(helper: GameTestHelper) = slack(helper, true, true, physical = false)

        @JvmStatic @GameTest(template = TEMPLATE, batch = "rope_tension", timeoutTicks = 100)
        fun magicalRopeSupportsHangingShip(helper: GameTestHelper) = slack(helper, true, false, hanging = true, physical = false)

        private fun slack(helper: GameTestHelper, vertical: Boolean, staticFirst: Boolean, hanging: Boolean = false, physical: Boolean = true) {
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
            val player = ServerPlayer(level.server, level, GameProfile(UUID.randomUUID(), "rope-physics"))
            level.server.playerList.placeNewPlayer(connection, player)
            player.setPos(base.x.toDouble(), base.y + 5.0, base.z.toDouble())
            player.setNoGravity(true); player.abilities.flying = true
            players.add(player)
            fun ship(pos: BlockPos, fixed: Boolean): ServerShip {
                level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState())
                return ShipAssembler.assembleToShip(level, setOf(pos.mutable()), 1.0).also {
                    it.isStatic = fixed
                    ships.add(it)
                }
            }
            val fixed = ship(base, true)
            val moving = ship(if (hanging) base.below(6) else if (vertical) base.above(6) else base.offset(6, 0, 0), true)
            val control = ship(base.offset(3, 6, 3), false)
            fun anchor(ship: ServerShip): WandAnchor {
                val box = ship.shipAABB ?: error("Missing rope test ship bounds")
                val pos = BlockPos(box.minX(), box.minY(), box.minZ())
                val chunk = ChunkPos(pos)
                if (level.setChunkForced(chunk.x, chunk.z, true)) chunks.add(chunk)
                return WandAnchor(pos, Direction.NORTH, ship.id)
            }
            val a = anchor(fixed)
            val b = anchor(moving)
            anchor(control)
            var link: WandLinks.Link? = null
            var movingAnchor: WandAnchor? = null
            var initial = Vec3.ZERO
            var peakDistance = 0.0
            helper.startSequence().thenIdle(8).thenWaitUntil {
                helper.assertTrue(control.velocity.y() < -0.1,
                    "Unlinked control ship is not falling: velocity=${control.velocity}, mass=${control.inertiaData.mass}, static=${control.isStatic}")
            }.thenExecute {
                moving.isStatic = false
                movingAnchor = b
                val length = a.world(level)!!.distanceTo(b.world(level)!!) + 3.0
                link = WandLinks.get(level).add(level, if (staticFirst) a else b, if (staticFirst) b else a,
                    true, length, physical = physical)
                helper.assertTrue(link != null, "Failed to create rope")
                links.add(link!!)
            }.thenWaitUntil {
                helper.assertTrue(link?.jointId != null, "Rope joint was not created")
            }.thenExecute {
                initial = movingAnchor!!.world(level)!!
                helper.assertTrue(initial.y > 100 && !moving.isStatic, "Rope fixture must be dynamic and clear of terrain: $initial")
            }.thenExecuteFor(if (hanging) 40 else 8) {
                if (hanging) {
                    val distance = a.world(level)!!.distanceTo(b.world(level)!!)
                    peakDistance = maxOf(peakDistance, distance)
                    helper.assertTrue(distance < link!!.length + 1.0, "Rope did not arrest its falling load: $distance / ${link!!.length}")
                }
            }.thenExecute {
                val current = movingAnchor!!.world(level)!!
                val distance = link!!.a.world(level)!!.distanceTo(link!!.b.world(level)!!)
                helper.assertTrue(WandLinks.get(level).links.containsKey(link!!.id),
                    "Rope broke under gravity: distance=$distance, length=${link!!.length}, missing=${link!!.missingTicks}, age=${link!!.age}")
                helper.assertTrue(link!!.missingTicks == 0, "Native rope joint is missing")
                if (hanging) {
                    helper.assertTrue(peakDistance > link!!.length && peakDistance < link!!.length + 1.0,
                        "Rope should stretch slightly and support its load: peak=$peakDistance / ${link!!.length}")
                    return@thenExecute
                }
                helper.assertTrue(distance < link!!.length - 0.5, "Fixture exhausted its slack: $distance / ${link!!.length}")
                if (vertical) helper.assertTrue(current.y < initial.y - 0.5,
                    "Rope propped up a ship: initial=$initial, current=$current")
                else helper.assertTrue(abs(current.x - initial.x) < 0.1 && abs(current.z - initial.z) < 0.1,
                    "Slack rope pulled toward the static ship: initial=$initial, current=$current")
            }.thenSucceed()
        }
    }
}
