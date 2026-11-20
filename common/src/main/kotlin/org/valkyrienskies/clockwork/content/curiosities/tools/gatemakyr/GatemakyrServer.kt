package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.world.level.ChunkPos
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.mixin.content.gatemakyr.GateServerPositionAccessor
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID
import java.util.WeakHashMap
import kotlin.math.ceil

object GatemakyrServer {
    private const val MIN_HOLD_TICKS = 20

    private data class Charge(val side: Int, val startedAt: Long, val stack: ItemStack, val slot: Int, var heartbeat: Long)
    private class State(level: ServerLevel) {
        val traversal = GatemakyrTraversal()
        val players = GatemakyrPlayerValidator()
        val charges = mutableMapOf<UUID, Charge>()
        val shots = mutableListOf<GatemakyrEffectPacket>()
        val rifts = linkedMapOf<Pair<UUID, Int>, GatemakyrEffectPacket>()
        val saved = GatemakyrPairs.get(level)
        val knownCarriers = mutableSetOf<Long>()
        val physical = mutableSetOf<UUID>()
        val opening = mutableMapOf<UUID, Long>()

        init {
            for ((owner, pair) in saved.pairs) {
                for ((side, mouth) in listOf(pair.first, pair.second).withIndex())
                    rifts[owner to side] = GatemakyrEffectPacket(level.dimension().location(), owner,
                        GatemakyrEffectPacket.Kind.RIFT, side, mouth.startedAt, target = mouth.target)
                opening[owner] = maxOf(pair.first.readyAt(), pair.second.readyAt())
            }
        }
    }
    private val levels = WeakHashMap<ServerLevel, State>()

    fun clear() {
        GatemakyrChunkWatch.clear()
        levels.clear()
    }

    internal fun playerGates(level: ServerLevel) = levels[level]?.players?.gates.orEmpty()

    internal fun physicalGates(level: ServerLevel): Collection<GatemakyrPlayerGate> {
        val state = levels[level] ?: return emptyList()
        if (state.physical.isEmpty()) return emptyList()
        return state.players.gates.values.filter { it.packet.owner in state.physical }
    }

    @JvmStatic fun observeMove(player: ServerPlayer, from: Vec3) {
        levels[player.serverLevel()]?.players?.observeMove(player, from)
    }

    @JvmStatic fun forgetMove(player: ServerPlayer) {
        levels[player.serverLevel()]?.players?.forget(player)
    }

    fun cross(player: ServerPlayer, request: GatemakyrCrossingPacket) {
        if ((player.connection as GateServerPositionAccessor).awaitingGateCorrection() != null) return
        val state = levels[player.serverLevel()]
        if (state == null || !state.players.accept(player, request, state.rifts))
            player.connection.teleport(player.x, player.y, player.z, player.yRot, player.xRot)
    }

    fun input(player: ServerPlayer, packet: GatemakyrInputPacket) {
        val level = player.serverLevel()
        val state = levels.getOrPut(level) { State(level) }
        val now = level.gameTime
        if (packet.action == GatemakyrInputPacket.Action.CANCEL) {
            if (state.charges.remove(player.uuid) != null) cancel(level, player.uuid)
            return
        }
        val stack = player.mainHandItem
        if (stack.item !is GatemakyrItem || !player.isAlive || player.isSpectator || packet.side !in 0..1 ||
            !packet.distance.isFinite() || packet.distance !in 2.0..48.0 || packet.width !in 1..32 || packet.height !in 1..32) return
        stack.orCreateTag.putBoolean(GatemakyrItem.FREE, packet.free)
        stack.orCreateTag.putDouble(GatemakyrItem.DISTANCE, packet.distance)
        stack.orCreateTag.putInt(GatemakyrItem.WIDTH, packet.width)
        stack.orCreateTag.putInt(GatemakyrItem.HEIGHT, packet.height)
        when (packet.action) {
            GatemakyrInputPacket.Action.CLEAR -> {
                for (world in level.server.allLevels) {
                    val current = levels.getOrPut(world) { State(world) }
                    forgetPair(world, current, player.uuid)
                    current.charges.remove(player.uuid)
                    current.shots.removeAll { it.owner == player.uuid }
                    current.rifts.entries.removeIf { (key, rift) ->
                        if (key.first == player.uuid) { close(world, rift); true } else false
                    }
                    val clear = GatemakyrEffectPacket(world.dimension().location(), player.uuid,
                        GatemakyrEffectPacket.Kind.CLEAR, 0, world.gameTime)
                    world.players().forEach { ClockworkPackets.sendTo(clear, it) }
                }
            }
            GatemakyrInputPacket.Action.CHARGE -> {
                val previous = state.charges[player.uuid]
                if (previous != null) {
                    if (previous.side == packet.side && previous.stack === stack && previous.slot == player.inventory.selected)
                        previous.heartbeat = now
                    return
                }
                state.charges[player.uuid] = Charge(packet.side, now, stack, player.inventory.selected, now)
                ClockworkPackets.sendToClientsTrackingAndSelf(GatemakyrEffectPacket(level.dimension().location(), player.uuid,
                    GatemakyrEffectPacket.Kind.CHARGE, packet.side, now), player)
            }
            GatemakyrInputPacket.Action.RELEASE -> {
                val charge = state.charges.remove(player.uuid) ?: return
                var ray: GatemakyrRayPath? = null
                val target = if (charge.side == packet.side && charge.stack === stack && charge.slot == player.inventory.selected &&
                    now - charge.startedAt >= MIN_HOLD_TICKS && now - charge.heartbeat <= 30)
                    GatemakyrTarget.acquire(player, packet.free, packet.distance, packet.width, packet.height,
                        state.rifts[player.uuid to (1 - packet.side)]?.target) { ray = it } else null
                if (target == null || state.shots.size >= 128) { cancel(level, player.uuid); return }
                val end = target.frame(level)?.impact ?: run { cancel(level, player.uuid); return }
                val origin = player.eyePosition.add(player.lookAngle.scale(0.7)).add(0.0, -0.2, 0.0)
                val duration = ceil((ray?.distance ?: origin.distanceTo(end)) / 1.4).toInt().coerceIn(3, 40)
                val shot = GatemakyrEffectPacket(level.dimension().location(), player.uuid, GatemakyrEffectPacket.Kind.SHOT,
                    charge.side, now, duration, origin, target, crossings = ray?.crossings.orEmpty())
                state.shots.add(shot)
                broadcast(level, shot, end)
            }
            else -> Unit
        }
    }

