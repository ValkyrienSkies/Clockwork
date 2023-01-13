package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.forge.config.AllClockworkConfigs;

@Mod(ClockWorkMod.MOD_ID)
public class ClockWorkModForge {
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ClockWorkMod.MOD_ID);
    public static final CreativeModeTab BASE_CREATIVE_TAB = new ClockworkGroup();
    static IEventBus MOD_BUS;
    boolean happendClientSetup = false;

    public ClockWorkModForge() {
        // Submit our event bus to let architectury register our content on the right time
//        MOD_BUS = FMLJavaModLoadingContext.get().getModEventBus();
//        MOD_BUS.addListener(this::clientSetup);
//        REGISTRATE.registerEventListeners(MOD_BUS);
//        MOD_BUS.addListener(this::onModelRegistry);
//        MOD_BUS.addListener(this::clientSetup);
//        MOD_BUS.addListener(this::entityRenderers);
        ModLoadingContext modLoadingContext = ModLoadingContext.get();

        IEventBus modEventBus = FMLJavaModLoadingContext.get()
                .getModEventBus();
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;

        REGISTRATE.registerEventListeners(modEventBus);

        AllClockworkBlocks.register();
        AllClockworkItems.register();
        AllClockworkTileEntities.register();

        AllClockworkParticles.register(modEventBus);
        AllClockworkConfigs.register(modLoadingContext);

        ClockWorkMod.init();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClockworkClientForge.onCWClient(modEventBus, forgeEventBus));
    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(ClockWorkMod.MOD_ID, path);
    }

    void clientSetup(final FMLClientSetupEvent event) {
        if (happendClientSetup) return;
        happendClientSetup = true;

        ClockWorkMod.initClient();
        AllClockworkPartials.init();
    }

    void entityRenderers(final EntityRenderersEvent.RegisterRenderers event) {

    }

    void onModelRegistry(final ModelRegistryEvent event) {

    }
}
