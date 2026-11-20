package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.mod.common.toWorldCoordinates
import kotlin.math.abs
import kotlin.math.sqrt

internal object GatemakyrAudio {
    private fun inRange(frame: GatemakyrTarget.Frame, width: Int, height: Int, point: Vec3, range: Double): Boolean {
        val radius = 0.5 * sqrt(width * width * frame.u.lengthSqr() + height * height * frame.v.lengthSqr())
        return frame.position.distanceToSqr(point) <= (range + radius) * (range + radius)
    }

    fun apparentSource(passage: GatemakyrPassage, width: Int, height: Int, oneSided: Boolean,
                       listener: Vec3, source: Vec3, range: Double): Vec3? {
        val near = passage.entrance
        val far = passage.exit
        if (!inRange(near, width, height, listener, range) || !inRange(far, width, height, source, range)) return null
        val listenerOffset = listener.subtract(near.position)
        val sourceOffset = source.subtract(far.position)
        val a = listenerOffset.dot(near.normal.normalize())
        val b = sourceOffset.dot(far.normal.normalize())
        if (abs(a) + abs(b) > range || oneSided && (a < -0.001 || b < -0.001) || a * b < -0.000001) return null
        val virtual = GatemakyrPassage(far, near).point(source)
        val totalDepth = abs(a) + abs(b)
        val fraction = if (totalDepth > 1e-8) abs(a) / totalDepth else 0.5
        val line = listener.lerp(virtual, fraction).subtract(near.position)
        val u = (line.dot(near.u) / near.u.lengthSqr()).coerceIn(-width * 0.5, width * 0.5)
        val v = (line.dot(near.v) / near.v.lengthSqr()).coerceIn(-height * 0.5, height * 0.5)
        val aperture = near.position.add(near.u.scale(u)).add(near.v.scale(v))
        val pathLength = listener.distanceTo(aperture) + source.distanceTo(passage.point(aperture))
        if (pathLength > range) return null
        val toward = aperture.subtract(listener)
        val direction = if (toward.lengthSqr() > 1e-10) toward else virtual.subtract(listener)
        return listener.add(direction.normalize().scale(pathLength))
    }

    fun nearest(gates: Collection<GatemakyrPlayerGate>, listener: Vec3, source: Vec3, range: Double): Vec3? {
        var best: Vec3? = null
        var limit = range
        for (gate in gates) {
            val point = apparentSource(gate.passage, gate.source.width, gate.source.height,
                gate.source.surface || gate.destination.surface, listener, source, limit) ?: continue
            best = point
            limit = listener.distanceTo(point)
        }
        return best
    }

    @JvmStatic fun broadcast(level: ServerLevel, except: Player?, x: Double, y: Double, z: Double, range: Double, packet: Packet<*>) {
        if (!level.server.isSameThread) return
        val gates = GatemakyrServer.playerGates(level).values
        if (gates.isEmpty()) return
        val source = level.toWorldCoordinates(Vec3(x, y, z))
        val audible = gates.filter { inRange(it.passage.exit, it.destination.width, it.destination.height, source, range) }
        if (audible.isEmpty()) return
        for (player in level.players()) {
            if (player === except) continue
            val point = nearest(audible, level.toWorldCoordinates(player.eyePosition), source, range) ?: continue
            val relay = when (packet) {
                is ClientboundSoundPacket -> ClientboundSoundPacket(packet.sound, packet.source, point.x, point.y, point.z,
                    packet.volume, packet.pitch, packet.seed)
                is ClientboundSoundEntityPacket -> ClientboundSoundPacket(packet.sound, packet.source, point.x, point.y, point.z,
                    packet.volume, packet.pitch, packet.seed)
                else -> return
            }
            player.connection.send(relay)
        }
    }
}
