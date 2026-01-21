package org.valkyrienskies.clockwork.mixin.content.flap_bearing;

import com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerItem;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.LinkedControllerClientHandlerMixinStorage;
import org.valkyrienskies.clockwork.content.contraptions.flap.dual_link.DualLinkBehaviour;
import org.valkyrienskies.clockwork.platform.PlatformUtils;

import static org.valkyrienskies.clockwork.content.contraptions.flap.dual_link.DualLinkHandler.getFrontFacing;

@Mixin(LinkedControllerItem.class)
public class MixinLinkedControllerItem {
    @Shadow
    private void toggleBindMode(BlockPos pos) {}

    @Unique
    public Direction clickedFace = null;

    @Inject(
            method = "onItemUseFirst",
            at = @At("HEAD"),
            remap = false,
            cancellable = true
    )
    private void injectStateCheck(ItemStack stack, UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        // region Copied from beginning of create method
        Player player = ctx.getPlayer();
        if (player == null) return;
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState hitState = world.getBlockState(pos);
        // endregion

        if (!player.mayBuild()) return;
        if (player.isShiftKeyDown()) return;

        if (ClockworkBlocks.SMART_FLAP_BEARING.has(hitState)) {
            clickedFace = ctx.getClickedFace();

            if (!(clickedFace == getFrontFacing(hitState) || clickedFace == getFrontFacing(hitState).getOpposite())) return;

            if (world.isClientSide)
                PlatformUtils.getEnvExecutor(() -> () -> toggleBindMode(ctx.getClickedPos()));
            player.getCooldowns()
                    .addCooldown((Item)(Object)this, 2);
            cir.setReturnValue(InteractionResult.SUCCESS);
            cir.cancel();
        }

    }

    @Inject(
            method = "toggleBindMode",
            at = @At("HEAD"),
            remap = false
    )
    private void injectToggleBindMode(BlockPos pos, CallbackInfo ci) {
        LinkedControllerClientHandlerMixinStorage.face = clickedFace;
    }
}
