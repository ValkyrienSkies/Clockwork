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
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.valkyrienskies.clockwork.fabric.config.AllClockworkConfigs;
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
        AllClockworkBlocks.register();
        AllClockworkItems.register();
        AllClockworkTileEntities.register();

        REGISTRATE.register();

        AllClockworkParticles.register();
        AllClockworkConfigs.register();
        ClockWorkMod.init();
    }

    @Environment(EnvType.CLIENT)
    public static class Client implements ClientModInitializer {

        @Override
        public void onInitializeClient() {
            ClockWorkMod.initClient();
            AllClockworkPartials.init();
            AllClockworkParticles.registerFactories();
        }


    }

    public static class ModMenu implements ModMenuApi {
    }
}
