package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.GrabTool
import java.util.UUID

internal class GatemakyrPlayerValidator {
    private data class Crossing(val gate: GatemakyrPlayerGate, val contact: Vec3, val tick: Long, val priorMotion: GatemakyrPassage?)
    private val crossings = mutableMapOf<UUID, Crossing>()
    private val eyes = mutableMapOf<UUID, Vec3>()
    private var previousGates = emptyMap<Pair<UUID, Int>, GatemakyrPlayerGate>()
    var gates = emptyMap<Pair<UUID, Int>, GatemakyrPlayerGate>()
        private set

    fun tick(level: ServerLevel, rifts: Map<Pair<UUID, Int>, GatemakyrEffectPacket>) {
        previousGates = gates
        gates = GatemakyrPlayerGate.snapshot(level, rifts, false) { it }
        crossings.entries.removeIf { level.gameTime - it.value.tick > 5 }
        eyes.keys.removeIf { level.getPlayerByUUID(it) == null }
        for (player in level.players()) {
            val last = eyes.put(player.uuid, player.eyePosition) ?: continue
            observe(player, last, previousGates)
        }
    }

    fun observeMove(player: ServerPlayer, from: Vec3) {
        val last = eyes[player.uuid]
        if (last != null && last != from) observe(player, last, to = from)
        observe(player, from)
    }

    private fun observe(player: ServerPlayer, from: Vec3, previous: Map<Pair<UUID, Int>, GatemakyrPlayerGate> = gates,
                departed: Pair<UUID, Int>? = null, to: Vec3 = player.eyePosition) {
        if (!GatemakyrPlayerGate.eligible(player)) return
        eyes[player.uuid] = to
        var nearest = Double.POSITIVE_INFINITY
        for ((key, gate) in gates) {
            if (key == departed) continue
            val old = previous[key]?.takeIf(gate::matches) ?: continue
            val fraction = gate.crossing(old, from, to) ?: continue
            if (fraction < nearest) {
                nearest = fraction
                crossings[player.uuid] = Crossing(gate, gate.crossingPoint(old, from, to, fraction), player.level().gameTime, previousGates[key]?.takeIf(gate::matches)?.passage)
            }
        }
    }

    fun forget(player: ServerPlayer) {
        crossings.remove(player.uuid)
        eyes.remove(player.uuid)
    }

    fun accept(player: ServerPlayer, request: GatemakyrCrossingPacket, rifts: Map<Pair<UUID, Int>, GatemakyrEffectPacket>): Boolean {
        eyes[player.uuid]?.let { observe(player, it) }
        val crossing = crossings.remove(player.uuid) ?: return false
        val gate = gates[request.owner to request.side] ?: return false
        if (rifts[request.owner to request.side] !== gate.packet || rifts[request.owner to (1 - request.side)] !== gate.partner) return false
        if (!GatemakyrPlayerGate.eligible(player) || request.dimension != player.level().dimension().location() ||
            !gate.matches(crossing.gate) || gate.packet.startedAt != request.placed || gate.partner.startedAt != request.partnerPlaced ||
            player.level().gameTime - crossing.tick > 5) return false
        val eye = player.eyePosition
        val passage = crossing.gate.passage
        val offset = player.position().subtract(eye)
        val contact = passage.point(crossing.contact)
        val arrival = GatemakyrSurfaceCollision.exitAnchor(player, contact, passage.point(eye), offset, crossing.gate) ?: return false
        val pos = arrival.end.add(offset)
        val rotation = passage.angles(player.xRot, player.yRot)
        player.deltaMovement = passage.velocity(player.deltaMovement, crossing.contact, crossing.priorMotion)
        GrabTool.crossGate(player, gate, rotation.x, rotation.y)
        player.absMoveTo(pos.x, pos.y, pos.z, rotation.y, rotation.x)
        player.yHeadRot = rotation.y
        player.yBodyRot = rotation.y
        gate.refreshSupport(player)
        player.fallDistance = 0f
        player.connection.resetPosition()
        player.serverLevel().chunkSource.move(player)
        GatemakyrEntityTransitPacket.send(player, arrival.contact.add(offset))
        observe(player, arrival.contact, departed = gate.partner.owner to gate.partner.side)
        if (!(pos.distanceToSqr(request.predictedFeet) <= 0.0025))
            player.connection.teleport(pos.x, pos.y, pos.z, rotation.y, rotation.x)
        return true
    }
}
