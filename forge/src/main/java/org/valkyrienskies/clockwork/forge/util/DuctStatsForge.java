package org.valkyrienskies.clockwork.forge.util;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.util.gui.DuctStats;

import java.util.List;

public class DuctStatsForge extends DuctStats {
    public DuctStatsForge(@NotNull Block block) {
        super(block);
    }

    @Override
    public void modify(ItemTooltipEvent context) {
        List<Component> ductStats = getDuctStats(getBlock(), context.getEntity());
        if (!ductStats.isEmpty()) {
            List<Component> tooltip = context.getToolTip();
            tooltip.add(CommonComponents.EMPTY);
            tooltip.addAll(ductStats);
        }
    }
}
