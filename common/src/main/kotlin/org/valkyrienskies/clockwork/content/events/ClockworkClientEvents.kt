package org.valkyrienskies.clockwork.content.events

import net.minecraft.client.Minecraft
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.GravitronRenderHandler
import org.valkyrienskies.clockwork.util.render.BoltUtil

object ClockworkClientEvents {
    private val ITEM_PREFIX = "item." + ClockworkMod.MOD_ID
    private val BLOCK_PREFIX = "block." + ClockworkMod.MOD_ID
    val GRAVITRON_HANDLER = GravitronRenderHandler()

    fun onTickStart(client: Minecraft?) {
    }

    fun onTick(client: Minecraft?) {
        ClockworkMod.OUTLINER.tickOutlines()
        GRAVITRON_HANDLER.tick();
    }

    fun onRenderTick() {
        BoltUtil.tick()
    }
}
