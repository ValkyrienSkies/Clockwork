package org.valkyrienskies.clockwork.forge.content.events;


import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.clockwork.ClockWorkHandlers;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.forge.ClockWorkModForge;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ForgeClockworkInputEvents {

    @SubscribeEvent
    public static InteractionResult onClickInput(InputEvent.ClickInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null)
            return InteractionResult.PASS;

        KeyMapping key = event.getKeyMapping();

        if (key == mc.options.keyUse || key == mc.options.keyAttack) {
            if (ClockWorkHandlers.BLUPER_HANDLER.onMouseInput(key == mc.options.keyAttack))
                event.setCanceled(true);
        }
        return InteractionResult.PASS;
    }
}
