package org.valkyrienskies.clockwork.ponder;

import com.simibubi.create.foundation.ponder.PonderTag;
import net.minecraft.resources.ResourceLocation;
import org.valkyrienskies.clockwork.ClockWorkBlocks;
import org.valkyrienskies.clockwork.ClockWorkMod;

public class ClockworkPonderTags {
    public static final PonderTag

            COMBUSTION_ENGINE = create("combustion_engine")
            .item(ClockWorkBlocks.COMBUSTION_ENGINE.get(), true, false)
            .defaultLang("Hear the Purr!", """
                    How to use the Combustion Engine
                    Original Ponders Made by West (wesston) in Discord
                    Adapted By TechTastic"""
            )
            .addToIndex();

    /*MUNITIONS = create("munitions")
            .item(CBCBlocks.SOLID_SHOT.get(), true, false)
            .defaultLang("Munitions", "Blocks that make up cannon loads, and what they can do")
            .addToIndex(),

    CANNON_CRAFTING = create("cannon_crafting")
            .item(CBCBlocks.CASTING_SAND.get(), true, false)
            .defaultLang("Cannon Crafting", "How to manufacture big cannons")
            .addToIndex();*/

    public static PonderTag create(String id) {
        return new PonderTag(new ResourceLocation(ClockWorkMod.MOD_ID, id));
    }

    public static void register() {}
}
