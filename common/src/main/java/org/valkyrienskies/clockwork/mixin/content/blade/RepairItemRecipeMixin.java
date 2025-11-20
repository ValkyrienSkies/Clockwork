package org.valkyrienskies.clockwork.mixin.content.blade;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item.BladeItem;

import javax.inject.Inject;

@Mixin(RepairItemRecipe.class)
public class RepairItemRecipeMixin {

    @WrapOperation(method = "matches(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/level/Level;)Z",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"))
    public Item ignoreBlade(ItemStack instance, Operation<Item> original) {
        if (instance.getDamageValue() == 0 && instance.getItem().getClass() == BladeItem.class) return ItemStack.EMPTY.getItem();
        return original.call(instance);
    }

}
