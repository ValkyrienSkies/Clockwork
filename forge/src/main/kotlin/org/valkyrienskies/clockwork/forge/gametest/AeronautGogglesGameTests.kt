package org.valkyrienskies.clockwork.forge.gametest

import com.mojang.authlib.GameProfile
import io.netty.buffer.Unpooled
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.Connection
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.server.level.ServerEntity
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautGogglesState
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautGogglesTogglePacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import java.util.UUID

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class AeronautGogglesGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = "aeronaut_goggles")
        fun togglesOnlyTheEquippedSender(helper: GameTestHelper) {
            val first = player(helper)
            val second = player(helper)
            first.setItemSlot(EquipmentSlot.HEAD, ClockworkItems.AERONAUT_GOGGLES.asStack())
            second.setItemSlot(EquipmentSlot.HEAD, ClockworkItems.AERONAUT_GOGGLES.asStack())

            helper.assertTrue(!down(first) && !down(second), "Goggles should start raised")
            toggle(helper, first)
            helper.assertTrue(down(first) && !down(second), "Toggling one player changed another player's goggles")
            toggle(helper, second)
            toggle(helper, first)
            helper.assertTrue(!down(first) && down(second), "Each player must be able to toggle independently")

            first.setItemSlot(EquipmentSlot.HEAD, ItemStack(Items.IRON_HELMET))
            first.setItemSlot(EquipmentSlot.MAINHAND, ClockworkItems.AERONAUT_GOGGLES.asStack())
            toggle(helper, first)
            helper.assertTrue(!down(first), "Holding goggles must not allow a toggle")
            first.setItemSlot(EquipmentSlot.HEAD, ClockworkItems.AERONAUT_GOGGLES.asStack())
            first.health = 0f
            toggle(helper, first)
            helper.assertTrue(!down(first), "A dead player must not be able to toggle goggles")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = "aeronaut_goggles")
        fun syncsToWearerTrackingAndLateTrackingPlayers(helper: GameTestHelper) {
            val wearerPackets = mutableListOf<Packet<*>>()
            val trackingPackets = mutableListOf<Packet<*>>()
            val wearer = player(helper, wearerPackets)
            val observer = player(helper)
            val wearerView = helper.makeMockPlayer()
            val trackingView = helper.makeMockPlayer()
            val lateTrackingView = helper.makeMockPlayer()
            wearer.setItemSlot(EquipmentSlot.HEAD, ClockworkItems.AERONAUT_GOGGLES.asStack())
            val tracker = ServerEntity(helper.level, wearer, 1, false) { trackingPackets.add(it) }

            toggle(helper, wearer)
            tracker.sendChanges()
            applyMetadata(wearerPackets, wearerView)
            applyMetadata(trackingPackets, trackingView)
            helper.assertTrue(down(wearerView) && down(trackingView), "Toggle did not sync to wearer and observer")

            val pairingPackets = mutableListOf<Packet<*>>()
            tracker.sendPairingData(observer) { pairingPackets.add(it) }
            applyMetadata(pairingPackets, lateTrackingView)
            helper.assertTrue(down(lateTrackingView), "A new observer did not receive the already-lowered state")

            wearerPackets.clear()
            trackingPackets.clear()
            toggle(helper, wearer)
            tracker.sendChanges()
            applyMetadata(wearerPackets, wearerView)
            applyMetadata(trackingPackets, trackingView)
            applyMetadata(trackingPackets, lateTrackingView)
            helper.assertTrue(!down(wearerView) && !down(trackingView) && !down(lateTrackingView),
                "Returning to the default raised state did not sync")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = "aeronaut_goggles")
        fun nonPlayerWearersUseRaisedGoggles(helper: GameTestHelper) {
            val wearer = player(helper)
            wearer.setItemSlot(EquipmentSlot.HEAD, ClockworkItems.AERONAUT_GOGGLES.asStack())
            toggle(helper, wearer)
            val stand = ArmorStand(helper.level, 0.0, 0.0, 0.0)
            stand.setItemSlot(EquipmentSlot.HEAD, ClockworkItems.AERONAUT_GOGGLES.asStack())
            helper.assertTrue(!AeronautGogglesState.getGogglesAreDown(stand),
                "An armor stand must not inherit a player's lowered goggles")
            helper.succeed()
        }

        private fun down(player: Player) = AeronautGogglesState.getGogglesAreDown(player)

        private fun player(helper: GameTestHelper, packets: MutableList<Packet<*>> = mutableListOf()): ServerPlayer {
            val player = ServerPlayer(helper.level.server, helper.level, GameProfile(UUID.randomUUID(), "goggles-test"))
            val connection = object : Connection(PacketFlow.SERVERBOUND) {
                override fun send(packet: Packet<*>, listener: PacketSendListener?) {
                    packets.add(packet)
                    listener?.onSuccess()
                }
            }
            player.connection = ServerGamePacketListenerImpl(helper.level.server, connection, player)
            return player
        }

        private fun toggle(helper: GameTestHelper, player: ServerPlayer) {
            val work = mutableListOf<Runnable>()
            var handled = false
            val context = object : ServerNetworkContext {
                override val sender = player
                override fun enqueueWork(runnable: Runnable) { work.add(runnable) }
                override fun setPacketHandled(value: Boolean) { handled = value }
            }
            val buffer = FriendlyByteBuf(Unpooled.buffer())
            try {
                AeronautGogglesTogglePacket().write(buffer)
                AeronautGogglesTogglePacket(buffer).handle(context)
            } finally {
                buffer.release()
            }
            helper.assertTrue(handled, "The toggle packet was not marked handled")
            work.forEach(Runnable::run)
        }

        private fun applyMetadata(packets: List<Packet<*>>, view: Player) {
            for (packet in packets.filterIsInstance<ClientboundSetEntityDataPacket>()) {
                val buffer = FriendlyByteBuf(Unpooled.buffer())
                try {
                    packet.write(buffer)
                    view.entityData.assignValues(ClientboundSetEntityDataPacket(buffer).packedItems())
                } finally {
                    buffer.release()
                }
            }
        }
    }
}
