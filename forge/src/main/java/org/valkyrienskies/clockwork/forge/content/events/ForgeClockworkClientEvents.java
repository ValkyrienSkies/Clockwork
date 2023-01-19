package org.valkyrienskies.clockwork.forge.content.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.valkyrienskies.clockwork.ClockWorkHandlers;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.content.contraptions.propellor.stream.PropStream;
import org.valkyrienskies.clockwork.forge.ClockWorkModForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;

import static com.jozufozu.flywheel.backend.Backend.isGameActive;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ForgeClockworkClientEvents {
    private static final String ITEM_PREFIX = "item." + ClockWorkMod.MOD_ID;
    private static final String BLOCK_PREFIX = "block." + ClockWorkMod.MOD_ID;

    public static void onTickStart(Minecraft client) {

        PropStream.tickClientPlayerSounds();

    }
    @SubscribeEvent
    public static void onTick(ClientTickEvent event) {
        if (!isGameActive())
            return;

        ClockWorkHandlers.tick();
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {

        @SubscribeEvent
        public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {

        }

        @SubscribeEvent
        public static void addEntityRendererLayers(EntityRenderersEvent.AddLayers event) {
            EntityRenderDispatcher dispatcher = Minecraft.getInstance()
                    .getEntityRenderDispatcher();
        }

        @SubscribeEvent
        public static void onLoadComplete(FMLLoadCompleteEvent event) {
            ModContainer createContainer = ModList.get()
                    .getModContainerById(ClockWorkMod.MOD_ID)
                    .orElseThrow(() -> new IllegalStateException("Create mod container missing on LoadComplete"));
        }

    }
}
