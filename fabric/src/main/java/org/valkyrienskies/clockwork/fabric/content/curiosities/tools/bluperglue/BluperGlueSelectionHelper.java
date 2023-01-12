package org.valkyrienskies.clockwork.fabric.content.curiosities.tools.bluperglue;

import com.simibubi.create.foundation.utility.Iterate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BluperGlueSelectionHelper {
    public static Set<BlockPos> searchGlueGroup(Level level, BlockPos startPos, BlockPos endPos, boolean includeOther) {
        if (endPos == null || startPos == null)
            return null;

        AABB bb = BluperGlueEntity.span(startPos, endPos);

        List<BlockPos> frontier = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> attached = new HashSet<>();
        Set<BluperGlueEntity> cachedOther = new HashSet<>();

        visited.add(startPos);
        frontier.add(startPos);

        while (!frontier.isEmpty()) {
            BlockPos currentPos = frontier.remove(0);
            attached.add(currentPos);

            for (Direction d : Iterate.directions) {
                BlockPos offset = currentPos.relative(d);
                boolean gluePresent = includeOther && BluperGlueEntity.isBluGlued(level, currentPos, d, cachedOther);

                if (!gluePresent && !bb.contains(Vec3.atCenterOf(offset)))
                    continue;
                if (!BluperGlueEntity.isValidFace(level, currentPos, d)
                        || !BluperGlueEntity.isValidFace(level, offset, d.getOpposite()))
                    continue;

                if (visited.add(offset))
                    frontier.add(offset);
            }
        }

        if (attached.size() < 2 && attached.contains(endPos))
            return null;

        return attached;
    }

    public static boolean collectGlueFromInventory(Player player, int requiredAmount, boolean simulate) {
        if (player.getAbilities().instabuild)
            return true;
        if (requiredAmount == 0)
            return true;

        NonNullList<ItemStack> items = player.getInventory().items;
        for (int i = -1; i < items.size(); i++) {
            int slot = i == -1 ? player.getInventory().selected : i;
            ItemStack stack = items.get(slot);
            if (stack.isEmpty())
                continue;
            if (!stack.isDamageableItem())
                continue;
            if (!(stack.getItem() instanceof BluperGlueItem))
                continue;

            int charges = Math.min(requiredAmount, stack.getMaxDamage() - stack.getDamageValue());

            if (!simulate)
                stack.hurtAndBreak(charges, player, i == -1 ? BluperGlueItem::onBroken : $ -> {
                });

            requiredAmount -= charges;
            if (requiredAmount <= 0)
                return true;
        }

        return false;
    }

}
