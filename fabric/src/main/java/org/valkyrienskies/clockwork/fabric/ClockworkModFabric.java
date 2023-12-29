package org.valkyrienskies.clockwork.fabric;

import com.mojang.blaze3d.platform.Window;
import dev.architectury.event.events.common.PlayerEvent;
import io.github.fabricators_of_create.porting_lib.entity.events.living.LivingEntityEvents;
import io.github.fabricators_of_create.porting_lib.event.client.KeyInputCallback;
import io.github.fabricators_of_create.porting_lib.event.client.MouseInputEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.content.curiosities.tools.designator.AuricDesignatorClusterRenderer;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronHandler;
import org.valkyrienskies.clockwork.content.events.ClockworkCommonEvents;
import org.valkyrienskies.clockwork.fabric.config.AllClockworkConfigs;
import org.valkyrienskies.clockwork.fabric.integration.cc.ClockworkFabricPeripheralProviders;
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric;

public class ClockworkModFabric implements ModInitializer {

    ResourceKey<CreativeModeTab> C_CREATIVE_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation(ClockworkMod.MOD_ID));

    @Override
    public void onInitialize() {
        new ValkyrienSkiesModFabric().onInitialize();

        ClockworkTags.INSTANCE.init();
        ClockworkBlocks.register();
        ClockworkItems.register();

        ClockworkBlockEntities.register();

        ClockworkEntities.register();
        FabricClockworkEntities.register();
        FabricClockworkFluids.register();

        ClockworkSounds.register();
        FabricClockworkSounds.prepare();

        ClockworkMod.INSTANCE.getREGISTRATE().register();

        ClockworkMod.init();
        AllClockworkConfigs.init();

        ClockworkParticles.init();
        FabricClockworkSounds.init();
        registerServerEvents();

        if (FabricLoader.getInstance().isModLoaded("computercraft")) {
            ClockworkFabricPeripheralProviders.register();
        }


        ClockworkMod.INSTANCE.createCreativeTab();
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
