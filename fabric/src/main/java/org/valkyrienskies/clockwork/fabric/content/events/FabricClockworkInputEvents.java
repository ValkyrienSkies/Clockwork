package org.valkyrienskies.clockwork.fabric.content.events;


import io.github.fabricators_of_create.porting_lib.event.client.MouseButtonCallback;
import io.github.fabricators_of_create.porting_lib.util.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import org.valkyrienskies.clockwork.ClockWorkHandlers;

public class FabricClockworkInputEvents {

    public static InteractionResult onClickInputCW(int button, int action, int mods) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null)
            return InteractionResult.PASS;

        int use = KeyBindingHelper.getKeyCode(mc.options.keyUse).getValue();
        int attack = KeyBindingHelper.getKeyCode(mc.options.keyAttack).getValue();
        boolean isUse = button == use;
        boolean isAttack = button == attack;

        return InteractionResult.PASS;
    }

    public static void register() {
        MouseButtonCallback.EVENT.register(FabricClockworkInputEvents::onClickInputCW);
    }
}
