package org.valkyrienskies.clockwork.mixin.content.casing;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.relays.elementary.ShaftBlock;
import com.simibubi.create.foundation.utility.placement.IPlacementHelper;
import com.simibubi.create.foundation.utility.placement.PlacementHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.ClockWorkBlocks;
import org.valkyrienskies.clockwork.content.contraptions.casing.ExtendedEncasedShaftBlock;


@Mixin(ShaftBlock.class)
public class MixinShaftBlock {

    @Unique
    private InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack heldItem = player.getItemInHand(hand);

        for (ExtendedEncasedShaftBlock extendedEncasedShaft : new ExtendedEncasedShaftBlock[]{ClockWorkBlocks.BALLOON_ENCASED_SHAFT.get()
        }) {
            if (!extendedEncasedShaft.getCasing().isIn(heldItem)) {
                continue;
            }
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            KineticTileEntity.switchToBlockState(level, pos, extendedEncasedShaft.defaultBlockState().setValue(ShaftBlock.AXIS, state.getValue(ShaftBlock.AXIS)));
            return InteractionResult.SUCCESS;
        }
        return null;
    }

    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z", ordinal = 1), cancellable = true)
    private void onCasingUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = onUse(state, world, pos, player, hand, ray);
        if (result != null)
            cir.setReturnValue(result);
    }
}