package org.valkyrienskies.clockwork.content.materials.solids.soda;

import com.simibubi.create.AllBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class SodaBottleItem extends Item {
    public SodaBottleItem(Properties properties) {
        super(properties);
    }

//    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
//        ItemStack itemStack = player.getItemInHand(usedHand);
//        HitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
//        if (hitResult.getType() == HitResult.Type.MISS) {
//            return InteractionResultHolder.pass(itemStack);
//        } else {
//            if (hitResult.getType() == HitResult.Type.BLOCK) {
//                BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
//                if (level.getBlockState(blockPos).is(AllBlocks.FLUID_TANK.get())) {
//
//                }
//            }
//            return InteractionResultHolder.pass(itemStack);
//        }
//    }

//    protected ItemStack turnBottleIntoItem(ItemStack bottleStack, Player player, ItemStack filledBottleStack) {
//        player.awardStat(Stats.ITEM_USED.get(this));
//        return ItemUtils.createFilledResult(bottleStack, player, filledBottleStack);
//    }
}
