package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.AllParticleTypes;
import net.minecraftforge.eventbus.api.IEventBus;
import org.valkyrienskies.clockwork.ClockworkModClient;
import org.valkyrienskies.clockwork.ClockworkPartials;
import org.valkyrienskies.clockwork.ClockworkShaders;
import org.valkyrienskies.clockwork.content.curiosities.tools.designator.AuricDesignatorClusterRenderer;
import org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron.ForgeGravitronHandler;

public class ClockworkModForgeClient {
    public static final ForgeGravitronHandler GRAVITRON_HANDLER = new ForgeGravitronHandler();
    public static final AuricDesignatorClusterRenderer AURIC_HANDLER = new AuricDesignatorClusterRenderer();

    public static void onCtorClient(IEventBus modEventBus) {
        ClockworkPartials.INSTANCE.init();
        ClockworkModClient.initClient();
        modEventBus.addListener(AllParticleTypes::registerFactories);
        ClockworkShaders.INSTANCE.init();
    }
}
