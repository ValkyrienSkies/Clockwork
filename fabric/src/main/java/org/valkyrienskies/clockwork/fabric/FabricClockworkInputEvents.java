package org.valkyrienskies.clockwork.fabric;

import io.github.fabricators_of_create.porting_lib.event.client.MouseInputEvents;
import net.minecraft.client.Minecraft;

public class FabricClockworkInputEvents {
    public static void onKeyInput(int key, int scancode, int action, int mods) {
        if (Minecraft.getInstance().screen != null) return;

        boolean pressed = action != 0;

        ClockworkModFabric.GRAVITRON_HANDLER.onKeyInput(key, pressed);
    }

    public static boolean onMouseScrolled(double deltaX, double delta) {
        if (Minecraft.getInstance().screen != null) return false;

        var cancelled = (ClockworkModFabric.GRAVITRON_HANDLER.mouseScrolled(delta));
        return cancelled;
    }

    public static boolean onMouseInput(int button, int modifiers, MouseInputEvents.Action action) {
        if (Minecraft.getInstance().screen != null) return false;

        var pressed = action == MouseInputEvents.Action.PRESS;

        if (ClockworkModFabric.GRAVITRON_HANDLER.onMouseInput(button, pressed)) return true;
        return false;
    }
}
