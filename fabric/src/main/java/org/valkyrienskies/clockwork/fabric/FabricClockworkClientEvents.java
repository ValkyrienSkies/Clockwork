package org.valkyrienskies.clockwork.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.valkyrienskies.clockwork.ClockworkMod;

import static com.jozufozu.flywheel.backend.Backend.isGameActive;

public class FabricClockworkClientEvents {

    public static void onTickStart(Minecraft client) {

    }

    public static void onTick(Minecraft client) {
        if (!isGameActive())
            return;


    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FabricClockworkClientEvents::onTick);
        ClientTickEvents.START_CLIENT_TICK.register(FabricClockworkClientEvents::onTickStart);
    }

    public static void onRenderTick() {

    }
}