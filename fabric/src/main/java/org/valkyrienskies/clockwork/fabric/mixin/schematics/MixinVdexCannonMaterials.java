package org.valkyrienskies.clockwork.fabric.mixin.schematics;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.cannon.SchematicannonInventory;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.valkyrienskies.clockwork.content.schematics.VdexCannonInventory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Direction;

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
    @Override public ItemStack takeMaterials(ItemRequirement requirement) {
        SchematicannonBlockEntity cannon = (SchematicannonBlockEntity) (Object) this;
        try (Transaction transaction = Transaction.openOuter()) {
            for (var req : requirement.getRequiredItems()) {
                long remaining = req.stack.getCount();
                for (Direction direction : Direction.values()) {
                    var storage = ItemStorage.SIDED.find(cannon.getLevel(), cannon.getBlockPos().relative(direction), direction.getOpposite());
                    if (storage == null) continue;
                    for (var view : storage) {
                        if (view.isResourceBlank() || !req.matches(view.getResource())) continue;
                        if (req.usage == ItemRequirement.ItemUseType.DAMAGE) {
                            ItemStack tool = view.getResource().toStack();
                            if (!tool.isDamageableItem()) continue;
                            try (Transaction nested = transaction.openNested()) {
                                if (view.extract(view.getResource(), 1, nested) != 1) continue;
                                int amount = (int) Math.min(remaining, tool.getMaxDamage() - tool.getDamageValue());
                                tool.setDamageValue(tool.getDamageValue() + amount);
                                if (tool.getDamageValue() < tool.getMaxDamage() && storage.insert(ItemVariant.of(tool), 1, nested) != 1) continue;
                                nested.commit();
                                remaining -= amount;
                            }
                        } else remaining -= view.extract(view.getResource(), remaining, transaction);
                        if (remaining == 0) break;
                    }
                    if (remaining == 0) break;
                }
                if (remaining > 0) return req.stack;
            }
            transaction.commit();
            return null;
        }
    }
}
