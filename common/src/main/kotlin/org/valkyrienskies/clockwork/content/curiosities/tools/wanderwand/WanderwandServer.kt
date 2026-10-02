package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.primitives.AABBi
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool.ToolType
import org.valkyrienskies.clockwork.util.AABBHelper.mergeAdjacentFast
import org.valkyrienskies.clockwork.util.AABBHelper.subtractWithAABB
import org.valkyrienskies.mod.common.world.clipIncludeShips
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

object WanderwandServer {
    class Session(val stack: ItemStack) {
        var tool = ToolType.SELECT
        var first: WandAnchor? = null
        var rope: WandAnchor? = null
        var length = 0.0
        var previousAnchor: Vec3? = null
        var previousPlayer: Vec3? = null
        var lastClick = Long.MIN_VALUE
        var lastReel = Long.MIN_VALUE
    }

    private fun equipped(player: ServerPlayer) = player.isAlive && !player.isSpectator && player.mainHandItem.item is WanderwandItem
    private fun session(player: ServerPlayer): Session {
        val data = WandLinks.get(player.serverLevel())
        val old = data.sessions[player.uuid]
        return if (old != null && old.stack === player.mainHandItem) old
        else Session(player.mainHandItem).also { data.sessions[player.uuid] = it }
    }

    fun click(player: ServerPlayer, packet: WandSelectionPacket) {
        if (!equipped(player)) return
        val level = player.serverLevel()
        val state = session(player)
        if (packet.clickedFace == -2) { // Tool change: cancel the corner, retain a held rope.
            state.first = null; state.tool = packet.tool; sync(player, state); return
        }
        if (state.lastClick == level.gameTime) return
        state.lastClick = level.gameTime
        if (state.tool != packet.tool) { state.first = null; state.tool = packet.tool }
        if (packet.leftClick && state.rope != null) {
            effect(level, player.uuid, "dismiss", state.rope!!)
            state.rope = null; state.previousAnchor = null; sync(player, state); return
        }
        val selection = state.tool == ToolType.SELECT || state.tool == ToolType.DESELECT
        val range = when {
            state.tool == ToolType.BIND && state.rope == null -> 64.0
            state.tool == ToolType.BIND -> if (player.isCreative) 5.0 else 4.5
            else -> 15.0
        }
        val eye = player.eyePosition
        val hit = level.clipIncludeShips(ClipContext(eye, eye.add(player.lookAngle.scale(range)),
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player), true,
            if (state.tool == ToolType.WELD && !packet.leftClick) state.first?.shipId else null)
        val target = if (hit.type == HitResult.Type.BLOCK) WandAnchor.at(level, hit.blockPos, hit.direction)
            else if (selection) WandAnchor(BlockPos.containing(eye.add(player.lookAngle.scale(4.0))), Direction.UP)
            else null
        if (packet.leftClick) {
            if (state.first != null) {
                effect(level, player.uuid, "cancel", state.first!!); state.first = null
            } else if (selection && target != null) editSelection(player, target.pos, null, true)
            else if (state.tool == ToolType.ATTACH) {
                val data = WandLinks.get(level)
                val link = data.links.values.filter {
                    it.a.pos == target?.pos || it.b.pos == target?.pos || (!it.rope &&
                        WandLinkTargeting.intersects(eye, player.lookAngle, min(range, target?.world(level)?.distanceTo(eye) ?: range),
                            it.a.world(level), it.b.world(level)))
                }
                    .minByOrNull { min(it.a.world(level)?.distanceToSqr(eye) ?: Double.MAX_VALUE,
                        it.b.world(level)?.distanceToSqr(eye) ?: Double.MAX_VALUE) }
                if (link != null) { data.remove(level, link); breakEffect(level, link, player.uuid) }
            }
            sync(player, state); return
        }
        if (target == null) return
        when (state.tool) {
            ToolType.SELECT, ToolType.DESELECT -> {
                // Infuser selections are world blocks. Never mix shipyard and world coordinates.
                if (target.shipId >= 0) { message(player, "Select world blocks for assembly."); return }
                val first = state.first
                if (first == null) {
                    state.first = target
                    effect(level, player.uuid, if (state.tool == ToolType.SELECT) "select_start" else "deselect_start", target)
                } else {
                    if (editSelection(player, first.pos, target.pos, false)) {
                        state.first = null
                        effect(level, player.uuid, if (state.tool == ToolType.SELECT) "select_end" else "deselect_end", target)
                    }
                }
            }
            ToolType.ATTACH -> {
                if (target.shipId < 0) { message(player, "Attach needs two separate ships."); return }
                val first = state.first
                if (first == null) { state.first = target; effect(level, player.uuid, "attach_start", target) }
                else if (first.shipId != target.shipId && first.world(level) != null && !level.getBlockState(first.pos).isAir) {
                    if (WandLinks.get(level).add(level, first, target, false, 1.0) != null) {
                        effect(level, player.uuid, "attach_end", target, first); state.first = null
                    }
                } else message(player, "Choose a block on a different ship.")
            }
            ToolType.BIND -> {
                val first = state.rope
                if (first == null) {
                    state.rope = target
                    state.length = target.world(level)!!.distanceTo(player.position().add(0.0, 1.0, 0.0)).coerceIn(2.0, 64.0)
                    state.previousAnchor = target.world(level)
                    state.previousPlayer = player.position()
                    effect(level, player.uuid, "bind_start", target)
                } else if (first.pos != target.pos || first.shipId != target.shipId) {
                    val distance = first.world(level)?.distanceTo(target.world(level)!!) ?: return
                    if (distance > 128) { message(player, "The rope cannot reach that far."); return }
                    if (WandLinks.get(level).add(level, first, target, true, max(distance, state.length)) != null) {
                        state.rope = null; state.previousAnchor = null
                        effect(level, player.uuid, "bind_end", target, first)
                    }
                }
            }
            ToolType.WELD -> {
                val first = state.first
                if (first == null) {
                    if (target.shipId < 0) { message(player, "Start a weld on a ship face."); return }
                    state.first = target; effect(level, player.uuid, "weld_start", target)
                    ClockworkSounds.WAND_START.playOnServer(level, player.blockPosition(), 0.8f, 1f)
                } else {
                    val error = WanderwandWeld.weld(level, first, target)
                    if (error == null) {
                        state.first = null; effect(level, player.uuid, "weld_end", target)
                        ClockworkSounds.WAND_WELD.playOnServer(level, player.blockPosition(), 0.8f, 1f)
                    } else message(player, error)
                }
            }
        }
        sync(player, state)
        syncLinks(level)
    }

