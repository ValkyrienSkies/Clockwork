package org.valkyrienskies.clockwork.fabric;

import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import org.valkyrienskies.clockwork.*;
//import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderWandClusterRenderer;
import org.valkyrienskies.clockwork.content.events.ClockworkCommonEvents;
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric;

public class ClockworkModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        new ValkyrienSkiesModFabric().onInitialize();

        ClockworkTags.INSTANCE.init();
        ClockworkSounds.register();
        ClockworkBlocks.register();
        ClockworkItems.register();

        ClockworkBlockEntities.register();

        ClockworkEntities.register();
        FabricClockworkEntities.register();
        FabricClockworkFluids.register();

        ClockworkContraptions.init();


        FabricClockworkSounds.prepare();

        ClockworkMod.INSTANCE.getREGISTRATE().register();

        RegisterResourceManagers.INSTANCE.init();

        ClockworkMod.init();
        //AllClockworkConfigs.init();

        ClockworkParticles.init();
        FabricClockworkSounds.init();
        registerServerEvents();

        ClockworkBoilerHeaters.INSTANCE.init();
    }

    public static void registerServerEvents() {
        ServerTickEvents.START_WORLD_TICK.register(ClockworkCommonEvents.INSTANCE::onWorldTick);
        LivingEntityEvents.LivingTickEvent.TICK.register(FabricClockworkCommonEvents::onLivingTick);
        AttackBlockCallback.EVENT.register(FabricClockworkCommonEvents::playerLeftClick);
    }
}
