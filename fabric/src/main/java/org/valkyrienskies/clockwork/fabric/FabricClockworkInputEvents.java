package org.valkyrienskies.clockwork.fabric;

import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrClient;
import org.valkyrienskies.clockwork.content.schematics.VdexPlacementClient;

import io.github.fabricators_of_create.porting_lib.event.client.MouseInputEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

public class FabricClockworkInputEvents {
    public static void onKeyInput(int key, int scancode, int action, int mods) {
        if (Minecraft.getInstance().screen != null) return;

        boolean pressed = action != 0;

        GatemakyrClient.onKeyInput(key, scancode, pressed);
        ClockworkModFabricClient.GRAVITRON_HANDLER.onKeyInput(key, pressed);
        ClockworkModFabricClient.WANDERWAND_HANDLER.onKeyInput(key, pressed);
    }

    public static boolean onMouseScrolled(double deltaX, double deltaY) {
        if (Minecraft.getInstance().screen != null) return false;

        return GatemakyrClient.scroll(deltaY) || VdexPlacementClient.mouseScrolled(deltaY) || ClockworkModFabricClient.GRAVITRON_HANDLER.mouseScrolled(deltaY) || ClockworkModFabricClient.WANDERWAND_HANDLER.mouseScrolled(deltaY);
    }

    public static boolean onMouseInput(int button, int mod, MouseInputEvents.Action action) {
        if (Minecraft.getInstance().screen != null) return false;

        boolean pressed = action == MouseInputEvents.Action.PRESS;

        return GatemakyrClient.onMouseInput(button, pressed) || VdexPlacementClient.onMouseInput(button, pressed) || ClockworkModFabricClient.GRAVITRON_HANDLER.onMouseInput(button, pressed) || ClockworkModFabricClient.WANDERWAND_HANDLER.onMouseInput(button, pressed);
    }


}
