package org.valkyrienskies.clockwork.fabric;

import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.fabric.config.AllClockworkConfigs;
import org.valkyrienskies.clockwork.fabric.content.curiosities.tools.bluperglue.BluperGlueSelectionHandler;
import org.valkyrienskies.clockwork.fabric.content.events.FabricClockworkClientEvents;
import org.valkyrienskies.clockwork.fabric.content.events.FabricClockworkCommonEvents;
import org.valkyrienskies.clockwork.fabric.content.events.FabricClockworkInputEvents;
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric;

public class ClockWorkModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // force VS2 to load before eureka
        new ValkyrienSkiesModFabric().onInitialize();

        ClockWorkBlocks.register();
        FabricClockworkBlocks.register();

        // TODO common items
        FabricClockworkItems.register();

        ClockWorkBlockEntities.register();
        FabricClockworkBlockEntities.register();

        // TODO common entities
        FabricClockworkEntities.register();

        ClockWorkSounds.register();
        FabricClockworkSounds.prepare();

        ClockWorkMod.REGISTRATE.register();

        ClockWorkMod.init();
        ClockWorkModFabric.init();

        ClockworkContraptionTypes.prepare();
    }

    public static void init() {
        ClockworkParticles.init();
        AllClockworkConfigs.init();

        FabricClockworkParticles.init();
        FabricClockworkSounds.init();

        FabricClockworkPackets.registerPackets();
        FabricClockworkCommonEvents.register();
        FabricClockworkPackets.channel.initServerListener();
    }

    public static void gatherData(FabricDataGenerator gen, ExistingFileHelper helper) {
        gen.addProvider(FabricClockworkSounds.provider(gen));
    }

    @Environment(EnvType.CLIENT)
    public static class Client implements ClientModInitializer {

        public static final BluperGlueSelectionHandler BLUPER_HANDLER = new BluperGlueSelectionHandler();
        public static final GravitronRenderHandler GRAVITRON_HANDLER = new GravitronRenderHandler();
        @Override
        public void onInitializeClient() {
            ClockWorkMod.initClient();

            ClockWorkPartials.init();
            FabricClockworkPartials.init();

            ClockworkParticles.initClient();

            FabricClockworkParticles.initClient();
            FabricClockworkPackets.channel.initClientListener();

            FabricClockworkClientEvents.register();
            FabricClockworkInputEvents.register();

            ShaderLoader.init();
        }


    }

    public static class ModMenu implements ModMenuApi {
    }
}
