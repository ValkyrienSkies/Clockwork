package org.valkyrienskies.clockwork.forge;

import dev.architectury.platform.Platform;
import dev.architectury.platform.forge.EventBuses;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.valkyrienskies.clockwork.*;

import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;
import static org.valkyrienskies.clockwork.ClockworkMod.MOD_ID;

@Mod(MOD_ID)
public class ClockworkModForge {
    public ClockworkModForge(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        context.registerConfig(
                ModConfig.Type.SERVER,
                ClockworkConfigUpdater.INSTANCE.getSERVER_SPEC(),
                "valkyrienskies/clockwork/server.toml"
        );

        context.registerConfig(
                ModConfig.Type.CLIENT,
                ClockworkConfigUpdater.INSTANCE.getCLIENT_SPEC(),
                "valkyrienskies/clockwork/client.toml"
        );

        modEventBus.addListener(this::onConfigLoading);
        modEventBus.addListener(this::onConfigReloading);

        if (Platform.isModLoaded("computercraft")) {
            EVENT_BUS.register(new CCTweakedForgeEvents());
        }

        EventBuses.registerModEventBus(MOD_ID, modEventBus);
        ClockworkMod.INSTANCE.getREGISTRATE().registerEventListeners(modEventBus);
        ClockworkSounds.register();
        ClockworkParticles.init();
        ClockworkBlocks.register();
        ClockworkItems.register();
        ClockworkBlockEntities.register();
        ClockworkDisplaySources.register();

        modEventBus.addListener(this::onRegister);

        ClockworkEntities.register();
        ForgeClockworkEntities.register();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.addListener((RegisterParticleProvidersEvent event) -> ClockworkParticles.initClient(event));
        });

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

        modEventBus.addListener(this::onTabModify);
        modEventBus.addListener(this::addPackFinders);

//        //todo fix forge vscore issue
//        modLoadingContext.registerExtensionPoint(
//                ConfigGuiHandler.ConfigGuiFactory.class,
//                () -> new ConfigGuiHandler.ConfigGuiFactory((minecraft, screen) -> VSClothConfig.createConfigScreenFor(screen, ClockworkConfig.class))
//        );


    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        ClockworkConfigUpdater.INSTANCE.update(event.getConfig());
    }

    private void onConfigReloading(ModConfigEvent.Reloading event) {
        ClockworkConfigUpdater.INSTANCE.update(event.getConfig());
    }

    private void onRegister(RegisterEvent evt) {
        ClockworkContraptions.init();
    }

    private void registerResourceManagers(AddReloadListenerEvent event) {

    }

    private void onTabModify(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == ClockworkMod.INSTANCE.getBASE_CREATIVE_TABINFO()) {
            event.accept(ClockworkBlocks.BALLOON_CASING.asItem());
        }
    }

    public static void init(final FMLCommonSetupEvent event) {
        event.enqueueWork(ClockworkMod::registerHeaters);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(ClockworkBlocks.GOO_BLOCK.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ClockworkBlocks.SLICKER.get(), RenderType.translucent());
    }

    // We register our TFG compat as a separate datapack for two reasons
    // 1. The user can disable it without disabling the rest of Clockworks datapacks
    // 2. It allows us to override our own recipe files when TFG is installed
    // (can't override from normal resources because can't have duplicate files merged into the jar)
    public void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }

        var modFile = ModList.get().getModFileById("vs_clockwork").getFile();
        var path = modFile.findResource("datapacks/tfg_skies");

        Pack pack = Pack.readMetaAndCreate(
                "tfg_skies",
                Component.literal("TFG Skies"),
                false,
                id -> new PathPackResources(id, path, true),
                PackType.SERVER_DATA,
                Pack.Position.TOP,
                PackSource.BUILT_IN
        );

        if (pack != null) {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
    }
}
