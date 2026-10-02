package org.valkyrienskies.clockwork.mixin.content.gravitron;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.ClockworkItems;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer extends AbstractClientPlayer {


    public MixinLocalPlayer(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Inject(method = "swing", at = @At("HEAD"), cancellable = true)
    private void vs_clockwork$preSwing(final InteractionHand hand, final CallbackInfo ci) {
        final ItemStack itemStack = getItemInHand(hand);
        final Item item = itemStack.getItem();
        if (item == ClockworkItems.GRAVITRON.get() || item == ClockworkItems.CREATIVE_GRAVITRON.get()) {
            ((MinecraftAccessor) Minecraft.getInstance()).setMissTime(10);
            // Successful server actions drive recoil; vanilla attack swings fight that motion.
            ci.cancel();
        }
    }
}
