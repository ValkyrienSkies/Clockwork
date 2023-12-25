package org.valkyrienskies.clockwork.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

public class FabricClockworkInputEvents {
    public static void onKeyInput(int key, int scancode, int action, int mods) {
        if (Minecraft.getInstance().screen != null) return;

        boolean pressed = action != 0;

        ClockworkModFabricClient.GRAVITRON_HANDLER.onKeyInput(key, pressed);
    }

    public static boolean onMouseScrolled(double delta) {
        if (Minecraft.getInstance().screen != null) return false;

        boolean cancelled = (ClockworkModFabricClient.GRAVITRON_HANDLER.mouseScrolled(delta));
        return cancelled;
    }

    public static InteractionResult onMouseInput(int button, int action, int mods) {
        if (Minecraft.getInstance().screen != null) return InteractionResult.PASS;

        boolean pressed = action == 0;

        if (ClockworkModFabricClient.GRAVITRON_HANDLER.onMouseInput(button, pressed)) return InteractionResult.CONSUME;
        return InteractionResult.PASS;
    }
}
