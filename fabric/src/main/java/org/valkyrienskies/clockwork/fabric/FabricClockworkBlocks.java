package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.MaterialColor;
import org.valkyrienskies.clockwork.ClockworkMod;


import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

public class FabricClockworkBlocks {

    static {
        ClockworkMod.INSTANCE.getREGISTRATE().creativeModeTab(ClockworkMod.INSTANCE::getBASE_CREATIVE_TAB);
    }

    public static void register() {
    }
}
