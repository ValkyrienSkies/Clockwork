package org.valkyrienskies.clockwork.fabric.content.events;

import com.simibubi.create.foundation.events.InputEvents;
import io.github.fabricators_of_create.porting_lib.event.client.MouseButtonCallback;
import io.github.fabricators_of_create.porting_lib.event.client.MouseScrolledCallback;
import io.github.fabricators_of_create.porting_lib.util.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import org.valkyrienskies.clockwork.content.events.ClockworkInputEvents;

public class FabricClockworkInputEvents {
    private static InteractionResult onClickInputCW(int button, int action, int mods) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return InteractionResult.PASS;
        final int use = KeyBindingHelper.getKeyCode(mc.options.keyUse).getValue();
        final int attack = KeyBindingHelper.getKeyCode(mc.options.keyAttack).getValue();
        final boolean isUse = button == use;
        final boolean isAttack = button == attack;
        return ClockworkInputEvents.INSTANCE.onClickInputCW(isUse, isAttack);
    }

    public static void register() {
        MouseScrolledCallback.EVENT.register(ClockworkInputEvents.INSTANCE::onMouseScrolled);
        MouseButtonCallback.EVENT.register(FabricClockworkInputEvents::onClickInputCW);
    }
}
