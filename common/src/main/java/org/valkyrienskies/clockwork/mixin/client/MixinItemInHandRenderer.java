package org.valkyrienskies.clockwork.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.ClockworkItems;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private ItemStack mainHandItem;

    @Shadow
    private ItemStack offHandItem;

    @Inject(method = "tick", at = @At("HEAD"))
    private void clockwork$gravitonCancelNbtUpdateAnim(CallbackInfo ci) {
        ItemStack newMainStack = minecraft.player.getMainHandItem();
        if (mainHandItem.getItem() == newMainStack.getItem()) {
            if (newMainStack.is(ClockworkItems.GRAVITRON.asItem())) {
                mainHandItem = newMainStack;
            }
        }

        ItemStack newOffStack = minecraft.player.getOffhandItem();

        if (offHandItem.getItem() == newOffStack.getItem()) {
            if (newOffStack.is(ClockworkItems.GRAVITRON.asItem())) {
                offHandItem = newOffStack;
            }
        }
    }
}
