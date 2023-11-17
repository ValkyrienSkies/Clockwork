package org.valkyrienskies.clockwork.fabric.content.events;

import io.github.fabricators_of_create.porting_lib.util.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

public class FabricClockworkInputEvents {

    public static InteractionResult onClickInputCW(int button, int action, int mods) {
        //TODO Minecraft mc = Minecraft.getInstance();
        //TODO if (mc.screen != null)
        //TODO     return InteractionResult.PASS;
        //TODO
        //TODO int use = KeyBindingHelper.getKeyCode(mc.options.keyUse).getValue();
        //TODO int attack = KeyBindingHelper.getKeyCode(mc.options.keyAttack).getValue();
        //TODO boolean isUse = button == use;
        //TODO boolean isAttack = button == attack;

        return InteractionResult.PASS;
    }

    public static void register() {
        //MouseButtonCallback.EVENT.register(FabricClockworkInputEvents::onClickInputCW);
    }
}
