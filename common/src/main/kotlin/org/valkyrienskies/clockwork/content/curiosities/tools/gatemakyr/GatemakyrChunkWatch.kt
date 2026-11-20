package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.TicketType
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.mutable.MutableObject
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.mod.common.isChunkInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.getShipsIntersecting
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.mixin.accessors.server.level.ChunkMapAccessor
import org.valkyrienskies.mod.mixinducks.world.entity.PlayerDuck
import java.util.WeakHashMap
import org.joml.Vector3d

object GatemakyrChunkWatch {
    private const val RADIUS = 2
    private val offsets = (-RADIUS..RADIUS).flatMap { x -> (-RADIUS..RADIUS).map { z -> ChunkPos(x, z) } }
        .sortedBy { it.x * it.x + it.z * it.z }
    private val ticket = TicketType.create("clockwork_gate", Comparator.naturalOrder<Long>(), 40)
    private class Watch(val level: ServerLevel) {
        var chunks = emptySet<Long>()
        val sent = mutableSetOf<Long>()
        val ships = mutableSetOf<Long>()
    }
    private val watches = WeakHashMap<ServerPlayer, Watch>()

    @JvmStatic fun watches(player: ServerPlayer, chunk: ChunkPos) =
        watches[player]?.let { it.level === player.level() && chunk.toLong() in it.sent } == true

    private fun position(level: ServerLevel, target: GatemakyrTarget): Vec3? {
        val resolved = target.frame(level)?.position ?: run {
            if (target.shipId < 0) return null
            val ship = level.shipObjectWorld.allShips.getById(target.shipId) ?: return null
            ship.shipToWorld.transformPosition(Vector3d(target.position.x, target.position.y, target.position.z)).toMinecraft()
        }
        val chunk = ChunkPos(BlockPos.containing(resolved))
        if (level.isChunkInShipyard(chunk.x, chunk.z) || !level.worldBorder.isWithinBounds(BlockPos.containing(resolved))) return null
        return resolved
    }

    private fun release(player: ServerPlayer, watch: Watch) {
        (player as PlayerDuck).vs_getPlayer().forceWatchingShips.removeAll(watch.ships)
    }

    fun clear() {
        watches.forEach(::release)
        watches.clear()
    }

    internal fun tick(level: ServerLevel, pairs: Collection<Pair<GatemakyrPairs.Mouth, GatemakyrPairs.Mouth>>) {
        if (level.gameTime % 5 != 0L) return
        watches.entries.removeIf { (p, w) ->
            (w.level === level && (p.isRemoved || p.level() !== level)).also { if (it) release(p, w) }
        }
        if (pairs.isEmpty() && watches.isEmpty()) return
        val mouths = pairs.flatMap { (a, b) ->
            val first = position(level, a.target)
            val second = position(level, b.target)
            if (first == null || second == null) emptyList() else listOf(first to second, second to first)
        }
        val refreshed = mutableSetOf<Long>()
        for (player in level.players()) {
            val exits = linkedSetOf<ChunkPos>()
            val observers = ArrayDeque<Pair<Vec3, Double>>()
            observers.add(player.position() to 96.0)
            while (observers.isNotEmpty() && exits.size < 8) {
                val (eye, range) = observers.removeFirst()
                for ((_, destination) in mouths.filter { it.first.distanceToSqr(eye) < range * range }
                    .sortedBy { it.first.distanceToSqr(eye) }) {
                    if (exits.add(ChunkPos(BlockPos.containing(destination)))) observers.add(destination to 32.0)
                    if (exits.size == 8) break
                }
            }
            val old = watches[player]
            if (old == null && exits.isEmpty()) continue
            if (old != null && old.level !== level) release(player, old)
            val watch = old?.takeIf { it.level === level } ?: Watch(level).also { watches[player] = it }
            val chunks = mutableSetOf<Long>()
            val ships = mutableSetOf<Long>()
            for (exit in exits) {
                val minX = exit.x - RADIUS
                val minZ = exit.z - RADIUS
                val maxX = exit.x + RADIUS
                val maxZ = exit.z + RADIUS
                if (listOf(minX - 12, maxX + 12).any { x -> listOf(minZ - 12, maxZ + 12).any { z -> level.isChunkInShipyard(x, z) } }) continue
                for (offset in offsets) {
                    val x = exit.x + offset.x
                    val z = exit.z + offset.z
                    if (level.isChunkInShipyard(x, z)) continue
                    val pos = ChunkPos(x, z)
                    if (!level.worldBorder.isWithinBounds(pos)) continue
                    val key = pos.toLong()
                    if (refreshed.add(key)) level.chunkSource.addRegionTicket(ticket, pos, 2, key)
                    chunks.add(key)
                }
                val bounds = AABB((minX * 16).toDouble(), level.minBuildHeight.toDouble(), (minZ * 16).toDouble(),
                    ((maxX + 1) * 16).toDouble(), level.maxBuildHeight.toDouble(), ((maxZ + 1) * 16).toDouble())
                level.getShipsIntersecting(bounds).forEach { ships.add(it.id) }
            }
            val forced = (player as PlayerDuck).vs_getPlayer().forceWatchingShips
            watch.ships.removeIf { id -> (id !in ships).also { if (it) forced.remove(id) } }
            for (id in ships) if (forced.add(id)) watch.ships.add(id)
            if (chunks != watch.chunks) {
                watch.chunks = chunks
                watch.sent.retainAll(chunks)
                ClockworkPackets.sendTo(GatemakyrChunkWatchPacket(level.dimension().location(), chunks), player)
                level.chunkSource.move(player)
            }
            val map = level.chunkSource.chunkMap as ChunkMapAccessor
            var remaining = 6
            for (key in chunks) {
                if (key in watch.sent) continue
                val pos = ChunkPos(key)
                val chunk = level.chunkSource.getChunkNow(pos.x, pos.z) ?: continue
                watch.sent.add(key)
                map.callPlayerLoadedChunk(player, MutableObject<ClientboundLevelChunkWithLightPacket>(), chunk)
                if (--remaining == 0) break
            }
            if (chunks.isEmpty()) watches.remove(player)
        }
    }
}
