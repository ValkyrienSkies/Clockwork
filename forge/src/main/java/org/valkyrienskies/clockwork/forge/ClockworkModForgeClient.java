package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.AllParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.ClockworkModClient;
import org.valkyrienskies.clockwork.ClockworkPartials;
import org.valkyrienskies.clockwork.ClockworkShaders;
import org.valkyrienskies.clockwork.forge.content.curiosities.tools.aeronaut.ForgeAeronautArmorLayer;
import org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron.ForgeGravitronHandler;
import org.valkyrienskies.clockwork.forge.content.curiosities.tools.wanderwand.ForgeWanderwandHandler;
import org.valkyrienskies.clockwork.forge.content.logistics.gas.backtank.ForgeGasBacktankArmorLayer;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = ClockworkMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClockworkModForgeClient {

    public static final ForgeGravitronHandler GRAVITRON_HANDLER = new ForgeGravitronHandler();
    public static final ForgeWanderwandHandler WANDERWAND_HANDLER = new ForgeWanderwandHandler();
    //public static final WanderWandClusterRenderer WANDER_HANDLER = new WanderWandClusterRenderer();

    public static void onCtorClient(IEventBus modEventBus) {
        ClockworkPartials.INSTANCE.init();
        ClockworkModClient.initClient();
        ClockworkShaders.INSTANCE.init();
        modEventBus.addListener(AllParticleTypes::registerFactories);

        //ClientReloadShadersEvent.EVENT.register(ClockworkModForgeClient::onShaderReload);
    }

    @SubscribeEvent
    public static void addEntityRendererLayers(EntityRenderersEvent.AddLayers event) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance()
                .getEntityRenderDispatcher();
        ForgeGasBacktankArmorLayer.registerOnAll(dispatcher);
        ForgeAeronautArmorLayer.registerOnAll(dispatcher);
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "gravitron",
                ClockworkModForgeClient.GRAVITRON_HANDLER.getOverlayRenderer());
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "wanderwand",
                ClockworkModForgeClient.WANDERWAND_HANDLER.getOverlayRenderer());
    }

}
