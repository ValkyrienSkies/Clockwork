package org.valkyrienskies.clockwork.ponder;

import com.simibubi.create.foundation.ponder.PonderTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import org.valkyrienskies.clockwork.ClockWorkBlocks;
import org.valkyrienskies.clockwork.ClockWorkMod;

public class ClockworkPonderTags {
    public static final PonderTag

            CLOCKWORK = create("clockwork")
            .item(ClockWorkBlocks.PHYSICS_INFUSER.get(), true, false)
            .addToIndex();

    public static PonderTag create(String id) {
        return new PonderTag(new ResourceLocation(ClockWorkMod.MOD_ID, id));
    }

    public static void register() {}
}
