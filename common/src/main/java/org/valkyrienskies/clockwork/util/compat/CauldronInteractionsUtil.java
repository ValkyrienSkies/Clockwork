package org.valkyrienskies.clockwork.util.compat;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import org.valkyrienskies.clockwork.content.physicalities.motion.wing.WingBlockItem;

public class CauldronInteractionsUtil {
    public static CauldronInteraction DYED_WING = (state, world, pos, player, hand, stack) -> {
        Item item = stack.getItem();
        if (!(item instanceof WingBlockItem wing)) {
            return InteractionResult.PASS;
        } else if (!wing.hasColor(stack)) {
            return InteractionResult.PASS;
        } else {
            if (!world.isClientSide) {
                wing.clearColor(stack);
                LayeredCauldronBlock.lowerFillLevel(state, world, pos);
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        }
    };
}
