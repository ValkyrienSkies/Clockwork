package org.valkyrienskies.clockwork.forge;

import net.minecraftforge.eventbus.api.IEventBus;

public class ClockworkClientForge {

    public static void onCWClient(IEventBus modEventBus, IEventBus forgeEventBus) {
        modEventBus.addListener(AllClockworkParticles::registerFactories);
    }
}
