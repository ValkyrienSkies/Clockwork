package org.valkyrienskies.clockwork.fabric;

import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.content.events.ClockworkCommonEvents;
import org.valkyrienskies.clockwork.fabric.config.AllClockworkConfigs;
import org.valkyrienskies.clockwork.fabric.integration.cc.ClockworkFabricPeripheralProviders;
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric;

public class ClockworkModFabric implements ModInitializer {


   ResourceKey<CreativeModeTab> C_CREATIVE_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation(ClockworkMod.MOD_ID));

    @Override
    public void onInitialize() {
        new ValkyrienSkiesModFabric().onInitialize();

        AllClockworkConfigs.init();

        ClockworkTags.INSTANCE.init();
        ClockworkBlocks.register();
        ClockworkItems.register();

        ClockworkBlockEntities.register();
        FabricClockworkBlockEntities.register();

        ClockworkEntities.register();
        FabricClockworkEntities.register();
        FabricClockworkFluids.register();

        ClockworkSounds.register();
        FabricClockworkSounds.prepare();

        ClockworkMod.INSTANCE.getREGISTRATE().register();

        ClockworkMod.init();
        FabricClockworkWorldgen.bootstrap();


        ClockworkParticles.init();
        FabricClockworkSounds.init();
        registerServerEvents();

        if (FabricLoader.getInstance().isModLoaded("computercraft")) {
            ClockworkFabricPeripheralProviders.register();
        }

        var gearwork = new ResourceLocation(ClockworkMod.MOD_ID, "gearwork");
        FabricLoader.getInstance().getModContainer(ClockworkMod.MOD_ID).ifPresent(container -> ResourceManagerHelper.registerBuiltinResourcePack(gearwork, container, "Clockwork: Gearwork", ResourcePackActivationType.NORMAL));



        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                C_CREATIVE_TAB,
                ClockworkMod.INSTANCE.createCreativeTab()
        );
    }

    public static void registerServerEvents() {
        ServerTickEvents.START_WORLD_TICK.register(ClockworkCommonEvents.INSTANCE::onWorldTick);
        LivingEntityEvents.TICK.register(FabricClockworkCommonEvents::onLivingTick);
        AttackBlockCallback.EVENT.register(FabricClockworkCommonEvents::playerLeftClick);
    }
}
