package org.valkyrienskies.clockwork.fabric;

import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.fabricators_of_create.porting_lib.event.client.MouseButtonCallback;
import io.github.fabricators_of_create.porting_lib.event.client.RenderTickStartCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.content.events.ClockworkClientEvents;
import org.valkyrienskies.clockwork.content.events.ClockworkCommonEvents;
import org.valkyrienskies.clockwork.content.events.ClockworkInputEvents;
import org.valkyrienskies.clockwork.data.ClockworkTags;
import org.valkyrienskies.clockwork.fabric.config.AllClockworkConfigs;
import org.valkyrienskies.clockwork.fabric.content.events.FabricClockworkClientEvents;
import org.valkyrienskies.clockwork.fabric.content.events.FabricClockworkCommonEvents;
import org.valkyrienskies.clockwork.fabric.content.events.FabricClockworkInputEvents;
import org.valkyrienskies.clockwork.fabric.integration.cc_restiched.ClockworkFabricPeripheralProviders;
import org.valkyrienskies.clockwork.platform.fabric.FallbackFabricTransfer;
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric;

public class ClockworkModFabric implements ModInitializer {

    public static void init() {
        ClockworkParticles.init();
        AllClockworkConfigs.init();

        FabricClockworkSounds.init();
        FallbackFabricTransfer.init();

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

        ClockworkTags.INSTANCE.init();

        ClockworkBlocks.register();
        FabricClockworkBlocks.register();

        // TODO common items
        ClockworkItems.register();
        FabricClockworkItems.register();

        ClockworkBlockEntities.register();
        FabricClockworkBlockEntities.register();

        // TODO common entities
        ClockworkEntities.INSTANCE.register();
        FabricClockworkEntities.register();

        //ClockworkFluids.INSTANCE.register();
        FabricClockworkFluids.register();

        ClockworkSounds.INSTANCE.register();
        FabricClockworkSounds.prepare();

        ClockworkMod.INSTANCE.getREGISTRATE().register();

        ClockworkMod.init();
        ClockworkModFabric.init();

        registerServerEvents();

        if (FabricLoader.getInstance().isModLoaded("computercraft"))
            ClockworkFabricPeripheralProviders.register();
    }

    public static void registerServerEvents() {
        ServerTickEvents.START_WORLD_TICK.register(ClockworkCommonEvents.INSTANCE::onWorldTick);
    }

    @Environment(EnvType.CLIENT)
    public static class Client implements ClientModInitializer {
        @Override
        public void onInitializeClient() {
            ClockworkMod.initClient();

            ClockworkPartials.INSTANCE.init();
            FabricClockworkPartials.init();

            ClockworkParticles.initClient();

            registerClientEvents();
            registerClientEvents();
            FabricClockworkClientEvents.register();
            FabricClockworkInputEvents.register();
            ShaderLoader.init();
        }

        public static void registerClientEvents() {
            ClientTickEvents.END_CLIENT_TICK.register(ClockworkClientEvents.INSTANCE::onTick);
            ClientTickEvents.START_CLIENT_TICK.register(ClockworkClientEvents.INSTANCE::onTickStart);
            RenderTickStartCallback.EVENT.register(ClockworkClientEvents.INSTANCE::onRenderTick);
        }

        public static void registerInputEvents() {
            MouseButtonCallback.EVENT.register(ClockworkInputEvents.INSTANCE::onClickInputCW);
        }
    }

    public static class ModMenu implements ModMenuApi {
    }
}
