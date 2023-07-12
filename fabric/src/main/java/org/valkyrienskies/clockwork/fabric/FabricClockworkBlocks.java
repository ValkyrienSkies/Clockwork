package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.MaterialColor;
import org.valkyrienskies.clockwork.ClockWorkMod;


import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;
import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class FabricClockworkBlocks {

    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkMod.BASE_CREATIVE_TAB);
    }

    public static void register() {
    }
}
