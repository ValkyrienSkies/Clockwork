package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.model.BakedModelManagerHelper;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.valkyrienskies.clockwork.fabric.config.AllClockworkConfigs;
import org.valkyrienskies.clockwork.fabric.content.curiosities.tools.bluperglue.BluperGlueSelectionHandler;
import org.valkyrienskies.clockwork.fabric.content.curiosities.tools.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.fabric.content.curiosities.tools.gravitron.GravitronItemRenderer;
import org.valkyrienskies.clockwork.fabric.content.curiosities.tools.gravitron.GravitronRenderHandler;
import org.valkyrienskies.clockwork.fabric.content.events.ClockworkClientEvents;
import org.valkyrienskies.clockwork.fabric.content.events.ClockworkCommonEvents;
import org.valkyrienskies.clockwork.fabric.content.events.ClockworkInputEvents;
import org.valkyrienskies.clockwork.fabric.render.assemblyscan.ScanShaders;
import org.valkyrienskies.core.impl.config.VSConfigClass;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.mod.compat.clothconfig.VSClothConfig;
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric;
import org.valkyrienskies.clockwork.fabric.AllClockworkPartials;

import static org.valkyrienskies.clockwork.ClockWorkMod.MOD_ID;

public class ClockWorkModFabric implements ModInitializer {
    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    public static final CreativeModeTab BASE_CREATIVE_TAB = new ClockworkGroup();


    @Override
    public void onInitialize() {
        // force VS2 to load before eureka
        new ValkyrienSkiesModFabric().onInitialize();
        AllClockworkSounds.prepare();
        AllClockworkBlocks.register();
        AllClockworkItems.register();
        AllClockworkTileEntities.register();
        AllClockworkEntities.register();

        REGISTRATE.register();

        AllClockworkParticles.register();
        AllClockworkConfigs.register();
        ClockWorkMod.init();
        ClockWorkModFabric.init();

        AllClockworkSounds.register();
        ClockworkCommonEvents.register();
        AllClockworkPackets.channel.initServerListener();
    }

    public static void init() {
        AllClockworkPackets.registerPackets();
    }

    public static void gatherData(FabricDataGenerator gen, ExistingFileHelper helper) {
        gen.addProvider(AllClockworkSounds.provider(gen));
    }

    @Environment(EnvType.CLIENT)
    public static class Client implements ClientModInitializer {

        public static final BluperGlueSelectionHandler BLUPER_HANDLER = new BluperGlueSelectionHandler();
        public static final GravitronRenderHandler GRAVITRON_HANDLER = new GravitronRenderHandler();
        @Override
        public void onInitializeClient() {
            ClockWorkMod.initClient();
            AllClockworkPartials.init();
            AllClockworkParticles.registerFactories();
            AllClockworkPackets.channel.initClientListener();
            ScanShaders.initialize();
            ClockworkClientEvents.register();
            ClockworkInputEvents.register();
            BuiltinItemRendererRegistry.INSTANCE.register(AllClockworkItems.GRAVITRON.get(), new GravitronItemRenderer());

        }


    }

    public static class ModMenu implements ModMenuApi {
    }
}
