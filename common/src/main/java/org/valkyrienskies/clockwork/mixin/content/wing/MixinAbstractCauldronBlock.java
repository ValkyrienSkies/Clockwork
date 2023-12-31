package org.valkyrienskies.clockwork.mixin.content.wing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.content.physicalities.wing.DyedWingBlockItem;

import java.util.Map;

@Mixin(AbstractCauldronBlock.class)
public class MixinAbstractCauldronBlock {
    @Unique
    CauldronInteraction DYED_WING = (state, world, pos, player, hand, stack) -> {
        Item item = stack.getItem();
        if (!(item instanceof DyedWingBlockItem wing)) {
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

    @Shadow
    @Final
    private Map<Item, CauldronInteraction> interactions;

    @Inject(method = "use", at = @At("HEAD"))
    private void vs_clockwork$use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!this.interactions.containsKey(ClockworkBlocks.WING.get().asItem())) {
            this.interactions.put(ClockworkBlocks.WING.get().asItem(), DYED_WING);
        }
        if (!this.interactions.containsKey(ClockworkBlocks.FLAP.get().asItem())) {
            this.interactions.put(ClockworkBlocks.FLAP.get().asItem(), DYED_WING);
        }
    }
}
