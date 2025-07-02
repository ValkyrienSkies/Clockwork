package org.valkyrienskies.clockwork.forge;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.armor.BacktankArmorLayer;
import com.simibubi.create.content.trains.schedule.TrainHatArmorLayer;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.clockwork.ClockworkModClient;
import org.valkyrienskies.clockwork.client.render.debug.KelvinEdgeRenderer;
import org.valkyrienskies.clockwork.content.logistics.gas.backtank.GasBacktankArmorLayer;
import org.valkyrienskies.clockwork.forge.content.logistics.gas.backtank.ForgeGasBacktankArmorLayer;

import static com.jozufozu.flywheel.backend.Backend.isGameActive;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClockworkClientEvents {

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "Gravitron",
                ClockworkModForgeClient.GRAVITRON_HANDLER.getOverlayRenderer());
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }



        PoseStack ms = event.getPoseStack();

        assert Minecraft.getInstance().level != null;
        KelvinEdgeRenderer.render(Minecraft.getInstance().level, event.getPoseStack(), event.getCamera());

        ms.pushPose();
        SuperRenderTypeBuffer buffer = SuperRenderTypeBuffer.getInstance();
        float partialTicks = AnimationTickHolder.getPartialTicks();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera()
                .getPosition();

        ClockworkModClient.getOUTLINER().renderOutlines(ms, SuperRenderTypeBuffer.getInstance(), camera, partialTicks);
        ClockworkModClient.getWANDER_OUTLINER().renderOutlines(ms, SuperRenderTypeBuffer.getInstance(), camera, partialTicks);

        buffer.draw();
        RenderSystem.enableCull();
        ms.popPose();
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (!isGameActive()) {
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        ClockworkModForgeClient.GRAVITRON_HANDLER.tick();

        ClockworkModClient.getOUTLINER().tickOutlines();
        ClockworkModClient.getWANDER_OUTLINER().tickOutlines();
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        int key = event.getKey();
        boolean pressed = !(event.getAction() == 0);

        ClockworkModForgeClient.GRAVITRON_HANDLER.onKeyInput(key, pressed);
    }

    @SubscribeEvent
    public static void onMouseScrolled(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        double delta = event.getScrollDelta();
        boolean cancelled = ClockworkModForgeClient.GRAVITRON_HANDLER.mouseScrolled(delta);
        event.setCanceled(cancelled);
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        int button = event.getButton();
        boolean pressed = !(event.getAction() == 0);

        if (ClockworkModForgeClient.GRAVITRON_HANDLER.onMouseInput(button, pressed)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void addEntityRendererLayers(EntityRenderersEvent.AddLayers event) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance()
                .getEntityRenderDispatcher();
        ForgeGasBacktankArmorLayer.registerOnAll(dispatcher);
    }

}
