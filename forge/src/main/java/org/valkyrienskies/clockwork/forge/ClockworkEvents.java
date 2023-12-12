package org.valkyrienskies.clockwork.forge;

import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool.GrabTool;


@Mod.EventBusSubscriber
public class ClockworkEvents {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        GrabTool.tick(event);
    }

    @SubscribeEvent
    public static void playerLeftClick(PlayerInteractEvent.LeftClickBlock event) {

    }
}
