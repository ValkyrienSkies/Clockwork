package org.valkyrienskies.clockwork.mixin.content.gravitron;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.ClockworkItems;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer extends Player {

    public MixinServerPlayer(Level level, BlockPos pos, float yRot, GameProfile gameProfile, @Nullable ProfilePublicKey profilePublicKey) {
        super(level, pos, yRot, gameProfile, profilePublicKey);
    }

    @Inject(method = "swing", at = @At("HEAD"), cancellable = true)
    private void preSwing(final InteractionHand hand, final CallbackInfo ci) {
        final ItemStack itemStack = getItemInHand(hand);
        final Item item = itemStack.getItem();
        if (item == ClockworkItems.GRAVITRON.get()) {
            if (ClockworkItems.GRAVITRON.get().leftClickItem(this)) {
                ci.cancel();
            }
        }
    }
}
