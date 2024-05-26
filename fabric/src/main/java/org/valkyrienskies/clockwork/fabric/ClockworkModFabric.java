package org.valkyrienskies.clockwork.fabric;

import io.github.fabricators_of_create.porting_lib.event.common.LivingEntityEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderWandClusterRenderer;
import org.valkyrienskies.clockwork.content.events.ClockworkCommonEvents;
import org.valkyrienskies.clockwork.fabric.config.AllClockworkConfigs;
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric;

public class ClockworkModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        new ValkyrienSkiesModFabric().onInitialize();

        ClockworkTags.INSTANCE.init();
        ClockworkBlocks.register();
        ClockworkItems.register();

        ClockworkBlockEntities.register();
        FabricClockworkBlockEntities.register();

        ClockworkEntities.register();
        FabricClockworkEntities.register();
        FabricClockworkFluids.register();

        ClockworkSounds.register();
        FabricClockworkSounds.prepare();

        ClockworkMod.INSTANCE.getREGISTRATE().register();

        ClockworkMod.init();
        //FabricClockworkWorldgen.bootstrap();
        AllClockworkConfigs.init();

        ClockworkParticles.init();
        FabricClockworkSounds.init();
        registerServerEvents();

        var gearwork = new ResourceLocation(ClockworkMod.MOD_ID, "gearwork");
        FabricLoader.getInstance().getModContainer(ClockworkMod.MOD_ID).ifPresent(container -> ResourceManagerHelper.registerBuiltinResourcePack(gearwork, container, "Clockwork: Gearwork", ResourcePackActivationType.NORMAL));


    }

    public static void registerServerEvents() {
        ServerTickEvents.START_WORLD_TICK.register(ClockworkCommonEvents.INSTANCE::onWorldTick);
        LivingEntityEvents.TICK.register(FabricClockworkCommonEvents::onLivingTick);
        AttackBlockCallback.EVENT.register(FabricClockworkCommonEvents::playerLeftClick);
    }
}
