package org.valkyrienskies.clockwork.content.materials.solids.soda;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.ClockWorkItems;

import java.util.Iterator;
import java.util.List;

public abstract class AbstractSoda extends Item {
    private static final int DRINK_DURATION = 32;

    public AbstractSoda(Properties properties) {
        super(properties);
    }

    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        Player player = livingEntity instanceof Player ? (Player)livingEntity : null;
        if (player instanceof ServerPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer)player, stack);
        }

        if (!level.isClientSide) {
            Vec3 angle = player.getLookAngle();
            player.setDeltaMovement(angle.reverse().multiply(5,5,5));
        }

        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        if (player == null || !player.getAbilities().instabuild) {
            if (stack.isEmpty()) {
//                return new ItemStack(ClockWorkItems.EMPTY_SODA.get());
            }

            if (player != null) {
//                player.getInventory().add(new ItemStack(ClockWorkItems.EMPTY_SODA.get()));
            }
        }

        level.gameEvent(livingEntity, GameEvent.DRINKING_FINISH, livingEntity.eyeBlockPosition());
        return stack;
    }

    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        return ItemUtils.startUsingInstantly(level, player, usedHand);
    }

    public String getDescriptionId(ItemStack stack) {
        return PotionUtils.getPotion(stack).getName(this.getDescriptionId() + ".effect.");
    }

    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        PotionUtils.addPotionTooltip(stack, tooltipComponents, 1.0F);
    }

    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || !PotionUtils.getMobEffects(stack).isEmpty();
    }

}
