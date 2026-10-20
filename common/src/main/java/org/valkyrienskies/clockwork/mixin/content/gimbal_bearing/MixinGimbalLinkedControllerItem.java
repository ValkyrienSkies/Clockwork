package org.valkyrienskies.clockwork.mixin.content.gimbal_bearing;

import com.simibubi.create.content.redstone.link.controller.LinkedControllerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.LinkedControllerClientHandlerMixinStorage;
import org.valkyrienskies.clockwork.platform.PlatformUtils;

@Mixin(LinkedControllerItem.class)
public class MixinGimbalLinkedControllerItem {
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
        Player player = ctx.getPlayer();
        if (player == null) return;
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState hitState = world.getBlockState(pos);

        if (!player.mayBuild()) return;
        if (player.isShiftKeyDown()) return;

        if (ClockworkBlocks.GIMBAL_BEARING.has(hitState)) {
            clickedFace = ctx.getClickedFace();
            if (world.isClientSide) {
                PlatformUtils.getEnvExecutor(() -> () -> toggleBindMode(ctx.getClickedPos()));
            }
            player.getCooldowns().addCooldown((Item) (Object) this, 2);
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
