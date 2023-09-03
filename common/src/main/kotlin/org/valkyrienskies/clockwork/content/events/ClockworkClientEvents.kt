package org.valkyrienskies.clockwork.content.events

import com.jozufozu.flywheel.backend.Backend
import net.minecraft.client.Minecraft
import org.valkyrienskies.clockwork.ClockworkMod

object ClockworkClientEvents {
    private val ITEM_PREFIX = "item." + ClockworkMod.MOD_ID
    private val BLOCK_PREFIX = "block." + ClockworkMod.MOD_ID
    fun onTickStart(client: Minecraft?) {
    }

    fun onTick(client: Minecraft?) {
        if (!Backend.isGameActive()) {
        }
        ClockworkMod.OUTLINER.tickOutlines()
        //        ClockWorkMod.Client.BLUPER_HANDLER.tick();
//        ClockWorkMod.Client.GRAVITRON_HANDLER.tick();
    }
}