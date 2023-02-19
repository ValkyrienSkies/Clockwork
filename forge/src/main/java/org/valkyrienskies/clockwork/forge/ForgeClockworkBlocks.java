package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.content.AllSections;
import com.simibubi.create.foundation.block.BlockStressDefaults;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.material.MaterialColor;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.forge.content.contraptions.combustion_engine.ForgeCombustionEngineBlock;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;
import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class ForgeClockworkBlocks {

    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkMod.BASE_CREATIVE_TAB);
    }

    //////// Propellor Bearing ////////

    static {
        REGISTRATE.startSection(AllSections.KINETICS);
    }

    public static final BlockEntry<ForgeCombustionEngineBlock> COMBUSTION_ENGINE =
            REGISTRATE.block("combustion_engine", ForgeCombustionEngineBlock::new)
                    .initialProperties(SharedProperties::copperMetal)
                    .properties(p -> p.color(MaterialColor.COLOR_ORANGE))
                    .transform(pickaxeOnly())
                    .blockstate((c, p) -> p.horizontalFaceBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static void register() {
    }
}
