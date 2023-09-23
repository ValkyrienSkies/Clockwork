package org.valkyrienskies.clockwork.fabric.content.events;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.commands.CommandSourceStack;

public class FabricClockworkCommonEvents {

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, boolean dedicated) {
        //AllClockworkCommands.register(dispatcher);
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(FabricClockworkCommonEvents::registerCommands);
    }


}
