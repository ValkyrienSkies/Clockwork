package org.valkyrienskies.clockwork.forge.content.events;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ForgeClockworkCommonEvents {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        //AllClockworkCommands.register(event.getDispatcher());
    }

}
