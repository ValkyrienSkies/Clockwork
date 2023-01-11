package org.valkyrienskies.clockwork.fabric.content.events;

import io.github.fabricators_of_create.porting_lib.event.common.BlockPlaceCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import org.valkyrienskies.clockwork.fabric.content.curiosities.tools.bluperglue.BluperGlueHandler;
import org.valkyrienskies.clockwork.fabric.content.curiosities.tools.bluperglue.BluperGlueItem;

public class ClockworkCommonEvents {

    public static void register() {
        UseBlockCallback.EVENT.register(BluperGlueItem::glueItemAlwaysPlacesWhenUsed);
        BlockPlaceCallback.EVENT.register(BluperGlueHandler::glueListensForBlockPlacement);
    }


}
