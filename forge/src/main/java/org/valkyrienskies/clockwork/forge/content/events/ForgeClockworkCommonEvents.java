package org.valkyrienskies.clockwork.forge.content.events;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.clockwork.content.commands.AllClockworkCommands;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueHandler;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueItem;
@Mod.EventBusSubscriber
public class ForgeClockworkCommonEvents {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        AllClockworkCommands.register(event.getDispatcher());
    }

}
