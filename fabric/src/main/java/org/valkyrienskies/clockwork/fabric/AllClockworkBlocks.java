package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.AllSections;
import com.simibubi.create.foundation.block.BlockStressDefaults;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.material.MaterialColor;
import org.valkyrienskies.clockwork.fabric.content.contraptions.components.propellor.PropellorBearingBlock;

import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static org.valkyrienskies.clockwork.fabric.ClockWorkModFabric.REGISTRATE;

public class AllClockworkBlocks {

    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkModFabric.BASE_CREATIVE_TAB);
    }

    static {
        REGISTRATE.startSection(AllSections.KINETICS);
    }

    //////// Propellor Bearing ////////

    public static final BlockEntry<PropellorBearingBlock> PROPELLOR_BEARING =
            REGISTRATE.block("propellor_bearing", PropellorBearingBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.PODZOL))
                    .transform(BuilderTransformers.bearing("propellor", "gearbox", true))
                    .transform(BlockStressDefaults.setImpact(12.0))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .register();



    public static void register() {}
}
