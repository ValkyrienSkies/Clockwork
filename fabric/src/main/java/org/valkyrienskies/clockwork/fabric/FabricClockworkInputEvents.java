package org.valkyrienskies.clockwork.fabric;

import io.github.fabricators_of_create.porting_lib.event.client.MouseInputEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

public class FabricClockworkInputEvents {
    public static void onKeyInput(int key, int scancode, int action, int mods) {
        if (Minecraft.getInstance().screen != null) return;

        boolean pressed = action != 0;

        ClockworkModFabricClient.GRAVITRON_HANDLER.onKeyInput(key, pressed);
    }

    public static boolean onMouseScrolled(double deltaX, double deltaY) {
        if (Minecraft.getInstance().screen != null) return false;

        return ClockworkModFabricClient.GRAVITRON_HANDLER.mouseScrolled(deltaY);
    }

    public static boolean onMouseInput(int button, int mod, MouseInputEvents.Action action) {
        if (Minecraft.getInstance().screen != null) return false;

        boolean pressed = action == MouseInputEvents.Action.PRESS;

        return ClockworkModFabricClient.GRAVITRON_HANDLER.onMouseInput(button, pressed);
    }


}
