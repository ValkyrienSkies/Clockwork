package org.valkyrienskies.clockwork.util.blocktype;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;
import org.valkyrienskies.clockwork.content.materials.solids.colorblock.ColorBlockEntity;
import org.valkyrienskies.clockwork.content.physicalities.wing.DyedWingBlockItem;

import java.util.List;

public abstract class DyedWing extends ConnectedWingAlike implements IBE<ColorBlockEntity> {
    public DyedWing(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, @NotNull Level level, @NotNull BlockPos pos, Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        ColorBlockEntity be = (ColorBlockEntity) level.getBlockEntity(pos);
        assert be != null;
        int color = be.getColor();

        if (stack.getItem() instanceof DyeItem dye && color != dye.getDyeColor().getTextColor()) {
            be.setColor(color == -1 ? dye.getDyeColor().getTextColor() :
                    Color.mixColors(color, dye.getDyeColor().getTextColor(), 0.5f));

            if (!level.isClientSide && !player.isCreative()) {
                if (stack.getCount() > 1)
                    stack.shrink(1);
                else if (stack.getCount() == 1)
                    player.setItemInHand(hand, ItemStack.EMPTY);
            }

            return InteractionResult.SUCCESS;
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public Class<ColorBlockEntity> getBlockEntityClass() {
        return ColorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ColorBlockEntity> getBlockEntityType() {
        return ClockWorkBlockEntities.COLOR_BLOCK_ENTITY.get();
    }

    @Override
    public List<ItemStack> getDrops(@NotNull BlockState state, LootContext.@NotNull Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);

        drops.replaceAll(stack -> {
            ColorBlockEntity be = (ColorBlockEntity) builder.getParameter(LootContextParams.BLOCK_ENTITY);
            int color = be.getColor();
            if ((stack.getItem() instanceof DyedWingBlockItem) && color != -1)
                stack.getOrCreateTag().putInt("Clockwork$color", color);
            return stack;
        });

        return drops;
    }



    @Override
    public ItemStack getCloneItemStack(@NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        ColorBlockEntity be = (ColorBlockEntity) level.getBlockEntity(pos);
        assert be != null;
        int color = be.getColor();

        if (color != -1) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt("Clockwork$color", color);
        }

        return stack;
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }
}
