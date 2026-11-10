package org.valkyrienskies.clockwork.content.curiosities.rope

import org.valkyrienskies.clockwork.content.curiosities.spring.SpringItem

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.ItemTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkEntities
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandServer
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandStatePacket
import org.valkyrienskies.mod.common.world.clipIncludeShips
import java.util.UUID

object RopeKnotServer {
    fun tie(player: ServerPlayer, stack: ItemStack, anchor: WandAnchor) {
        val level = player.serverLevel()
        if (!player.mayBuild() || player.isSpectator || stack.item !is RopeKnotItem || stack.isEmpty ||
            !valid(level, anchor) || !level.mayInteract(player, anchor.pos)) return
        val end = anchor.world(level) ?: return
        val spring = stack.item is SpringItem
        val first = RopeKnotItem.start(stack, level)
        if (first == null) {
            RopeKnotItem.setStart(stack, level, anchor)
            (if (spring) ClockworkSounds.SPRING_ATTACH_START else ClockworkSounds.ROPE_TIE).play(level, null, end, 0.75f, 1f)
            return
        }
        if (!valid(level, first)) {
            RopeKnotItem.clearStart(stack)
            message(player, "missing", spring)
            return
        }
        val start = first.world(level) ?: return
        if (start.distanceTo(end) > 128.0) { message(player, "long", spring); return }
        if (!level.mayInteract(player, first.pos)) return
        val link = WandLinks.get(level).add(level, first, anchor, true,
            (if (spring) start.distanceTo(end).coerceAtLeast(0.5) else RopeKnotPhysics.placedLength(start.distanceTo(end))), physical = true, spring = spring)
        if (link == null) { message(player, "duplicate", spring); return }
        RopeKnotItem.clearStart(stack)
        if (!player.abilities.instabuild) stack.shrink(1)
        (if (spring) ClockworkSounds.SPRING_ATTACH_END else ClockworkSounds.ROPE_TIE).play(level, null, end, 1f, 1f)
        WanderwandServer.syncLinks(level)
    }

    private fun valid(level: ServerLevel, anchor: WandAnchor) = level.hasChunkAt(anchor.pos) &&
        !level.getBlockState(anchor.pos).isAir && anchor.world(level) != null

    fun ensureKnots(level: ServerLevel, link: WandLinks.Link) {
        fun ensure(anchor: WandAnchor, previous: UUID?, end: Int): UUID? {
            if (previous != null && level.getEntity(previous)?.isRemoved == false) return previous
            val point = anchor.world(level) ?: return null
            if (!level.hasChunkAt(BlockPos.containing(point))) return null
            val knot = ClockworkEntities.ROPE_KNOT.create(level) ?: return null
            knot.spring = link.spring
            knot.linkId = link.id
            knot.end = end
            knot.setPos(point.x, point.y - knot.bbHeight / 2, point.z)
            return if (level.addFreshEntity(knot)) knot.uuid else null
        }
        link.knotA = ensure(link.a, link.knotA, 0)
        link.knotB = ensure(link.b, link.knotB, 1)
    }

    fun hit(player: ServerPlayer, knot: RopeKnotEntity): Boolean {
        val level = player.serverLevel()
        if (!player.mayBuild() || player.isSpectator || !player.isAlive || knot.isRemoved) return false
        val data = WandLinks.get(level)
        val link = knot.linkId?.let { data.links[it] }?.takeIf { it.physical } ?: return false
        val anchor = if (knot.end == 0) link.a else link.b
        val point = anchor.world(level) ?: return false
        if (!level.mayInteract(player, anchor.pos) || point.distanceToSqr(player.eyePosition) > 36) return false
        val hit = level.clipIncludeShips(ClipContext(player.eyePosition, point,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player), true)
        if (hit.type != HitResult.Type.MISS && hit.blockPos != anchor.pos &&
            hit.location.distanceTo(player.eyePosition) + 0.3 < point.distanceTo(player.eyePosition)) return false
        val stack = player.mainHandItem
        val cutting = stack.`is`(ItemTags.SWORDS) || stack.`is`(ItemTags.AXES)
        if (!cutting && !stack.isEmpty && stack.item !is RopeKnotItem) return false
        if (!cutting && link.lastHit != Long.MIN_VALUE && level.gameTime - link.lastHit < RopeKnotPhysics.HIT_INTERVAL) return false
        link.lastHit = level.gameTime
        link.hits++
        if (!RopeKnotPhysics.breaksOnHit(link.hits, cutting)) {
            (if (link.spring) ClockworkSounds.SPRING_ATTACH_START else ClockworkSounds.ROPE_TIE).play(level, null, point, 0.4f, 0.8f + link.hits * 0.12f)
            return true
        }
        data.remove(level, link)
        if (cutting) stack.hurtAndBreak(1, player) { it.broadcastBreakEvent(InteractionHand.MAIN_HAND) }
        else if (!player.abilities.instabuild) {
            val drop = ItemEntity(level, point.x, point.y, point.z, ItemStack(if (link.spring) ClockworkItems.SPRING.get() else ClockworkItems.ROPE_KNOT.get()))
            drop.setDefaultPickUpDelay()
            level.addFreshEntity(drop)
        }
        breakEffect(level, link, !cutting, point)
        WanderwandServer.syncLinks(level)
        return true
    }

    fun breakEffect(level: ServerLevel, link: WandLinks.Link, untied: Boolean = false, soundPosition: Vec3? = null) {
        val a = link.a.world(level)
        val b = link.b.world(level)
        val point = soundPosition ?: a ?: b ?: return
        if (untied) (if (link.spring) ClockworkSounds.SPRING_ATTACH_END else ClockworkSounds.ROPE_TIE).play(level, null, point, 0.85f, 0.8f)
        else (if (link.spring) ClockworkSounds.SPRING_SNAP else ClockworkSounds.ROPE_SNAP).play(level, null, point, 1f, 0.9f + level.random.nextFloat() * 0.2f)
        if (soundPosition == null && b != null && b.distanceToSqr(point) > 16 * 16)
            (if (link.spring) ClockworkSounds.SPRING_SNAP else ClockworkSounds.ROPE_SNAP).play(level, null, b, 1f, 0.9f + level.random.nextFloat() * 0.2f)
        if (a == null || b == null) return
        val tag = CompoundTag().also {
            it.putString("kind", "physical_rope_break"); it.putUUID("id", link.id)
            it.putDouble("ax", a.x); it.putDouble("ay", a.y); it.putDouble("az", a.z)
            it.putDouble("bx", b.x); it.putDouble("by", b.y); it.putDouble("bz", b.z)
            if (link.spring) { it.put("a", link.a.save()); it.put("b", link.b.save()) }
            it.putDouble("length", if (link.spring) link.appliedLength else link.length)
            it.putBoolean("untied", untied); it.putBoolean("spring", link.spring)
        }
        for (player in level.players()) if (minOf(a.distanceToSqr(player.position()), b.distanceToSqr(player.position())) < 192 * 192)
            ClockworkPackets.sendTo(WanderwandStatePacket(tag), player)
    }

    private fun message(player: ServerPlayer, key: String, spring: Boolean = false) =
        player.displayClientMessage(Component.translatable("message.vs_clockwork.${if (spring) "spring" else "rope_knot"}.$key"), true)
}
