package org.valkyrienskies.clockwork.content.curiosities.rope

import org.valkyrienskies.clockwork.content.curiosities.spring.SpringItem
import org.valkyrienskies.clockwork.content.curiosities.strut.StrutItem

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
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
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableItem
import org.valkyrienskies.mod.common.world.clipIncludeShips
import java.util.UUID

object RopeKnotServer {
    fun tie(player: ServerPlayer, stack: ItemStack, anchor: WandAnchor) {
        val level = player.serverLevel()
        if (!player.mayBuild() || player.isSpectator || stack.item !is RopeKnotItem || stack.isEmpty ||
            !valid(level, anchor) || !level.mayInteract(player, anchor.pos)) return
        val strut = stack.item is StrutItem
        if (strut && anchor.winch) { tie(player, stack, anchor.copy(winch = false)); return }
        val candidate = if (!strut && stack.item !is SpringItem && !anchor.winch &&
            level.getBlockEntity(anchor.pos) is RopeWinchBlockEntity &&
            WandLinks.get(level).links.values.none { (it.a.winch && it.a.pos == anchor.pos) || (it.b.winch && it.b.pos == anchor.pos) })
            anchor.copy(face = Direction.DOWN, winch = true) else anchor
        if (candidate != anchor) { tie(player, stack, candidate); return }
        val end = anchor.world(level) ?: return
        val spring = stack.item is SpringItem
        val first = RopeKnotItem.start(stack, level)
        if (first == null) {
            RopeKnotItem.setStart(stack, level, anchor)
            attachSound(strut, spring).play(level, null, end, 0.75f, 1f)
            return
        }
        if (!valid(level, first)) {
            RopeKnotItem.clearStart(stack)
            message(player, "missing", stack)
            return
        }
        val start = first.world(level) ?: return
        val distance = start.distanceTo(end)
        if (distance > 128.0) { message(player, "long", stack); return }
        if (!level.mayInteract(player, first.pos)) return
        if (strut && distance < 0.5) { message(player, "short", stack); return }
        val length = if (spring || strut) distance.coerceAtLeast(0.5) else RopeKnotPhysics.placedLength(distance)
        val link = WandLinks.get(level).add(level, first, anchor, !strut, length, physical = true, spring = spring, strut = strut)
        if (link == null) { message(player, "duplicate", stack); return }
        RopeKnotItem.clearStart(stack)
        if (!player.abilities.instabuild) stack.shrink(1)
        attachSound(strut, spring, finished = true).play(level, null, end, 1f, 1f)
        WanderwandServer.syncLinks(level)
    }

    private fun attachSound(strut: Boolean, spring: Boolean, thinkable: Boolean = false, finished: Boolean = false) = when {
        strut -> ClockworkSounds.STRUT_ATTACH
        thinkable -> ClockworkSounds.THINKABLE_ATTACH
        spring && finished -> ClockworkSounds.SPRING_ATTACH_END
        spring -> ClockworkSounds.SPRING_ATTACH_START
        else -> ClockworkSounds.ROPE_TIE
    }

    private fun valid(level: ServerLevel, anchor: WandAnchor) = anchor.entityId == null && level.hasChunkAt(anchor.pos) &&
        !level.getBlockState(anchor.pos).isAir && anchor.world(level) != null

    fun ensureKnots(level: ServerLevel, link: WandLinks.Link) {
        fun ensure(anchor: WandAnchor, previous: UUID?, end: Int): UUID? {
            if (previous != null && level.getEntity(previous)?.isRemoved == false) return previous
            val point = anchor.world(level) ?: return null
            if (!level.hasChunkAt(BlockPos.containing(point))) return null
            val knot = ClockworkEntities.ROPE_KNOT.create(level) ?: return null
            knot.spring = link.spring; knot.winch = anchor.winch
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
        val interactionPos = if (anchor.entityId == null) anchor.pos else BlockPos.containing(point)
        if (!level.mayInteract(player, interactionPos) || point.distanceToSqr(player.eyePosition) > 36) return false
        val hit = level.clipIncludeShips(ClipContext(player.eyePosition, point,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player), true)
        if (hit.type != HitResult.Type.MISS && (anchor.entityId != null || hit.blockPos != anchor.pos) &&
            hit.location.distanceTo(player.eyePosition) + 0.3 < point.distanceTo(player.eyePosition)) return false
        val stack = player.mainHandItem
        val cutting = stack.`is`(ItemTags.SWORDS) || stack.`is`(ItemTags.AXES)
        if (!cutting && !stack.isEmpty && stack.item !is RopeKnotItem && stack.item !is ThinkableItem) return false
        if (!cutting && link.lastHit != Long.MIN_VALUE && level.gameTime - link.lastHit < RopeKnotPhysics.HIT_INTERVAL) return false
        link.lastHit = level.gameTime
        link.hits++
        if (!RopeKnotPhysics.breaksOnHit(link.hits, cutting)) {
            attachSound(link.strut, link.spring, link.thinkable)
                .play(level, null, point, 0.4f, 0.8f + link.hits * 0.12f)
            return true
        }
        data.remove(level, link)
        if (cutting) stack.hurtAndBreak(1, player) { it.broadcastBreakEvent(InteractionHand.MAIN_HAND) }
        else if (!player.abilities.instabuild && !link.thinkable) {
            val item = when {
                link.strut -> ClockworkItems.STRUT
                link.spring -> ClockworkItems.SPRING
                else -> ClockworkItems.ROPE_KNOT
            }
            val drop = ItemEntity(level, point.x, point.y, point.z, ItemStack(item.get()))
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
        val snapSound = when {
            link.strut -> ClockworkSounds.STRUT_BREAK
            link.thinkable -> ClockworkSounds.THINKABLE_SNAP
            link.spring -> ClockworkSounds.SPRING_SNAP
            else -> ClockworkSounds.ROPE_SNAP
        }
        if (untied) attachSound(link.strut, link.spring, link.thinkable, finished = true)
            .play(level, null, point, 0.85f, 0.8f)
        else snapSound.play(level, null, point, 1f, 0.9f + level.random.nextFloat() * 0.2f)
        if (soundPosition == null && b != null && b.distanceToSqr(point) > 16 * 16)
            snapSound.play(level, null, b, 1f, 0.9f + level.random.nextFloat() * 0.2f)
        if (a == null || b == null || link.strut) return
        val tag = CompoundTag().also {
            it.putString("kind", "physical_rope_break"); it.putUUID("id", link.id)
            it.putDouble("ax", a.x); it.putDouble("ay", a.y); it.putDouble("az", a.z)
            it.putDouble("bx", b.x); it.putDouble("by", b.y); it.putDouble("bz", b.z)
            if (link.spring || link.thinkable) { it.put("a", link.a.save()); it.put("b", link.b.save()) }
            it.putDouble("length", if (link.spring) link.appliedLength else link.length)
            it.putBoolean("untied", untied); it.putBoolean("spring", link.spring)
            it.putBoolean("thinkable", link.thinkable)
            if (link.hose) it.putBoolean("hose", true)
        }
        for (player in level.players()) if (minOf(a.distanceToSqr(player.position()), b.distanceToSqr(player.position())) < 192 * 192)
            ClockworkPackets.sendTo(WanderwandStatePacket(tag), player)
    }

    private fun message(player: ServerPlayer, key: String, stack: ItemStack) =
        player.displayClientMessage(Component.translatable("message.vs_clockwork.${stack.descriptionId.substringAfterLast('.')}.$key"), true)
}
