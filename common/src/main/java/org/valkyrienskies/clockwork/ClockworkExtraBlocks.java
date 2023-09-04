package org.valkyrienskies.clockwork;

import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MaterialColor;

import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

/**
 * For blocks that Kotlin no likey register.
 */
public class ClockworkExtraBlocks {
    static {
        ClockworkMod.INSTANCE.getREGISTRATE().creativeModeTab(ClockworkMod.INSTANCE::getBASE_CREATIVE_TAB);
    }

    public static final BlockEntry<CasingBlock> BALLOON_CASING = ClockworkMod.INSTANCE.getREGISTRATE().block("balloon_casing", CasingBlock::new)
            .properties(p -> p.color(MaterialColor.WOOL))
            .properties(p -> p.sound(SoundType.BAMBOO))
            .transform(BuilderTransformers.casing(ClockworkSpriteShifts.INSTANCE::getBALLOON_CASING))
            .transform(axeOrPickaxe())
            .register();

    public static void register() {}
}
