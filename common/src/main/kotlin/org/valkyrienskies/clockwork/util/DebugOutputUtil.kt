package org.valkyrienskies.clockwork.util

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkLang

object DebugOutputUtil {
    fun indicatePositionToNearbyPlayers(level: Level, pos: Vec3, langKey: String?, vararg args: Any) {
        val particleType = ParticleTypes.END_ROD
        val particleCount = 20

        val player = level.getNearestPlayer(pos.x, pos.y, pos.z, 256.0, false)
        if (langKey != null) player?.displayClientMessage(ClockworkLang.translate(langKey, *args).component(), false)
        if (level is ServerLevel)
            level.sendParticles(
                particleType,
                pos.x, pos.y, pos.z,
                particleCount, 0.3, 0.3, 0.3, 0.0
            )
        else if (level is ClientLevel)
            repeat(particleCount) { level.addParticle(particleType, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0) }
    }
}