package org.valkyrienskies.clockwork.forge.content.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.clockwork.content.events.ClockworkCommonEvents;

@Mod.EventBusSubscriber
public class ForgeClockworkCommonEvents {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        //AllClockworkCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world.isClientSide()) {
            return;
        }
        ClockworkCommonEvents.INSTANCE.onWorldTick((ServerLevel) event.world);
    }

}
