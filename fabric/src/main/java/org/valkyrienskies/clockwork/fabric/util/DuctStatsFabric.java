package org.valkyrienskies.clockwork.fabric.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.util.gui.DuctStats;

import java.util.List;

public class DuctStatsFabric extends DuctStats {

    public DuctStatsFabric(@NotNull Block block) {
        super(block);
    }

    @Override
    public void modify(ItemStack stack, Player player, TooltipFlag flags, List<Component> tooltip) {
        List<Component> ductStats = getDuctStats(this.getBlock(), player);
        if (!ductStats.isEmpty()) {
            tooltip.add(Component.empty());
            tooltip.addAll(ductStats);
        }
    }
}
