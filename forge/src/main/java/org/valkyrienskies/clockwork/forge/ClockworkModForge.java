package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.AllParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.DeferredRegister;
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

import static org.valkyrienskies.clockwork.ClockworkMod.MOD_ID;

@Mod(MOD_ID)
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

        DeferredRegister<CreativeModeTab> deferredRegister = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
        deferredRegister.register("general",
                ClockworkMod.INSTANCE::createCreativeTab
        );
        deferredRegister.register(modEventBus);

        if (FMLLoader.getLoadingModList().getModFileById("computercraft") != null){
            //ClockworkForgePeripheralProviders.register();
        }
    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    void clientSetup(final FMLClientSetupEvent event) {
        if (happendClientSetup) return;
        happendClientSetup = true;
    }

    void entityRenderers(final EntityRenderersEvent.RegisterRenderers event) {

    }

    void onParticleRegistry(RegisterParticleProvidersEvent event) {
        ClockworkParticles.initClient();
    }

}
