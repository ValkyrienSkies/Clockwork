package org.valkyrienskies.clockwork.fabric.content.events;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class FabricClockworkCommonEvents {

    public static void register() {
        CommandRegistrationCallback.EVENT.register(FabricClockworkCommonEvents::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> commandSourceStackCommandDispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection commandSelection) {

    }


}
