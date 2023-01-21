package org.valkyrienskies.clockwork.fabric.content.events;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.valkyrienskies.clockwork.ClockWorkHandlers;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.content.contraptions.propellor.stream.PropStream;

import static com.jozufozu.flywheel.backend.Backend.isGameActive;

public class FabricClockworkClientEvents {
    private static final String ITEM_PREFIX = "item." + ClockWorkMod.MOD_ID;
    private static final String BLOCK_PREFIX = "block." + ClockWorkMod.MOD_ID;

    public static void onTickStart(Minecraft client) {

        PropStream.tickClientPlayerSounds();

    }

    public static void onTick(Minecraft client) {
        if (!isGameActive())
            return;

        ClockWorkHandlers.tick();
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FabricClockworkClientEvents::onTick);
        ClientTickEvents.START_CLIENT_TICK.register(FabricClockworkClientEvents::onTickStart);
    }
}
