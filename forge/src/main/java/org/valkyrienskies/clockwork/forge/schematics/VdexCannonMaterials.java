package org.valkyrienskies.clockwork.forge.schematics;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;

public class VdexCannonMaterials {
    private record Slot(IItemHandler handler, int index, ItemStack original, ItemStack available) {}
    private record Taken(Slot slot, ItemStack stack, boolean damaged) {}

    public static ItemStack take(SchematicannonBlockEntity cannon, Collection<LazyOptional<IItemHandler>> inventories, ItemRequirement requirement) {
        var slots = new ArrayList<Slot>();
        var seen = Collections.newSetFromMap(new IdentityHashMap<IItemHandler, Boolean>());
        for (var cap : inventories) {
            IItemHandler handler = cap.orElse(EmptyHandler.INSTANCE);
            if (!seen.add(handler)) continue;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.extractItem(slot, Integer.MAX_VALUE, true);
                if (!stack.isEmpty()) slots.add(new Slot(handler, slot, stack.copy(), stack.copy()));
            }
        }
        for (var req : requirement.getRequiredItems()) {
            int remaining = req.stack.getCount();
            for (var slot : slots) if (!slot.available.isEmpty() && req.matches(slot.available)) {
                if (req.usage == ItemRequirement.ItemUseType.DAMAGE) {
                    if (!slot.available.isDamageableItem()) continue;
                    int amount = Math.min(remaining, slot.available.getMaxDamage() - slot.available.getDamageValue());
                    slot.available.setDamageValue(slot.available.getDamageValue() + amount);
                    remaining -= amount;
                    if (slot.available.getDamageValue() >= slot.available.getMaxDamage()) slot.available.shrink(1);
                } else {
                    int amount = Math.min(remaining, slot.available.getCount());
                    slot.available.shrink(amount); remaining -= amount;
                }
                if (remaining == 0) break;
            }
            if (remaining > 0) return req.stack;
        }
        var taken = new ArrayList<Taken>();
        for (var slot : slots) {
            boolean damaged = !slot.available.isEmpty() && !ItemStack.isSameItemSameTags(slot.original, slot.available);
            int amount = damaged ? slot.original.getCount() : slot.original.getCount() - slot.available.getCount();
            if (amount == 0) continue;
            ItemStack extracted = slot.handler.extractItem(slot.index, amount, false);
            taken.add(new Taken(slot, extracted, damaged));
            if (extracted.getCount() != amount || !ItemStack.isSameItemSameTags(extracted, slot.original)) {
                for (var entry : taken) giveBack(cannon, entry.slot, entry.stack);
                return slot.original;
            }
        }
        for (var entry : taken) if (entry.damaged) giveBack(cannon, entry.slot, entry.slot.available.copy());
        return null;
    }

    private static void giveBack(SchematicannonBlockEntity cannon, Slot slot, ItemStack stack) {
        ItemStack rest = slot.handler.insertItem(slot.index, stack, false);
        if (!rest.isEmpty()) Block.popResource(cannon.getLevel(), cannon.getBlockPos(), rest);
    }
}
