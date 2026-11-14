package org.valkyrienskies.clockwork.fabric.mixin.schematics;

import com.simibubi.create.content.schematics.cannon.SchematicannonInventory;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.schematics.VdexBlueprintItem;

@Mixin(value = SchematicannonInventory.class, remap = false)
public class MixinVdexCannonInventory {
    @Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true)
    private void clockwork$blueprint(int slot, ItemVariant stack, int count, CallbackInfoReturnable<Boolean> cir) {
        if (slot == 0 && stack.getItem() instanceof VdexBlueprintItem) cir.setReturnValue(true);
    }
}
