package org.valkyrienskies.clockwork.content.physicalities.motion.wing;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import javax.swing.text.AttributeSet;
import java.util.List;
import java.util.Optional;

public class WingBlockItem extends BlockItem {
    public WingBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (stack.hasTag()) {
            CompoundTag tag = stack.getOrCreateTag();
            int color = tag.getInt("Clockwork$color");
            MutableComponent comp = new TextComponent("#" + Integer.toHexString(color));
            tooltipComponents.add(comp.setStyle(Style.EMPTY.withColor(color)));
        }

        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
}