    private fun cancel(level: ServerLevel, owner: UUID) {
        val packet = GatemakyrEffectPacket(level.dimension().location(), owner, GatemakyrEffectPacket.Kind.CANCEL, 0, level.gameTime)
        level.players().forEach { ClockworkPackets.sendTo(packet, it) }
    }

    fun tick(level: ServerLevel) {
        val state = levels.getOrPut(level) { State(level) }
        GatemakyrChunkWatch.tick(level, state.saved.pairs.values)
        if (state.rifts.isEmpty() && state.shots.isEmpty() && state.charges.isEmpty() && state.players.gates.isEmpty()) return
        val now = level.gameTime
        state.charges.entries.removeIf { (id, charge) ->
            val player = level.getPlayerByUUID(id)
            val invalid = player == null || !player.isAlive || player.isSpectator || player.mainHandItem !== charge.stack ||
                player.inventory.selected != charge.slot || now - charge.heartbeat > 30
            if (invalid) cancel(level, id)
            else if (now % 10L == 0L) ClockworkPackets.sendToClientsTrackingAndSelf(GatemakyrEffectPacket(level.dimension().location(),
                id, GatemakyrEffectPacket.Kind.CHARGE, charge.side, charge.startedAt), player as ServerPlayer)
            invalid
        }
        val iterator = state.shots.iterator()
        while (iterator.hasNext()) {
            val shot = iterator.next()
            val planned = shot.target ?: continue
            val end = planned.frame(level)?.impact
            if (end == null) {
                iterator.remove()
                continue
            }
            var blocked = false
            var completedHops = 0
            var impact: GatemakyrTarget? = null
            val progress = (now - shot.startedAt).toDouble() / shot.duration
            shot.flightSegments(end, progress - 1.0 / shot.duration, progress) { leg, a, b ->
                completedHops = leg
                val hit = GatemakyrRaycast.flightHit(level, a, b)
                blocked = hit.type == HitResult.Type.BLOCK
                if (blocked) impact = GatemakyrTarget.surface(level, hit, planned.width, planned.height)
                !blocked
            }
            if (!blocked && progress < 1.0) continue
            iterator.remove()
            if (blocked) {
                val update = GatemakyrEffectPacket(shot.dimension, shot.owner, GatemakyrEffectPacket.Kind.IMPACT, shot.side,
                    shot.startedAt, (now - shot.startedAt).toInt().coerceAtLeast(1), shot.origin, impact, crossings = shot.crossings.take(completedHops))
                level.players().forEach { ClockworkPackets.sendTo(update, it) }
                if (impact == null) continue
            }
            val partnerKey = shot.owner to (1 - shot.side)
            val other = state.rifts[partnerKey]
            val target = (impact ?: planned).fitTo(other?.target)
            if (!target.canPlace(level)) continue
            if (other != null) {
                val resized = other.target!!.fitTo(target)
                state.rifts[partnerKey] = GatemakyrEffectPacket(other.dimension, other.owner, other.kind, other.side, other.startedAt, target = resized)
            }
            val rift = GatemakyrEffectPacket(level.dimension().location(), shot.owner, GatemakyrEffectPacket.Kind.RIFT, shot.side,
                now, target = target)
            state.rifts.put(shot.owner to shot.side, rift)?.let { close(level, it) }
            while (state.rifts.size > 128) state.rifts.remove(state.rifts.keys.first())?.let {
                close(level, it)
                forgetPair(level, state, it.owner)
            }
            updatePair(level, state, shot.owner)
            syncRift(level, state, rift)
        }
        state.opening.entries.removeIf { (owner, ready) ->
            if (now < ready) false else {
                state.saved.pairs[owner]?.let {
                    if (GatemakyrShipPortals.set(level, owner, it.first.target, it.second.target)) state.physical.add(owner)
                }
                true
            }
        }
        state.players.tick(level, state.rifts)
        state.traversal.tick(level, state.players.gates)
        if (now % 20L != 0L) return
        val carriers = mutableSetOf<Long>()
        state.rifts.entries.removeIf { (_, rift) ->
            val target = rift.target!!
            val invalid = if (target.shipId >= 0) {
                val ship = level.shipObjectWorld.allShips.getById(target.shipId)
                if (ship != null) carriers.add(target.shipId)
                if (ship == null) target.shipId in state.knownCarriers
                else level.shipObjectWorld.loadedShips.getById(target.shipId) != null &&
                    target.hasBacking(level) == false
            } else if (target.surface) target.hasBacking(level) == false
            else rift.owner !in state.saved.pairs && !target.available(level)
            if (invalid) {
                close(level, rift)
                forgetPair(level, state, rift.owner)
            }
            invalid
        }
        state.knownCarriers.clear()
        state.knownCarriers.addAll(carriers)
        for (rift in state.rifts.values) {
            if (rift.side == 0 || (rift.owner to 0) !in state.rifts) syncRift(level, state, rift)
        }
    }

