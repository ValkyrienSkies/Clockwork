package org.valkyrienskies.clockwork.fabric;

import com.mojang.blaze3d.platform.Window;
import io.github.fabricators_of_create.porting_lib.event.client.KeyInputCallback;
import io.github.fabricators_of_create.porting_lib.event.client.MouseButtonCallback;
import io.github.fabricators_of_create.porting_lib.event.client.MouseScrolledCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronHandler;

public class ClockworkModFabricClient implements ClientModInitializer {
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

        MouseScrolledCallback.EVENT.register(FabricClockworkInputEvents::onMouseScrolled);
        MouseButtonCallback.EVENT.register(FabricClockworkInputEvents::onMouseInput);
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
