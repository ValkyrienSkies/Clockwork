package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.AllParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.valkyrienskies.clockwork.ClockworkBlockEntities;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.ClockworkEntities;
import org.valkyrienskies.clockwork.ClockworkItems;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.ClockworkPackets;
import org.valkyrienskies.clockwork.ClockworkPartials;
import org.valkyrienskies.clockwork.ClockworkParticles;
import org.valkyrienskies.clockwork.ClockworkSounds;
import org.valkyrienskies.clockwork.data.ClockworkTags;
import org.valkyrienskies.clockwork.forge.config.AllClockworkConfigs;

@Mod(ClockworkMod.MOD_ID)
public class ClockworkModForge {
    boolean happendClientSetup = false;

    public ClockworkModForge() {
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

        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::onParticleRegistry);
        modEventBus.addListener(this::onModelRegistry);
        ClockworkMod.INSTANCE.getREGISTRATE().registerEventListeners(modEventBus);

        ClockworkBlocks.register();
        ForgeClockworkBlocks.register();

        ClockworkTags.INSTANCE.init();
        // ForgeClockworkTags.init();

        ClockworkItems.register();
        ForgeClockworkItems.register();

        ClockworkBlockEntities.register();
        ForgeClockworkBlockEntities.register();

        //ClockworkFluids.INSTANCE.register();
        ForgeClockworkFluids.register();

        ClockworkEntities.INSTANCE.register();
        ForgeClockworkEntities.register();

        ClockworkParticles.init();
        AllClockworkConfigs.register(modLoadingContext);

        ClockworkSounds.INSTANCE.register();
        // TODO forge sounds

        ClockworkMod.init();
        ClockworkPackets.init();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            // In create itself they do it FMLClientSetupEvent this does not work (what a scam)
            // It prob gets staticly loaded earlier and well yhea...
            ClockworkPartials.INSTANCE.init();
            modEventBus.addListener(AllParticleTypes::registerFactories);
            // TODO forge partials

            ShaderLoader.init(modEventBus);
        });

        if (FMLLoader.getLoadingModList().getModFileById("computercraft") != null){
            //ClockworkForgePeripheralProviders.register();
        }

    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(ClockworkMod.MOD_ID, path);
    }

    void clientSetup(final FMLClientSetupEvent event) {
        if (happendClientSetup) return;
        happendClientSetup = true;
    }

    void entityRenderers(final EntityRenderersEvent.RegisterRenderers event) {

    }

    void onParticleRegistry(final ParticleFactoryRegisterEvent event) {
        ClockworkParticles.initClient();
    }

    void onModelRegistry(final ModelRegistryEvent event) {

    }
}
