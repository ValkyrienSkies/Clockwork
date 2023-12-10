package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.armor.RemainingAirOverlay;
import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler;
import com.simibubi.create.content.trains.TrainHUD;
import com.simibubi.create.content.trains.track.TrackPlacementOverlay;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.clockwork.ClockworkMod;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClockworkEvents {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (Minecraft.getInstance().screen != null)
            return;

        int key = event.getKey();
        boolean pressed = !(event.getAction() == 0);

        ClockworkModForge.GRAVITRON_HANDLER.onKeyInput(key, pressed);
    }

    @SubscribeEvent
    public static void onMouseScrolled(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen != null)
            return;

        double delta = event.getScrollDelta();
        boolean cancelled = ClockworkModForge.GRAVITRON_HANDLER.mouseScrolled(delta);
        event.setCanceled(cancelled);
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        if (Minecraft.getInstance().screen != null)
            return;

        int button = event.getButton();
        boolean pressed = !(event.getAction() == 0);

        if (ClockworkModForge.GRAVITRON_HANDLER.onMouseInput(button, pressed))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        // Register overlays in reverse order
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "gravitron", ClockworkModForge.GRAVITRON_HANDLER);
    }
}
