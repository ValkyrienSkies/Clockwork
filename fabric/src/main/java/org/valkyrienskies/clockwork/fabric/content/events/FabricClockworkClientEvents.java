package org.valkyrienskies.clockwork.fabric.content.events;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.valkyrienskies.clockwork.ClockworkMod;

import static com.jozufozu.flywheel.backend.Backend.isGameActive;

public class FabricClockworkClientEvents {
    private static final String ITEM_PREFIX = "item." + ClockworkMod.MOD_ID;
    private static final String BLOCK_PREFIX = "block." + ClockworkMod.MOD_ID;

    public static void onTickStart(Minecraft client) {

    }

    public static void onTick(Minecraft client) {
        if (!isGameActive())
            return;

        //ClockWorkHandlers.tick();
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FabricClockworkClientEvents::onTick);
        ClientTickEvents.START_CLIENT_TICK.register(FabricClockworkClientEvents::onTickStart);
    }
}
