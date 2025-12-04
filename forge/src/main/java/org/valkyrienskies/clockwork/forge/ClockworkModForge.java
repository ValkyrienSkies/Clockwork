package org.valkyrienskies.clockwork.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.forge.config.AllClockworkConfigs;
import org.valkyrienskies.mod.compat.clothconfig.VSClothConfig;
import org.valkyrienskies.clockwork.util.AtmosphereParametersResolver;

import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;
import static org.valkyrienskies.clockwork.ClockworkMod.MOD_ID;

@Mod(MOD_ID)
public class ClockworkModForge {
    public ClockworkModForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        EventBuses.registerModEventBus(MOD_ID, modEventBus);
        ClockworkMod.INSTANCE.getREGISTRATE().registerEventListeners(modEventBus);
        ClockworkSounds.register();
        ClockworkBlocks.register();
        ClockworkItems.register();
        ClockworkBlockEntities.register();
        ForgeClockworkBlockEntities.register();

        modEventBus.addListener(this::onRegister);

        ClockworkEntities.register();
        ForgeClockworkEntities.register();

        ClockworkParticles.init();

        //AllClockworkConfigs.register(modLoadingContext);


        ClockworkPackets.init();

        //todo: fix this you fucking moron
        //ForgeClockworkWorldgen.CONFIGURED_FEATURES.register(modEventBus);
        //ForgeClockworkWorldgen.PLACED_FEATURES.register(modEventBus);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClockworkModForgeClient.onCtorClient(modEventBus));

        modEventBus.addListener(this::onClientSetup);
        EVENT_BUS.addListener(this::registerResourceManagers);

        modEventBus.addListener(ClockworkModForge::init);
        ClockworkMod.init();

//        //todo fix forge vscore issue
//        modLoadingContext.registerExtensionPoint(
//                ConfigGuiHandler.ConfigGuiFactory.class,
//                () -> new ConfigGuiHandler.ConfigGuiFactory((minecraft, screen) -> VSClothConfig.createConfigScreenFor(screen, ClockworkConfig.class))
//        );

    }

    private void onRegister(RegisterEvent evt) {
        ClockworkContraptions.init();
    }

    private void registerResourceManagers(AddReloadListenerEvent event) {
        event.addListener(AtmosphereParametersResolver.INSTANCE);
    }

    public static void init(final FMLCommonSetupEvent event) {}

    private void onClientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(ClockworkBlocks.GOO_BLOCK.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ClockworkBlocks.SLICKER.get(), RenderType.translucent());
    }
}
