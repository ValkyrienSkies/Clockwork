package org.valkyrienskies.clockwork.fabric.content.events;

import io.github.fabricators_of_create.porting_lib.util.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

public class FabricClockworkInputEvents {

    public static InteractionResult onClickInputCW(int button, int action, int mods) {
        /*
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null)
             return InteractionResult.PASS;

         int use = KeyBindingHelper.getKeyCode(mc.options.keyUse).getValue();
         int attack = KeyBindingHelper.getKeyCode(mc.options.keyAttack).getValue();
         boolean isUse = button == use;
         boolean isAttack = button == attack;


         */
        return InteractionResult.PASS;
    }

    public static void register() {
        //ScreenMouseEvents.BeforeMouseClick.EVENT.register(FabricClockworkInputEvents::onClickInputCW);
    }
}
