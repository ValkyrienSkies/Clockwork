package org.valkyrienskies.clockwork.forge.content.events;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.elevator.ElevatorControlsHandler;
import com.simibubi.create.content.trains.TrainHUD;
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
    public static void onClickInput(final InputEvent.ClickInputEvent event) {
        if (Minecraft.getInstance().screen != null)
            return;

        final InteractionResult result = ClockworkInputEvents.INSTANCE.onClickInputCW(event.isUseItem(), event.isAttack());
        if (result == InteractionResult.SUCCESS) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(final InputEvent.MouseScrollEvent event) {
        if (Minecraft.getInstance().screen != null)
            return;

        final double delta = event.getScrollDelta();
        final boolean cancelled = ClockworkInputEvents.INSTANCE.onMouseScrolled(delta);
        event.setCanceled(cancelled);
    }
}
