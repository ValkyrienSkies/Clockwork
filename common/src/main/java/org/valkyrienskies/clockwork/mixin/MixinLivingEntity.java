package org.valkyrienskies.clockwork.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.ClockworkItems;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "getEquipmentSlotForItem", at = @At("RETURN"), cancellable = true)
    private static void vs_clockwork$$getEquipmentSlotForItem(ItemStack item, CallbackInfoReturnable<EquipmentSlot> cir) {
        if (item.is(ClockworkItems.GAS_BANKTANK.get())) cir.setReturnValue(EquipmentSlot.CHEST);
        else cir.setReturnValue(cir.getReturnValue());
    }
}