    @JvmStatic fun isGrappling(player: ServerPlayer): Boolean {
        if (!equipped(player)) return false
        return WandLinks.get(player.serverLevel()).sessions[player.uuid]?.rope != null
    }

    private fun editSelection(player: ServerPlayer, a: BlockPos, b: BlockPos?, removeRegion: Boolean): Boolean {
        val tag = player.mainHandItem.orCreateTag
        val dimension = player.level().dimension().location().toString()
        if (tag.contains("selectionDimension") && tag.getString("selectionDimension") != dimension) tag.remove("selectedBlocks")
        val existing = WandSelectionMath.normalize(WanderwandItem.readAABBSetFromNBT(tag.getCompound("selectedBlocks")))
        val result: List<AABBic>
        if (removeRegion) {
            result = existing.filterNot { WandSelectionMath.contains(it, a.x, a.y, a.z) }
        } else {
            b ?: return false
            val box = AABBi(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z), max(a.x, b.x) + 1, max(a.y, b.y) + 1, max(a.z, b.z) + 1)
            if (WandSelectionMath.volume(box) > 100000) { message(player, "Selection is limited to 100,000 blocks."); return false }
            result = if (session(player).tool == ToolType.DESELECT) mergeAdjacentFast(existing.subtractWithAABB(box))
                else WandSelectionMath.union(existing, box)
        }
        if (result.size > 512 || result.sumOf { WandSelectionMath.volume(it) } > 100000) {
            message(player, "Selection is too large or fragmented."); return false
        }
        tag.putString("selectionDimension", dimension)
        tag.put("selectedBlocks", WanderwandItem.writeAABBSetToNBT(result))
        syncSelection(player)
        return true
    }

    fun syncSelection(player: ServerPlayer) {
        val tag = player.mainHandItem.orCreateTag
        val visible = !tag.contains("selectionDimension") || tag.getString("selectionDimension") == player.level().dimension().location().toString()
        ClockworkPackets.sendTo(WanderwandRenderUpdatePacket(BlockPos.ZERO, ToolType.SELECT,
            blocks = if (visible) tag.getCompound("selectedBlocks") else CompoundTag()), player)
    }

    fun reel(player: ServerPlayer, direction: Int) {
        if (!equipped(player) || !player.isShiftKeyDown || direction == 0) return
        val state = session(player)
        if (state.rope == null || state.lastReel == player.level().gameTime) return
        state.lastReel = player.level().gameTime
        state.length = (state.length - direction * 0.75).coerceIn(2.0, 64.0)
        sync(player, state)
    }

    fun tick(level: ServerLevel) {
        val data = WandLinks.get(level)
        data.sessions.entries.removeIf { (id, s) ->
            val player = level.getPlayerByUUID(id) as? ServerPlayer
            val removed = player == null || !equipped(player) || player.mainHandItem !== s.stack
            if (removed) s.rope?.let { effect(level, id, "dismiss", it) }
            removed
        }
        for (player in level.players()) {
            if (!equipped(player)) continue
            val s = session(player)
            val rope = s.rope
            if (rope != null) {
                val anchor = rope.world(level)
                if (anchor == null || !level.hasChunkAt(rope.pos) || level.getBlockState(rope.pos).isAir ||
                    anchor.distanceToSqr(player.position()) > 160 * 160) {
                    effect(level, player.uuid, "break", rope); s.rope = null; s.previousAnchor = null; sync(player, s)
                } else {
                    val anchorVelocity = anchor.subtract(s.previousAnchor ?: anchor)
                    s.previousAnchor = anchor
                    // Movement packets carry positions, not the client's velocity. Preserve the observed
                    // tangential motion instead of repeatedly sending the server's stale deltaMovement.
                    val movement = s.previousPlayer?.let { player.position().subtract(it) } ?: player.deltaMovement
                    s.previousPlayer = player.position()
                    val result = WandRopePhysics.constrain(player.position().add(0.0, 1.0, 0.0),
                        if (movement.lengthSqr() < 100) movement else player.deltaMovement, anchor, anchorVelocity, s.length)
                    if (result != null && !player.abilities.flying && !player.isPassenger) {
                        player.deltaMovement = result
                        player.hurtMarked = true
                        player.fallDistance = 0f
                    }
                }
            }
            if (level.gameTime % 10L == 0L) sync(player, s)
        }
        if (level.gameTime % 20L == 0L) { data.tick(level); syncLinks(level) }
    }

    private fun sync(player: ServerPlayer, s: Session) {
        val tag = CompoundTag()
        tag.putString("kind", "state"); tag.putUUID("owner", player.uuid); tag.putInt("tool", s.tool.ordinal)
        s.first?.let { tag.put("first", it.save()) }; s.rope?.let { tag.put("rope", it.save()) }
        tag.putDouble("length", s.length)
        ClockworkPackets.sendToClientsTrackingAndSelf(WanderwandStatePacket(tag), player)
    }

    private fun syncLinks(level: ServerLevel) {
        val data = WandLinks.get(level)
        for (player in level.players()) {
            val list = ListTag()
            data.links.values.asSequence().filter {
                (it.a.world(level)?.distanceToSqr(player.position()) ?: Double.MAX_VALUE) < 192 * 192 ||
                    (it.b.world(level)?.distanceToSqr(player.position()) ?: Double.MAX_VALUE) < 192 * 192
            }.take(256).forEach { list.add(it.save()) }
            ClockworkPackets.sendTo(WanderwandStatePacket(CompoundTag().also { it.putString("kind", "links"); it.put("links", list) }), player)
        }
    }

    fun breakEffect(level: ServerLevel, link: WandLinks.Link, owner: UUID = UUID(0, 0)) = effect(level, owner, "break", link.a, link.b)
    private fun effect(level: ServerLevel, owner: UUID, action: String, anchor: WandAnchor, other: WandAnchor? = null) {
        val tag = CompoundTag()
        tag.putString("kind", "event"); tag.putUUID("owner", owner); tag.putString("action", action); tag.put("anchor", anchor.save())
        other?.let { tag.put("other", it.save()) }
        val world = anchor.world(level) ?: return
        ClockworkPackets.sendToNear(level, BlockPos.containing(world), 192, WanderwandStatePacket(tag))
        val sound = if (action.endsWith("start")) ClockworkSounds.WAND_START else ClockworkSounds.WAND_FINISH
        if (!action.startsWith("weld") && action != "break") sound.playOnServer(level, BlockPos.containing(world), 0.45f, if (action == "dismiss") 0.8f else 1.1f)
    }
    private fun message(player: ServerPlayer, text: String) = player.displayClientMessage(Component.literal(text), true)
}
