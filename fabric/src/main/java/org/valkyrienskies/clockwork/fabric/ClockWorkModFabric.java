package org.valkyrienskies.clockwork.fabric;

import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.fabricators_of_create.porting_lib.event.client.MouseButtonCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.content.events.ClockworkClientEvents;
import org.valkyrienskies.clockwork.content.events.ClockworkInputEvents;
import org.valkyrienskies.clockwork.fabric.config.AllClockworkConfigs;
import org.valkyrienskies.clockwork.fabric.content.events.FabricClockworkClientEvents;
import org.valkyrienskies.clockwork.fabric.content.events.FabricClockworkCommonEvents;
import org.valkyrienskies.clockwork.fabric.content.events.FabricClockworkInputEvents;
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric;

public class ClockWorkModFabric implements ModInitializer {

    public static void init() {
        ClockWorkParticles.init();
        AllClockworkConfigs.init();

        FabricClockworkParticles.init();
        FabricClockworkSounds.init();

        //ClockworkCommonEvents.register();
        FabricClockworkCommonEvents.register();
    }

    public static void gatherData(FabricDataGenerator gen, ExistingFileHelper helper) {
        gen.addProvider(FabricClockworkSounds.provider(gen));
    }

    @Override
    public void onInitialize() {
        // force VS2 to load before eureka
        new ValkyrienSkiesModFabric().onInitialize();

        ClockWorkBlocks.register();
        FabricClockworkBlocks.register();

        // TODO common items
        ClockWorkItems.register();
        FabricClockworkItems.register();

        ClockWorkBlockEntities.register();
        FabricClockworkBlockEntities.register();

        // TODO common entities
        ClockWorkEntities.register();
        FabricClockworkEntities.register();

        ClockWorkSounds.register();
        FabricClockworkSounds.prepare();

        ClockWorkMod.REGISTRATE.register();

        ClockWorkMod.init();
        ClockWorkModFabric.init();
    }

    @Environment(EnvType.CLIENT)
    public static class Client implements ClientModInitializer {
        @Override
        public void onInitializeClient() {
            ClockWorkMod.initClient();

            ClockWorkPartials.init();
            FabricClockworkPartials.init();

            ClockWorkParticles.initClient();
            FabricClockworkParticles.initClient();

            registerClientEvents();
            registerClientEvents();
            FabricClockworkClientEvents.register();
            FabricClockworkInputEvents.register();
            ShaderLoader.init();
        }

        public static void registerClientEvents() {
            ClientTickEvents.END_CLIENT_TICK.register(ClockworkClientEvents::onTick);
            ClientTickEvents.START_CLIENT_TICK.register(ClockworkClientEvents::onTickStart);
        }

        public static void registerInputEvents() {
            MouseButtonCallback.EVENT.register(ClockworkInputEvents::onClickInputCW);
        }
    }

    public static class ModMenu implements ModMenuApi {
    }
}
