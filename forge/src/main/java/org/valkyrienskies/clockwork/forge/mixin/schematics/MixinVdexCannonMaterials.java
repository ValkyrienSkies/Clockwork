package org.valkyrienskies.clockwork.forge.mixin.schematics;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.cannon.SchematicannonInventory;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.valkyrienskies.clockwork.content.schematics.VdexCannonInventory;
import java.util.LinkedHashSet;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.valkyrienskies.clockwork.forge.schematics.VdexCannonMaterials;

@Mixin(value = SchematicannonBlockEntity.class, remap = false)
public abstract class MixinVdexCannonMaterials implements VdexCannonInventory {
    @Shadow public SchematicannonInventory inventory;
    @Override public ItemStack blueprint() { return inventory.getStackInSlot(0); }
    @Override public boolean canFinishBlueprint() { return inventory.getStackInSlot(1).isEmpty(); }
    @Override public boolean finishBlueprint() {
        if (!inventory.getStackInSlot(1).isEmpty()) return false;
        ItemStack paper = inventory.getStackInSlot(0);
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        inventory.setStackInSlot(1, paper);
        return true;
    }
    @Shadow public LinkedHashSet<LazyOptional<IItemHandler>> attachedInventories;
    @Override public ItemStack takeMaterials(ItemRequirement requirement) {
        return VdexCannonMaterials.take((SchematicannonBlockEntity) (Object) this, attachedInventories, requirement);
    }
}
