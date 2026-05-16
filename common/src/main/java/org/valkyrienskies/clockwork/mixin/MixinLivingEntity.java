package org.valkyrienskies.clockwork.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.ClockworkItems;
import org.valkyrienskies.clockwork.ClockworkTags;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "getEquipmentSlotForItem", at = @At("RETURN"), cancellable = true)
    private static void vs_clockwork$$getEquipmentSlotForItem(ItemStack item, CallbackInfoReturnable<EquipmentSlot> cir) {
        if (item.is(ClockworkItems.GAS_BANKTANK.get())) cir.setReturnValue(EquipmentSlot.CHEST);
        else cir.setReturnValue(cir.getReturnValue());
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
    private void vs_clockwork$$preSwing(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack itemInHand = self.getItemInHand(hand);
        //ts dont work imma manually do it
        if (itemInHand.is(ClockworkTags.AllItemTags.DISABLE_SWING_ANIMATION.getTag())) {
            ci.cancel();
        }

        //temporary
        if (itemInHand.is(ClockworkItems.GRAVITRON.asItem()) || itemInHand.is(ClockworkItems.CREATIVE_GRAVITRON.asItem())) {
            ci.cancel();
        }
        if (itemInHand.is(ClockworkItems.HANDHELD_DRILL.asItem())) {
            ci.cancel();
        }
        if (itemInHand.is(ClockworkItems.HANDHELD_SAW.asItem())) {
            ci.cancel();
        }
    }
}
