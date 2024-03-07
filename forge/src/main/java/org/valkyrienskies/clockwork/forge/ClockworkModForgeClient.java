package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.AllParticleTypes;
import dev.architectury.event.events.client.ClientReloadShadersEvent;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.eventbus.api.IEventBus;
import org.valkyrienskies.clockwork.ClockworkModClient;
import org.valkyrienskies.clockwork.ClockworkPartials;
import org.valkyrienskies.clockwork.ClockworkShaders;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderWandClusterRenderer;
import org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron.ForgeGravitronHandler;

import java.io.IOException;

public class ClockworkModForgeClient {

    public static final ForgeGravitronHandler GRAVITRON_HANDLER = new ForgeGravitronHandler();
    public static final WanderWandClusterRenderer WANDER_HANDLER = new WanderWandClusterRenderer();

    public static void onCtorClient(IEventBus modEventBus) {
        ClockworkPartials.INSTANCE.init();
        ClockworkModClient.initClient();
        modEventBus.addListener(AllParticleTypes::registerFactories);
        ClockworkShaders.INSTANCE.init();
        //ClientReloadShadersEvent.EVENT.register(ClockworkModForgeClient::onShaderReload);
     }

}
