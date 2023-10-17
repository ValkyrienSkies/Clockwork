package org.valkyrienskies.clockwork.forge.content.events;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.content.events.ClockworkInputEvents;

@Mod.EventBusSubscriber(modid = ClockworkMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ForgeClockworkInputEvents {

    @SubscribeEvent
    public static void onClickInput(InputEvent.ClickInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null)
            return;

        final InteractionResult result = ClockworkInputEvents.INSTANCE.onClickInputCW(event.isUseItem(), event.isAttack());
        if (result == InteractionResult.SUCCESS) {
            event.setCanceled(true);
        }
    }
}
