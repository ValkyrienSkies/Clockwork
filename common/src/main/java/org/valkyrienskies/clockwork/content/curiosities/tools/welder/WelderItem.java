package org.valkyrienskies.clockwork.content.curiosities.tools.welder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import java.util.Random;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.valkyrienskies.clockwork.ClockWorkSounds;
import org.valkyrienskies.clockwork.content.contraptions.universal_joint.UniversalJointBlock;
import org.valkyrienskies.clockwork.content.contraptions.universal_joint.UniversalJointBlockEntity;
import org.valkyrienskies.clockwork.platform.CWItem;

public class WelderItem extends CWItem {

    BlockPos firstTargetPos = null;

    private int countdown = 200;

    public WelderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        //todo add more connection types
        if (context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof UniversalJointBlock) {
            if (firstTargetPos == null) {
                firstTargetPos = context.getClickedPos();
                Player user = context.getPlayer();
                if (user != null) {
                    user.sendMessage(new TextComponent("First position set to " + firstTargetPos.toShortString() + "!").withStyle(ChatFormatting.AQUA), user.getUUID());
                }
                context.getLevel().playSound(user, context.getClickedPos(), ClockWorkSounds.WELDER_WHIRR.getMainEvent(), SoundSource.PLAYERS, 1f, Mth.randomBetween(new Random(), 0.8f, 1.2f));
                return InteractionResult.SUCCESS;
            } else {
                BlockEntity be = context.getLevel().getBlockEntity(firstTargetPos);
                BlockEntity otherbe = context.getLevel().getBlockEntity(context.getClickedPos());
                if (be instanceof UniversalJointBlockEntity && otherbe instanceof UniversalJointBlockEntity) {
                    UniversalJointBlockEntity ube = (UniversalJointBlockEntity) be;
                    UniversalJointBlockEntity otherube = (UniversalJointBlockEntity) otherbe;

                    ube.setConnectedPos(context.getClickedPos());
                    otherube.setConnectedPos(firstTargetPos);

                    Player user = context.getPlayer();
                    if (user != null) {
                        user.sendMessage(new TextComponent("Connected " + firstTargetPos.toShortString() + " to " + context.getClickedPos().toShortString() + "!").withStyle(ChatFormatting.AQUA), user.getUUID());
                    }
                    context.getLevel().playSound(user, context.getClickedPos(), ClockWorkSounds.WELDER_WELD.getMainEvent(), SoundSource.PLAYERS, 1f, Mth.randomBetween(new Random(), 0.8f, 1.2f));
                    return InteractionResult.SUCCESS;
                } else {
                    return InteractionResult.FAIL;
                }
            }
        }


        return InteractionResult.PASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (firstTargetPos != null) {
            countdown--;
            if (countdown <= 0) {
                firstTargetPos = null;
                countdown = 200;
            }

            if (!isSelected) {
                firstTargetPos = null;
                countdown = 200;
            }
        }
    }
}
