package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.AllParticleTypes;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.ClockworkModClient;
import org.valkyrienskies.clockwork.ClockworkPartials;
import org.valkyrienskies.clockwork.ClockworkShaders;
import org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron.ForgeGravitronHandler;

public class ClockworkModForgeClient {

    public static final ForgeGravitronHandler GRAVITRON_HANDLER = new ForgeGravitronHandler();

    public static void onCtorClient(IEventBus modEventBus) {
        ClockworkPartials.INSTANCE.init();
        ClockworkModClient.initClient();
        modEventBus.addListener(AllParticleTypes::registerFactories);
        ClockworkShaders.INSTANCE.init();
        OverlayRegistry.registerOverlayAbove(ForgeIngameGui.HOTBAR_ELEMENT, "Gravitron",
                ClockworkModForgeClient.GRAVITRON_HANDLER.getOverlayRenderer());
    }
}
