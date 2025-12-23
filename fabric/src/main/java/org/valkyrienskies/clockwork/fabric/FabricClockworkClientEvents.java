package org.valkyrienskies.clockwork.fabric;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.ClockworkModClient;
import org.valkyrienskies.clockwork.client.render.airpocket.AirpocketRenderer;
import org.valkyrienskies.clockwork.client.render.debug.KelvinEdgeRenderer;
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautArmorLayer;
import org.valkyrienskies.clockwork.content.logistics.gas.backtank.GasBacktankArmorLayer;

import static net.createmod.ponder.PonderClient.isGameActive;

public class FabricClockworkClientEvents {

    public static void onTickStart(Minecraft client) {

    }

    public static void onTick(Minecraft client) {
        if (!isGameActive())
            return;

        ClockworkModFabricClient.GRAVITRON_HANDLER.tick();
        ClockworkModFabricClient.WANDERWAND_HANDLER.tick();

        ClockworkModClient.getOUTLINER().tickOutlines();
        ClockworkModClient.getWANDER_OUTLINER().tickOutlines();
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FabricClockworkClientEvents::onTick);
        ClientTickEvents.START_CLIENT_TICK.register(FabricClockworkClientEvents::onTickStart);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(FabricClockworkClientEvents::addEntityRendererLayers);
    }

    public static void onRenderWorld(WorldRenderContext worldRenderContext) {
        PoseStack ms = worldRenderContext.matrixStack();
        ms.pushPose();
        SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
        float partialTicks = AnimationTickHolder.getPartialTicks();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        ClockworkModClient.getOUTLINER().renderOutlines(ms, DefaultSuperRenderTypeBuffer.getInstance(), camera, partialTicks);
        ClockworkModClient.getWANDER_OUTLINER().renderOutlines(ms, DefaultSuperRenderTypeBuffer.getInstance(), camera, partialTicks);
        KelvinEdgeRenderer.render(worldRenderContext.world(), worldRenderContext.matrixStack(), worldRenderContext.camera());
        ClockworkModClient.getWANDERWAND_EFFECT_RENDERER().render(ms, buffer, camera, partialTicks);
        AirpocketRenderer.render(worldRenderContext.world(), worldRenderContext.matrixStack(), worldRenderContext.camera());

        buffer.draw();
        RenderSystem.enableCull();
        ms.popPose();
    }

    public static void addEntityRendererLayers(EntityType<? extends LivingEntity> entityType, LivingEntityRenderer<?, ?> entityRenderer,
                                               LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper registrationHelper, EntityRendererProvider.Context context) {
        GasBacktankArmorLayer.registerOn(entityRenderer, registrationHelper);
        AeronautArmorLayer.registerOn(entityRenderer, registrationHelper);
    }

}
