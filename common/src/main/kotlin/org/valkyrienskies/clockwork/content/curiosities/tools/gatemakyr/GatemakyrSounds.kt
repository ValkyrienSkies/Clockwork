package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import org.valkyrienskies.mod.common.toWorldCoordinates
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkSounds
import java.util.UUID

object GatemakyrSounds {
    private class MovingSound(val level: ClientLevel, event: SoundEvent, repeat: Boolean, loudness: Float,
                              val position: () -> Vec3?) : AbstractTickableSoundInstance(event, SoundSource.PLAYERS, RandomSource.create()) {
        init {
            looping = repeat
            delay = 0
            volume = loudness
            update()
        }
        private fun update() {
            val point = position()
            if (point == null || Minecraft.getInstance().level !== level) { stop(); return }
            x = point.x; y = point.y; z = point.z
        }
        override fun tick() = update()
    }

    private val charges = mutableMapOf<UUID, MovingSound>()
    private val shots = mutableMapOf<GatemakyrClient.Flight, MovingSound>()
    private val portals = mutableMapOf<Pair<UUID, Int>, Pair<Long, MovingSound>>()

    private val networkPlayback = ThreadLocal<Boolean>()

    @JvmStatic fun fromNetwork(play: Runnable) {
        val previous = networkPlayback.get()
        networkPlayback.set(true)
        try {
            play.run()
        } finally {
            if (previous == null) networkPlayback.remove() else networkPlayback.set(previous)
        }
    }

    @JvmStatic fun relayLocal(level: ClientLevel, source: Vec3, event: SoundEvent, category: SoundSource,
                              volume: Float, pitch: Float, delayed: Boolean, seed: Long) {
        if (networkPlayback.get() == true || volume <= 0f) return
        val gates = GatemakyrClient.rayGates(level).values
        if (gates.isEmpty()) return
        val mc = Minecraft.getInstance()
        val listener = mc.gameRenderer.mainCamera.position
        val point = GatemakyrAudio.nearest(gates, listener, level.toWorldCoordinates(source), event.getRange(volume).toDouble()) ?: return
        val sound = SimpleSoundInstance(event, category, volume, pitch, RandomSource.create(seed), point.x, point.y, point.z)
        val distance = listener.distanceTo(point)
        if (delayed && distance > 10.0) mc.soundManager.playDelayed(sound, (distance / 2).toInt())
        else mc.soundManager.play(sound)
    }

    fun charge(player: Player) {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        if (charges.containsKey(player.uuid)) return
        val sound = MovingSound(level, ClockworkSounds.GATEMAKYR_CHARGE.mainEvent!!, false, 0.65f) {
            level.getPlayerByUUID(player.uuid)?.takeIf { it.isAlive && it.mainHandItem.item is GatemakyrItem }?.eyePosition
        }
        charges[player.uuid] = sound
        mc.soundManager.play(sound)
    }

    fun stopCharge(owner: UUID) {
        charges.remove(owner)?.let { Minecraft.getInstance().soundManager.stop(it) }
    }

    fun oneShot(sound: ClockworkSounds.SoundEntry, position: Vec3, volume: Float = 0.7f) {
        Minecraft.getInstance().level?.playLocalSound(position.x, position.y, position.z, sound.mainEvent!!, SoundSource.PLAYERS, volume, 1f, false)
    }

    fun clear() {
        val manager = Minecraft.getInstance().soundManager
        charges.values.forEach(manager::stop)
        shots.values.forEach(manager::stop)
        portals.values.forEach { manager.stop(it.second) }
        charges.clear(); shots.clear(); portals.clear()
    }

    fun tick() {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val now = level.gameTime
        charges.entries.removeIf { (owner, sound) ->
            val active = GatemakyrClient.state(level.getPlayerByUUID(owner))?.animation?.charging == true
            if (!active) mc.soundManager.stop(sound)
            !active
        }
        shots.entries.removeIf { (flight, sound) ->
            val ended = flight !in GatemakyrClient.flights || now >= flight.packet.startedAt + flight.packet.duration
            if (ended) mc.soundManager.stop(sound)
            ended
        }
        for (flight in GatemakyrClient.flights) {
            if (shots.containsKey(flight) || now >= flight.packet.startedAt + flight.packet.duration) continue
            val sound = MovingSound(level, ClockworkSounds.GATEMAKYR_PROJECTILE.mainEvent!!, true, 0.4f) {
                val end = flight.packet.target?.frame(level)?.impact
                end?.let { flight.packet.flightPoint(flight.origin ?: flight.packet.origin, it,
                    ((level.gameTime - flight.packet.startedAt).toDouble() / flight.packet.duration).coerceIn(0.0, 1.0)) }
            }
            shots[flight] = sound
            mc.soundManager.play(sound)
        }
        portals.entries.removeIf { (key, entry) ->
            val changed = GatemakyrClient.rifts[key]?.packet?.startedAt != entry.first
            if (changed) mc.soundManager.stop(entry.second)
            changed
        }
        for ((key, rift) in GatemakyrClient.rifts) {
            if (portals.containsKey(key) || portals.size >= 16) continue
            val target = rift.packet.target ?: continue
            val position = target.frame(level)?.position ?: continue
            if (position.distanceToSqr(mc.gameRenderer.mainCamera.position) > 32.0 * 32.0) continue
            val sound = MovingSound(level, ClockworkSounds.PORTAL_LOOP.mainEvent!!, true, 0.28f) {
                target.frame(level)?.position?.takeIf { it.distanceToSqr(mc.gameRenderer.mainCamera.position) < 40.0 * 40.0 }
            }
            portals[key] = rift.packet.startedAt to sound
            mc.soundManager.play(sound)
        }
        portals.entries.removeIf { it.value.second.isStopped }
    }
}
