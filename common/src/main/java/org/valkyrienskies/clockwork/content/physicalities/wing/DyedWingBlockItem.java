package org.valkyrienskies.clockwork.content.physicalities.wing;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;
import org.valkyrienskies.clockwork.content.materials.solids.colorblock.ColorBlockEntity;

import java.util.List;
import java.util.Set;

public class DyedWingBlockItem extends BlockItem {
    public DyedWingBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (stack.hasTag()) {
            CompoundTag tag = stack.getOrCreateTag();
            int color = tag.getInt("Clockwork$color");
            MutableComponent comp = new TextComponent("#" + Integer.toHexString(color).toUpperCase());
            tooltipComponents.add(comp.setStyle(Style.EMPTY.withColor(color)));
        }

        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

    public boolean hasColor(ItemStack stack) {
        return stack.hasTag() && stack.getOrCreateTag().contains("Clockwork$color") && stack.getOrCreateTag().getInt("Clockwork$color") != -1;
    }

    public void clearColor(ItemStack stack) {
        if (!stack.hasTag())
            return;

        CompoundTag tag = stack.getOrCreateTag();
        Set<String> keys = tag.getAllKeys();
        keys.remove("Clockwork$color");
        if (keys.size() > 0)
            tag.remove("Clockwork$color");
        else
            stack.setTag(null);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        boolean result = super.placeBlock(context, state);

        if (result) {
            ItemStack stack = context.getItemInHand();
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();

            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) {
                be = new ColorBlockEntity(ClockWorkBlockEntities.COLOR_BLOCK_ENTITY.get(), pos, state);
                level.setBlockEntity(be);
            }
            ColorBlockEntity color = (ColorBlockEntity) be;

            color.setColor(stack.hasTag() && stack.getOrCreateTag().contains("Clockwork$color") ?
                    stack.getOrCreateTag().getInt("Clockwork$color") : -1);
        }

        return result;
    }
}