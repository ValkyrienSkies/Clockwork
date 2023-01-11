package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.fabric.config.AllClockworkConfigs;
import org.valkyrienskies.clockwork.fabric.content.curiosities.tools.bluperglue.BluperGlueSelectionHandler;
import org.valkyrienskies.clockwork.fabric.content.events.ClockworkClientEvents;
import org.valkyrienskies.clockwork.fabric.content.events.ClockworkCommonEvents;
import org.valkyrienskies.clockwork.fabric.content.events.ClockworkInputEvents;
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric;

import static org.valkyrienskies.clockwork.ClockWorkMod.MOD_ID;

public class ClockWorkModFabric implements ModInitializer {
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);
    public static final CreativeModeTab BASE_CREATIVE_TAB = new ClockworkGroup();

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static void init() {
        AllClockworkPackets.registerPackets();
    }

    @Override
    public void onInitialize() {
        // force VS2 to load before eureka
        new ValkyrienSkiesModFabric().onInitialize();
        AllClockworkBlocks.register();
        AllClockworkItems.register();
        AllClockworkTileEntities.register();
        AllClockworkEntities.register();

        REGISTRATE.register();

        AllClockworkParticles.register();
        AllClockworkConfigs.register();
        ClockWorkMod.init();
        ClockWorkModFabric.init();

        ClockworkCommonEvents.register();
        AllClockworkPackets.channel.initServerListener();
    }

    @Environment(EnvType.CLIENT)
    public static class Client implements ClientModInitializer {

        public static final BluperGlueSelectionHandler BLUPER_HANDLER = new BluperGlueSelectionHandler();

        @Override
        public void onInitializeClient() {
            ClockWorkMod.initClient();
            AllClockworkPartials.init();
            AllClockworkParticles.registerFactories();
            AllClockworkPackets.channel.initClientListener();

            ClockworkClientEvents.register();
            ClockworkInputEvents.register();

        }


    }

    public static class ModMenu implements ModMenuApi {
    }
}
