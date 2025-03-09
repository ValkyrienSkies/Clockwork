package org.valkyrienskies.clockwork.mixin.content.gas;

import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.ClockworkItems;

@Mixin(BacktankUtil.class)
public class MixinBacktankUtil {

    @Inject(method = "maxAir(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private static void clockwork$$maxAir(ItemStack backtank, CallbackInfoReturnable<Integer> cir) {

        if (backtank.is(ClockworkItems.GAS_BANKTANK.get())) cir.setReturnValue(Integer.MAX_VALUE);
        cir.setReturnValue(cir.getReturnValue());
    }
}
