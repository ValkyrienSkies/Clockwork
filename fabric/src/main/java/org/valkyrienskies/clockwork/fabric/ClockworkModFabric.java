package org.valkyrienskies.clockwork.fabric;

import dev.architectury.platform.Platform;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraftforge.fml.config.ModConfig;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.content.events.ClockworkCommonEvents;
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric;

import static org.valkyrienskies.clockwork.fabric.RegisterPeripheralLookupKt.registerPeripheralLookup;

public class ClockworkModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        new ValkyrienSkiesModFabric().onInitialize();

        ForgeConfigRegistry.INSTANCE.register(
                ClockworkMod.MOD_ID,
                ModConfig.Type.SERVER,
                ClockworkConfigUpdater.INSTANCE.getSERVER_SPEC(),
                "valkyrienskies/clockwork/server.toml"
        );

        ForgeConfigRegistry.INSTANCE.register(
                ClockworkMod.MOD_ID,
                ModConfig.Type.CLIENT,
                ClockworkConfigUpdater.INSTANCE.getCLIENT_SPEC(),
                "valkyrienskies/clockwork/client.toml"
        );

        ModConfigEvents.reloading(ClockworkMod.MOD_ID).register (ClockworkConfigUpdater.INSTANCE::update);
        ModConfigEvents.loading(ClockworkMod.MOD_ID).register (ClockworkConfigUpdater.INSTANCE::update);

        ClockworkTags.INSTANCE.init();
        ClockworkSounds.register();
        ClockworkParticles.init();
        ClockworkBlocks.register();
        ClockworkItems.register();

        ClockworkBlockEntities.register();

        ClockworkEntities.register();
        FabricClockworkEntities.register();
        FabricClockworkFluids.register();

        ClockworkContraptions.init();


        FabricClockworkSounds.prepare();

        ClockworkMod.INSTANCE.getREGISTRATE().register();

        RegisterResourceManagers.INSTANCE.init();

        ClockworkMod.init();
        //AllClockworkConfigs.init();


        FabricClockworkSounds.init();
        registerServerEvents();

        ClockworkBoilerHeaters.INSTANCE.init();

        ItemGroupEvents.modifyEntriesEvent(ClockworkMod.INSTANCE.getBASE_CREATIVE_TABINFO()).register(content -> {
            content.addAfter(ClockworkBlocks.BALLOON_CASING.asItem(), ClockworkBlocks.IMPACT_SENSOR.asStack());
        });

        if (Platform.isModLoaded("computercraft")) {
            registerPeripheralLookup();
        }
    }

    public static void registerServerEvents() {
        ServerTickEvents.START_WORLD_TICK.register(ClockworkCommonEvents.INSTANCE::onWorldTick);
        LivingEntityEvents.LivingTickEvent.TICK.register(FabricClockworkCommonEvents::onLivingTick);
        AttackBlockCallback.EVENT.register(FabricClockworkCommonEvents::playerLeftClick);
    }
}
