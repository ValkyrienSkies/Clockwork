package org.valkyrienskies.clockwork.forge.content.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.util.render.BoltUtil;

import static net.createmod.ponder.PonderClient.isGameActive;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ForgeClockworkClientEvents {
    private static final String ITEM_PREFIX = "item." + ClockworkMod.MOD_ID;
    private static final String BLOCK_PREFIX = "block." + ClockworkMod.MOD_ID;

    public static void onTickStart(Minecraft client) {

    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent event) {
        if (!isGameActive()) {
            return;
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!isGameActive()) {
            return;
        }

        BoltUtil.INSTANCE.tick();
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
                    .getModContainerById(ClockworkMod.MOD_ID)
                    .orElseThrow(() -> new IllegalStateException("Create mod container missing on LoadComplete"));
        }

    }
}
