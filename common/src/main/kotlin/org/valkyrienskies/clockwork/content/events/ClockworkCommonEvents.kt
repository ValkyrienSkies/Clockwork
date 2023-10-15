package org.valkyrienskies.clockwork.content.events

import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.ActiveChutes

object ClockworkCommonEvents {
    fun onWorldTick(level: ServerLevel) {
        ActiveChutes.tick(level)
    }
}
