package org.valkyrienskies.clockwork.fabric;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.github.fabricators_of_create.porting_lib.event.client.KeyInputCallback;
import io.github.fabricators_of_create.porting_lib.event.client.MouseButtonCallback;
import io.github.fabricators_of_create.porting_lib.event.client.MouseScrolledCallback;
import io.github.fabricators_of_create.porting_lib.event.client.RegisterShadersCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronHandler;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderWandClusterRenderer;

import java.io.IOException;

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
        //ClientReloadShadersEvent.EVENT.register(ClockworkModClient::onShaderReload);
        RegisterShadersCallback.EVENT.register(this::registerShaders);

        KeyInputCallback.EVENT.register(FabricClockworkInputEvents::onKeyInput);

        MouseScrolledCallback.EVENT.register(FabricClockworkInputEvents::onMouseScrolled);
        MouseButtonCallback.EVENT.register(FabricClockworkInputEvents::onMouseInput);

        BlockRenderLayerMap.INSTANCE.putBlock(ClockworkBlocks.GOO_BLOCK.get(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putItem(ClockworkBlocks.GOO_BLOCK.get().asItem(), RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putItem(ClockworkBlocks.SLICKER.get().asItem(), RenderType.translucent());
    }

    private void registerShaders(ResourceManager resourceManager, RegisterShadersCallback.ShaderRegistry shaderRegistry) throws IOException {
        shaderRegistry.registerShader(new ShaderInstance(resourceManager, "crystal", DefaultVertexFormat.NEW_ENTITY), shaderInstance -> ClockworkShaders.crystal = shaderInstance);
        shaderRegistry.registerShader(new ShaderInstance(resourceManager, "heat", DefaultVertexFormat.NEW_ENTITY), shaderInstance -> ClockworkShaders.heat = shaderInstance);
        shaderRegistry.registerShader(new ShaderInstance(resourceManager, "haze", DefaultVertexFormat.NEW_ENTITY), shaderInstance -> ClockworkShaders.haze = shaderInstance);
        //shaderRegistry.registerShader(new ShaderInstance(resourceManager, "scan_effect", DefaultVertexFormat.POSITION_TEX), shaderInstance -> ClockworkShaders.scan_effect = shaderInstance);
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
