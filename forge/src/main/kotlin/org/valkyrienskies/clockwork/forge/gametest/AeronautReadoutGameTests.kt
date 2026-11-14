package org.valkyrienskies.clockwork.forge.gametest

import com.mojang.authlib.GameProfile
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.Connection
import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.handshake.ClientIntentionPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import net.minecraftforge.network.NetworkHooks
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.*
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class AeronautReadoutGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private val ships = ArrayList<ServerShip>()
        private val observers = ArrayList<ServerPlayer>()
        private val connections = ArrayList<EmbeddedChannel>()

        @JvmStatic
        @AfterBatch(batch = "aeronaut_readout")
        fun cleanup(level: ServerLevel) {
            ships.forEach { level.shipObjectWorld.deleteShip(it) }
            ships.clear()
            observers.forEach { level.server.playerList.remove(it) }
            observers.clear()
            connections.forEach { it.finishAndReleaseAll() }
            connections.clear()
            AeronautShipReadout.clear()
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = "aeronaut_readout")
        fun inspectionRequiresEquippedLoweredGoggles(helper: GameTestHelper) {
            val player = ServerPlayer(helper.level.server, helper.level, GameProfile(UUID.randomUUID(), "hud-test"))
            val state = player as MixinPlayerDuck
            helper.assertTrue(!AeronautShipRequestPacket.canInspect(player), "An unequipped player could inspect ships")
            player.setItemSlot(EquipmentSlot.HEAD, ClockworkItems.AERONAUT_GOGGLES.asStack())
            helper.assertTrue(!AeronautShipRequestPacket.canInspect(player), "Raised goggles could inspect ships")
            state.gogglesDown = true
            helper.assertTrue(AeronautShipRequestPacket.canInspect(player), "Lowered equipped goggles could not inspect ships")
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY)
            helper.assertTrue(!AeronautShipRequestPacket.canInspect(player), "Removing goggles retained inspection access")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = "aeronaut_readout", timeoutTicks = 100)
        fun designCentersUpdateAfterRemovingSources(helper: GameTestHelper) {
            // VS needs a nearby observer to load the assembled ship in a headless server.
            val connection = object : Connection(PacketFlow.SERVERBOUND) {
                override fun send(packet: Packet<*>, listener: PacketSendListener?) { listener?.onSuccess() }
            }
            val channel = EmbeddedChannel()
            channel.pipeline().addLast("packet_handler", connection)
            channel.pipeline().fireChannelActive()
            connections.add(channel)
            NetworkHooks.registerServerLoginChannel(connection, ClientIntentionPacket("localhost", 0, ConnectionProtocol.LOGIN))
            val observer = ServerPlayer(helper.level.server, helper.level, GameProfile(UUID.randomUUID(), "readout-test"))
            helper.level.server.playerList.placeNewPlayer(connection, observer)
            val observerPos = helper.absolutePos(BlockPos(2, 10, 2))
            observer.setPos(observerPos.x.toDouble(), observerPos.y.toDouble(), observerPos.z.toDouble())
            observer.setNoGravity(true)
            observers.add(observer)
            val left = BlockPos(1, 2, 1)
            val right = BlockPos(3, 2, 1)
            val middle = BlockPos(2, 2, 1)
            val rocket = BlockPos(2, 2, 2)
            helper.setBlock(left, ClockworkBlocks.WING.get().defaultBlockState())
            helper.setBlock(right, ClockworkBlocks.WING.get().defaultBlockState())
            helper.setBlock(middle, Blocks.STONE.defaultBlockState())
            helper.setBlock(rocket, ClockworkBlocks.SUGAR_ROCKET.get().defaultBlockState())
            val positions = listOf(left, right, middle, rocket).map { helper.absolutePos(it) }.toSet()
            val ship = ShipAssembler.assembleToShip(helper.level, positions, 1.0)
            ships.add(ship)
            ship.isStatic = true
            helper.runAfterDelay(5) {
                val loadedShip = helper.level.shipObjectWorld.loadedShips.getById(ship.id)
                helper.assertTrue(loadedShip != null, "Observer did not load the ship")
                val bounds = loadedShip!!.shipAABB!!
                fun local(pos: BlockPos) = Vector3d(bounds.minX() + pos.x - left.x + .5,
                    bounds.minY() + pos.y - left.y + .5, bounds.minZ() + pos.z - left.z + .5)
                val leftLocal = local(left)
                val rightLocal = local(right)
                val rocketLocal = local(rocket)
                val snapshot = AeronautShipReadout.sample(helper.level, loadedShip!!)
                helper.assertTrue(snapshot.mass > 0, "Ship mass was missing")
                helper.assertTrue(snapshot.potentialThrust != null && snapshot.potentialThrust!!.position.distance(rocketLocal) < 1e-6,
                    "Unlit rocket's potential thrust ${snapshot.potentialThrust} was not at $rocketLocal; active chunks=${loadedShip.activeChunksSet.size}")
                helper.assertTrue(snapshot.liveThrust == null, "An unlit rocket produced a live thrust marker")
                val expectedLift = Vector3d(leftLocal).add(rightLocal).mul(.5)
                helper.assertTrue(snapshot.potentialLift != null && snapshot.potentialLift!!.position.distance(expectedLift) < 1e-6,
                    "Two equal wings did not balance between their positions")
                helper.level.setBlockAndUpdate(BlockPos.containing(rightLocal.x, rightLocal.y, rightLocal.z), Blocks.AIR.defaultBlockState())
                helper.level.setBlockAndUpdate(BlockPos.containing(rocketLocal.x, rocketLocal.y, rocketLocal.z), Blocks.AIR.defaultBlockState())
                helper.runAfterDelay(6) {
                    val updated = AeronautShipReadout.sample(helper.level, loadedShip)
                    helper.assertTrue(updated.potentialThrust == null, "Removing a rocket left a stale thrust marker")
                    helper.assertTrue(updated.potentialLift != null && updated.potentialLift!!.position.distance(leftLocal) < 1e-6,
                        "Removing a wing did not invalidate the cached lift center")
                    helper.succeed()
                }
            }
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = "aeronaut_readout")
        fun snapshotSurvivesTheNetworkCodec(helper: GameTestHelper) {
            val center = AeronautForceCenter(Vector3d(30000000.5, 70.0, -100.5), Vector3d(), 200.0)
            val snapshot = AeronautShipReadout.Snapshot(1500.0, Vector3d(30000001.5, 71.0, -101.5), center, null, null, center)
            val buffer = FriendlyByteBuf(Unpooled.buffer())
            try {
                AeronautShipReadoutPacket(42L, helper.level.dimension().location(), snapshot).write(buffer)
                val decoded = AeronautShipReadoutPacket(buffer)
                helper.assertTrue(decoded.shipId == 42L && decoded.dimension == helper.level.dimension().location(), "Ship identity changed in transit")
                helper.assertTrue(decoded.snapshot.mass == 1500.0 && decoded.snapshot.massCenter == snapshot.massCenter, "Mass data changed in transit")
                helper.assertTrue(decoded.snapshot.liveThrust == null && decoded.snapshot.potentialLift == null, "Missing centers became phantom markers")
                helper.assertTrue(decoded.snapshot.potentialThrust == center && decoded.snapshot.liveLift == center, "Zero resultant forces lost their weighted centers")
            } finally { buffer.release() }
            helper.succeed()
        }
    }
}
