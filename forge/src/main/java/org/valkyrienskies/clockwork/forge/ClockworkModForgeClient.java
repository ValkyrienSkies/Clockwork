package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.AllParticleTypes;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.valkyrienskies.clockwork.ClockworkModClient;
import org.valkyrienskies.clockwork.ClockworkPartials;
import org.valkyrienskies.clockwork.ClockworkShaders;
import org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron.ForgeGravitronHandler;
import org.valkyrienskies.clockwork.forge.content.curiosities.tools.wanderwand.ForgeWanderwandHandler;

import java.io.IOException;

public class ClockworkModForgeClient {

    public static final ForgeGravitronHandler GRAVITRON_HANDLER = new ForgeGravitronHandler();
    public static final ForgeWanderwandHandler WANDERWAND_HANDLER = new ForgeWanderwandHandler();
    //public static final WanderWandClusterRenderer WANDER_HANDLER = new WanderWandClusterRenderer();

    public static void onCtorClient(IEventBus modEventBus) {
        ClockworkPartials.INSTANCE.init();
        ClockworkModClient.initClient();
        ClockworkShaders.INSTANCE.init();
        modEventBus.addListener(AllParticleTypes::registerFactories);

        //ClientReloadShadersEvent.EVENT.register(ClockworkModForgeClient::onShaderReload);
    }
}
