package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.data.ModelGen;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.MaterialColor;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.fabric.content.physicalities.motion.wing.WingBlock;

import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class FabricClockworkBlocks {

    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkMod.BASE_CREATIVE_TAB);
    }

    /////// WINX CLUB //////
    public static final BlockEntry<WingBlock> WING =
            REGISTRATE.block("wing", WingBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.TERRACOTTA_WHITE))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
                    .item()
                    .transform(ModelGen.customItemModel("wing", "item"))
                    .register();

    public static void register() {}
}
