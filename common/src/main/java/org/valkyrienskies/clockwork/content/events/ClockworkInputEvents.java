package org.valkyrienskies.clockwork.content.events;


import io.github.fabricators_of_create.porting_lib.util.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric_designator.AreaDesignatorItem;

public class ClockworkInputEvents {

    public static InteractionResult onClickInputCW(int button, int action, int mods) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null)
            return InteractionResult.PASS;

        int use = KeyBindingHelper.getKeyCode(mc.options.keyUse).getValue();
        int attack = KeyBindingHelper.getKeyCode(mc.options.keyAttack).getValue();
        boolean isUse = button == use;
        boolean isAttack = button == attack;

        if (isAttack) {
            if (Minecraft.getInstance().player != null) {
                Player player = Minecraft.getInstance().player;
                if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof AreaDesignatorItem) {
                    AreaDesignatorItem item = (AreaDesignatorItem) player.getItemInHand(InteractionHand.MAIN_HAND).getItem();
                    item.onAttack(player);
                    return InteractionResult.PASS;
                }
            }
        }

        return InteractionResult.PASS;
    }
}
