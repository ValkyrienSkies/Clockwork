package org.valkyrienskies.clockwork.forge;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.forge.integration.cc.ClockworkForgePeripheralProviders;
import org.valkyrienskies.clockwork.util.AtmosphereParametersResolver;
import org.valkyrienskies.mod.compat.clothconfig.VSClothConfig;

import static org.valkyrienskies.clockwork.ClockworkMod.MOD_ID;

@Mod(MOD_ID)
public class ClockworkModForge {

    //final DeferredRegister<EntityDataSerializer<?>> DATA_SERIALIZER_REGISTER = DeferredRegister.create(ForgeRegistries.Keys.DATA_SERIALIZERS, MOD_ID);

    public ClockworkModForge() {
        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ClockworkMod.INSTANCE.getREGISTRATE().registerEventListeners(modEventBus);

        ClockworkBlocks.register();
        ClockworkItems.register();
        ClockworkBlockEntities.register();
        ForgeClockworkBlockEntities.register();

        ForgeClockworkFluids.register();

        ClockworkEntities.register();
        ForgeClockworkEntities.register();

        ClockworkParticles.init();

        //AllClockworkConfigs.register(modLoadingContext);

        ClockworkSounds.register();
        ClockworkPackets.init();

        ClockworkShaders.INSTANCE.init();

        //ForgeClockworkWorldgen.CONFIGURED_FEATURES.register(modEventBus);
        //ForgeClockworkWorldgen.PLACED_FEATURES.register(modEventBus);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClockworkModForgeClient.onCtorClient(modEventBus));

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerResourceManagers);

        if (FMLLoader.getLoadingModList().getModFileById("computercraft") != null) {
            ClockworkForgePeripheralProviders.register();
        }

        modEventBus.addListener(ClockworkModForge::init);

        //todo fix forge vscore issue
        modLoadingContext.registerExtensionPoint(
                ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((minecraft, screen) -> VSClothConfig.createConfigScreenFor(screen, ClockworkConfig.class))
        );

    }

    private void registerResourceManagers(AddReloadListenerEvent event) {
        event.addListener(AtmosphereParametersResolver.INSTANCE);
    }

    public static void init(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ClockworkMod.init();
            ClockworkShaders.INSTANCE.init();
        });
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(ClockworkBlocks.GOO_BLOCK.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ClockworkBlocks.SLICKER.get(), RenderType.translucent());
    }
}
