package org.valkyrienskies.clockwork.fabric;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.ClockworkMod;

import static com.jozufozu.flywheel.backend.Backend.isGameActive;

public class FabricClockworkClientEvents {

    public static void onTickStart(Minecraft client) {

    }

    public static void onTick(Minecraft client) {
        if (!isGameActive())
            return;

        ClockworkModFabric.GRAVITRON_HANDLER.tick();

        ClockworkMod.getOUTLINER().tickOutlines();
        ClockworkMod.getAURIC_OUTLINER().tickOutlines();
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FabricClockworkClientEvents::onTick);
        ClientTickEvents.START_CLIENT_TICK.register(FabricClockworkClientEvents::onTickStart);
    }

    public static void onRenderWorld(WorldRenderContext worldRenderContext) {
        PoseStack ms = worldRenderContext.matrixStack();
        ms.pushPose();
        SuperRenderTypeBuffer buffer = SuperRenderTypeBuffer.getInstance();
        float partialTicks = AnimationTickHolder.getPartialTicks();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        ClockworkMod.getOUTLINER().renderOutlines(ms, SuperRenderTypeBuffer.getInstance(), camera, partialTicks);
        ClockworkMod.getAURIC_OUTLINER().renderOutlines(ms, SuperRenderTypeBuffer.getInstance(), camera, partialTicks);

        buffer.draw();
        RenderSystem.enableCull();
        ms.popPose();
    }
}