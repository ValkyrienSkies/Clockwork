package org.valkyrienskies.clockwork.fabric;

import com.mojang.blaze3d.platform.Window;
import io.github.fabricators_of_create.porting_lib.event.client.KeyInputCallback;
import io.github.fabricators_of_create.porting_lib.event.client.MouseInputEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronHandler;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderWandClusterRenderer;

public class ClockworkModFabricClient implements ClientModInitializer {

    public static final WanderWandClusterRenderer WANDER_HANDLER = new WanderWandClusterRenderer();
    public static final GravitronHandler GRAVITRON_HANDLER = new GravitronHandler();

    @Override
    public void onInitializeClient() {
        ClockworkModClient.initClient();

        ClockworkPartials.INSTANCE.init();
        FabricClockworkPartials.init();

        ClockworkParticles.initClient();

        registerClientEvents();
        FabricClockworkClientEvents.register();
        ClockworkShaders.INSTANCE.init();

        KeyInputCallback.EVENT.register(FabricClockworkInputEvents::onKeyInput);


        BlockRenderLayerMap.INSTANCE.putBlock(ClockworkBlocks.GOO_BLOCK.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putItem(ClockworkBlocks.GOO_BLOCK.get().asItem(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putItem(ClockworkBlocks.SLICKER.get().asItem(), RenderType.translucent());

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
