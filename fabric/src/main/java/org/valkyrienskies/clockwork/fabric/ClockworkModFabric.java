package org.valkyrienskies.clockwork.fabric;

import com.mojang.blaze3d.platform.Window;
import com.simibubi.create.foundation.events.ClientEvents;
import io.github.fabricators_of_create.porting_lib.event.client.KeyInputCallback;
import io.github.fabricators_of_create.porting_lib.event.client.MouseInputEvents;
import io.github.fabricators_of_create.porting_lib.event.client.RenderTickStartCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluper.BluperGlueSelectionHandler;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronHandler;
import org.valkyrienskies.clockwork.content.events.ClockworkCommonEvents;
import org.valkyrienskies.clockwork.platform.SharedValues;
import org.valkyrienskies.clockwork.platform.fabric.SharedValuesImpl;
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric;

public class ClockworkModFabric implements ModInitializer, ClientModInitializer {

    public static final GravitronHandler GRAVITRON_HANDLER = new GravitronHandler();
    public static final BluperGlueSelectionHandler BLUPER_CLUSTER_HANDLER = new BluperGlueSelectionHandler();
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
        ClockworkEntityDataSerializers.init();
        FabricClockworkFluids.register();

        ClockworkSounds.register();
        FabricClockworkSounds.prepare();

        ClockworkMod.INSTANCE.getREGISTRATE().register();

        ClockworkMod.init();
        ClockworkParticles.init();
        FabricClockworkSounds.init();
        registerServerEvents();

        ClockworkMod.INSTANCE.createCreativeTab();
        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                C_CREATIVE_TAB,
                ClockworkMod.INSTANCE.createCreativeTab()
        );
    }

    public static void registerServerEvents() {
        ServerTickEvents.START_WORLD_TICK.register(ClockworkCommonEvents.INSTANCE::onWorldTick);
    }

    @Override
    public void onInitializeClient() {
        ClockworkMod.initClient();

        ClockworkPartials.INSTANCE.init();
        FabricClockworkPartials.init();

        ClockworkParticles.initClient();

        registerClientEvents();
        FabricClockworkClientEvents.register();

        KeyInputCallback.EVENT.register(FabricClockworkInputEvents::onKeyInput);
        MouseInputEvents.BEFORE_SCROLL.register(FabricClockworkInputEvents::onMouseScrolled);
        MouseInputEvents.BEFORE_BUTTON.register(FabricClockworkInputEvents::onMouseInput);
    }

    public static void registerClientEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(FabricClockworkClientEvents::onTick);
        ClientTickEvents.START_CLIENT_TICK.register(FabricClockworkClientEvents::onTickStart);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(FabricClockworkClientEvents::onRenderWorld);
        HudRenderCallback.EVENT.register((graphics, partialTicks) -> {
            Window window = Minecraft.getInstance().getWindow();
            GRAVITRON_HANDLER.render(graphics, partialTicks, window.getWidth(), window.getHeight());
        });
    }
}
