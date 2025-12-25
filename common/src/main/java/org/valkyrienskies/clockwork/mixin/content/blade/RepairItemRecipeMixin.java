package org.valkyrienskies.clockwork.mixin.content.blade;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item.BladeItem;

@Mixin(RepairItemRecipe.class)
public class RepairItemRecipeMixin {

    @WrapOperation(method = "matches(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/level/Level;)Z",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    public boolean ignoreBlade(ItemStack instance, Operation<Boolean> original) {
        if (instance.getItem().getClass() == BladeItem.class) return true;
        return original.call(instance);
    }

}
