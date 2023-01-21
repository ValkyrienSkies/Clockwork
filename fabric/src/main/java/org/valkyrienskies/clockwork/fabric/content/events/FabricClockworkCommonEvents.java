package org.valkyrienskies.clockwork.fabric.content.events;

import com.mojang.brigadier.CommandDispatcher;
import io.github.fabricators_of_create.porting_lib.event.common.BlockPlaceCallback;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.commands.CommandSourceStack;
import org.valkyrienskies.clockwork.content.commands.AllClockworkCommands;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueHandler;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueItem;

public class FabricClockworkCommonEvents {

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, boolean dedicated) {
        AllClockworkCommands.register(dispatcher);
    }

    public static void register() {
        UseBlockCallback.EVENT.register(BluperGlueItem::glueItemAlwaysPlacesWhenUsed);
        BlockPlaceCallback.EVENT.register(BluperGlueHandler::glueListensForBlockPlacement);
        CommandRegistrationCallback.EVENT.register(FabricClockworkCommonEvents::registerCommands);
    }


}
