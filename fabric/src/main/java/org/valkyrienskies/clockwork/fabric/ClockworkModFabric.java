package org.valkyrienskies.clockwork.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.content.events.ClockworkCommonEvents;
import org.valkyrienskies.mod.compat.clothconfig.VSClothConfig;
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

    public static class ModMenu implements ModMenuApi {
        @Override
        public ConfigScreenFactory<?> getModConfigScreenFactory() {
            return (parent) -> VSClothConfig.createConfigScreenFor(
                    parent,
                    ClockworkConfig.class
            );
        }
    }
}
