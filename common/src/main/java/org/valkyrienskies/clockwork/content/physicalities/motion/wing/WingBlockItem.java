package org.valkyrienskies.clockwork.content.physicalities.motion.wing;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.util.blocktype.ConnectedWingAlike;

import java.util.List;
import java.util.Set;

public class WingBlockItem extends BlockItem {
    public WingBlockItem(Block block, Properties properties) {
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

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof LayeredCauldronBlock))
            return super.useOn(context);

        return InteractionResult.PASS;
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
        ItemStack stack = context.getItemInHand();
        state.setValue(ConnectedWingAlike.COLOR, stack.hasTag() && stack.getOrCreateTag().contains("Clockwork$color") ?
                stack.getOrCreateTag().getInt("Clockwork$color") : -1);

        return super.placeBlock(context, state);
    }
}