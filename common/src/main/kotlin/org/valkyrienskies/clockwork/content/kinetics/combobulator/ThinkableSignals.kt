package org.valkyrienskies.clockwork.content.kinetics.combobulator

import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import kotlin.math.ceil

object ThinkableSignals {
    fun travelTicks(length: Double): Int = ceil(length * 2).toInt().coerceIn(12, 80)

    fun tick(level: ServerLevel, link: WandLinks.Link): Boolean {
        val receiver = ThinkableConnections.device(level, link.b)
        val value = ThinkableItem.source(level, link.a)
        val available = receiver?.acceptsThinkable(link.b.face) == true && value != null
        receiver?.receiveThinkable(link.b.face, value.takeIf { available })
        if (!available) {
            val changed = link.pulseStart >= 0
            link.pulseStart = -1
            link.pulseArrived = true
            return changed
        }
        val now = level.gameTime
        if (!link.pulseArrived && now >= link.pulseStart + link.pulseDuration) {
            link.pulseArrived = true
            receiver?.onThinkablePulse(link.b.face)
        }
        if (link.nextPulse == 0L) {
            link.nextPulse = now + 20 + level.random.nextInt(60)
            return false
        }
        if (now < link.nextPulse) return false
        link.pulseStart = now
        link.pulseDuration = travelTicks(link.length)
        link.pulseArrived = false
        link.nextPulse = now + link.pulseDuration + 80 + level.random.nextInt(81)
        return true
    }
}