    private fun forgetPair(level: ServerLevel, state: State, owner: UUID) {
        state.physical.remove(owner)
        state.opening.remove(owner)
        if (state.saved.pairs.remove(owner) != null) state.saved.setDirty()
        GatemakyrShipPortals.remove(level, owner)
    }

    private fun updatePair(level: ServerLevel, state: State, owner: UUID) {
        val a = state.rifts[owner to 0]
        val b = state.rifts[owner to 1]
        val first = a?.target
        val second = b?.target
        if (first == null || second == null || first.width != second.width || first.height != second.height) {
            forgetPair(level, state, owner)
            return
        }
        state.physical.remove(owner)
        if (owner in state.saved.pairs) GatemakyrShipPortals.remove(level, owner)
        val pair = GatemakyrPairs.Mouth(first, a.startedAt) to GatemakyrPairs.Mouth(second, b.startedAt)
        state.saved.pairs[owner] = pair
        state.saved.setDirty()
        state.opening[owner] = maxOf(pair.first.readyAt(), pair.second.readyAt())
    }

    private fun syncRift(level: ServerLevel, state: State, rift: GatemakyrEffectPacket) {
        val other = state.rifts[rift.owner to (1 - rift.side)]
        val position = rift.target!!.frame(level)?.position
        val otherPosition = other?.target?.frame(level)?.position
        val snapshot = GatemakyrEffectPacket(rift.dimension, rift.owner, rift.kind, rift.side, rift.startedAt,
            target = rift.target, partner = other?.target, partnerStartedAt = other?.startedAt ?: 0)
        for (player in level.players()) {
            if (player.uuid == rift.owner || listOfNotNull(position, otherPosition).any {
                    GatemakyrChunkWatch.watches(player, ChunkPos(BlockPos.containing(it)))
                } || position != null && player.position().distanceToSqr(position) < 128.0 * 128.0 ||
                otherPosition != null && player.position().distanceToSqr(otherPosition) < 128.0 * 128.0)
                ClockworkPackets.sendTo(snapshot, player)
        }
    }

    private fun close(level: ServerLevel, rift: GatemakyrEffectPacket) {
        val packet = GatemakyrEffectPacket(level.dimension().location(), rift.owner, GatemakyrEffectPacket.Kind.CLOSE,
            rift.side, level.gameTime, target = rift.target)
        level.players().forEach { ClockworkPackets.sendTo(packet, it) }
    }

    private fun broadcast(level: ServerLevel, packet: GatemakyrEffectPacket, position: Vec3) {
        for (player in level.players()) if (player.uuid == packet.owner || player.position().distanceToSqr(position) < 128.0 * 128.0 ||
            packet.kind == GatemakyrEffectPacket.Kind.SHOT && (player.position().distanceToSqr(packet.origin) < 128.0 * 128.0 ||
                packet.crossings.any { (a, b) -> minOf(player.position().distanceToSqr(a), player.position().distanceToSqr(b)) < 128.0 * 128.0 }))
            ClockworkPackets.sendTo(packet, player)
    }
}
