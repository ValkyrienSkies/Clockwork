package org.valkyrienskies.clockwork.forge.gametest

import com.mojang.authlib.GameProfile
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.Connection
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotEntity
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotItem
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotServer
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import java.util.UUID

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class RopeKnotGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"

        private fun player(helper: GameTestHelper): ServerPlayer {
            val player = ServerPlayer(helper.level.server, helper.level, GameProfile(UUID.randomUUID(), "rope-test"))
            val connection = object : Connection(PacketFlow.SERVERBOUND) {
                override fun send(packet: Packet<*>, listener: PacketSendListener?) { listener?.onSuccess() }
            }
            player.connection = ServerGamePacketListenerImpl(helper.level.server, connection, player)
            val pos = helper.absolutePos(BlockPos(2, 2, 0))
            player.setPos(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
            return player
        }

        private fun place(helper: GameTestHelper, player: ServerPlayer): WandLinks.Link {
            val a = helper.absolutePos(BlockPos(2, 2, 2))
            val b = helper.absolutePos(BlockPos(5, 2, 2))
            helper.level.setBlockAndUpdate(a, Blocks.STONE.defaultBlockState())
            helper.level.setBlockAndUpdate(b, Blocks.STONE.defaultBlockState())
            val stack = ItemStack(ClockworkItems.ROPE_KNOT.get(), 16)
            player.setItemInHand(InteractionHand.MAIN_HAND, stack)
            RopeKnotServer.tie(player, stack, WandAnchor(a, Direction.NORTH))
            helper.assertTrue(stack.count == 16 && RopeKnotItem.start(stack, helper.level) != null, "Starting consumed a knot")
            RopeKnotServer.tie(player, stack, WandAnchor(b, Direction.NORTH))
            helper.assertTrue(stack.count == 15 && RopeKnotItem.start(stack, helper.level) == null, "Completing must consume exactly one knot and clear placement")
            val link = WandLinks.get(helper.level).links.values.single { it.a.pos == a && it.b.pos == b }
            helper.assertTrue(link.physical && link.rope && link.jointId == null, "World/world rope needs no physics joint")
            helper.assertTrue(helper.level.getEntity(link.knotA!!) is RopeKnotEntity && helper.level.getEntity(link.knotB!!) is RopeKnotEntity,
                "Both knots must be targetable")
            return link
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = "rope_knots", timeoutTicks = 50)
        fun threeHitsRefundOneRope(helper: GameTestHelper) {
            val player = player(helper)
            val link = place(helper, player)
            val knot = helper.level.getEntity(link.knotA!!) as RopeKnotEntity
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)
            val data = WandLinks.get(helper.level)
            helper.startSequence()
                .thenExecute { player.attack(knot); helper.assertTrue(data.links.containsKey(link.id), "First hit broke rope") }
                .thenIdle(6)
                .thenExecute { player.attack(knot); helper.assertTrue(data.links.containsKey(link.id), "Second hit broke rope") }
                .thenIdle(6)
                .thenExecute {
                    player.attack(knot)
                    helper.assertTrue(!data.links.containsKey(link.id), "Third hit did not untie rope")
                    helper.assertTrue(knot.isRemoved && helper.level.getEntity(link.knotB!!) == null, "Knot hitboxes survived removal")
                    val drops = helper.level.getEntitiesOfClass(ItemEntity::class.java, AABB(link.a.pos).inflate(2.0))
                        .filter { it.item.`is`(ClockworkItems.ROPE_KNOT.get()) }.sumOf { it.item.count }
                    helper.assertTrue(drops == 1, "Untying must refund one rope, got $drops")
                }.thenSucceed()
        }

        @JvmStatic
        @GameTest(template = TEMPLATE, batch = "rope_knots")
        fun cuttingDestroysRopeAndMissingHitboxesRebuild(helper: GameTestHelper) {
            val player = player(helper)
            val link = place(helper, player)
            helper.level.getEntity(link.knotA!!)!!.discard()
            val old = link.knotA
            RopeKnotServer.ensureKnots(helper.level, link)
            helper.assertTrue(old != link.knotA, "Unloaded or removed knot was not rebuilt")
            val knot = helper.level.getEntity(link.knotA!!) as RopeKnotEntity
            player.attack(knot)
            helper.assertTrue(link.hits == 1, "Attacking with a knot should begin untying")
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.IRON_SWORD))
            player.attack(knot)
            helper.assertTrue(!WandLinks.get(helper.level).links.containsKey(link.id), "Sword must cut in one hit")
            helper.assertTrue(helper.level.getEntitiesOfClass(ItemEntity::class.java, AABB(link.a.pos).inflate(2.0))
                .none { it.item.`is`(ClockworkItems.ROPE_KNOT.get()) }, "Cut rope should not refund an item")
            helper.succeed()
        }
    }
}
