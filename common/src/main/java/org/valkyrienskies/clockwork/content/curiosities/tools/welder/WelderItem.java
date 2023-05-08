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
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

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
            if (context.getLevel().getBlockEntity(context.getClickedPos()) != null && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof UniversalJointBlockEntity) {
                UniversalJointBlockEntity ube = (UniversalJointBlockEntity) context.getLevel().getBlockEntity(context.getClickedPos());

                if (ube != null && ube.getConnectedPos() != null) {

                    Player user = context.getPlayer();

                    if (user != null) {
                        user.sendMessage(new TextComponent("That block already has an attachment...").withStyle(ChatFormatting.YELLOW), user.getUUID());
                    }
                    firstTargetPos = null;
                    return InteractionResult.FAIL;
                }
            }
            if (firstTargetPos == null) {
                firstTargetPos = context.getClickedPos();
                Player user = context.getPlayer();
                if (user != null) {
                    user.sendMessage(new TextComponent("First position set to " + firstTargetPos.toShortString() + "!").withStyle(ChatFormatting.AQUA), user.getUUID());
                }
                context.getLevel().playSound(user, context.getClickedPos(), ClockWorkSounds.WELDER_WHIRR.getMainEvent(), SoundSource.PLAYERS, 1f, Mth.randomBetween(new Random(), 0.8f, 1.2f));
                return InteractionResult.SUCCESS;
            } else if (!(context.getClickedPos().equals(firstTargetPos))) {
                BlockEntity be = context.getLevel().getBlockEntity(firstTargetPos);
                BlockEntity otherbe = context.getLevel().getBlockEntity(context.getClickedPos());
                if (be instanceof UniversalJointBlockEntity && otherbe instanceof UniversalJointBlockEntity) {
                    UniversalJointBlockEntity ube = (UniversalJointBlockEntity) be;
                    UniversalJointBlockEntity otherube = (UniversalJointBlockEntity) otherbe;

                    Ship ship = VSGameUtilsKt.getShipObjectManagingPos(context.getLevel(), context.getClickedPos());
                    if (ship == null) {
                        Player user = context.getPlayer();
                        if (user != null) {
                            user.sendMessage(new TextComponent("Target is not on a ship!").withStyle(ChatFormatting.RED), user.getUUID());
                        }
                        firstTargetPos = null;
                        return InteractionResult.FAIL;
                    }
                    ube.setConnectedPos(context.getClickedPos(), true);
                    otherube.setConnectedPos(firstTargetPos, false);

                    Player user = context.getPlayer();
                    if (user != null) {
                        user.sendMessage(new TextComponent("Connected " + firstTargetPos.toShortString() + " to " + context.getClickedPos().toShortString() + "!").withStyle(ChatFormatting.GREEN), user.getUUID());
                    }
                    context.getLevel().playSound(user, context.getClickedPos(), ClockWorkSounds.WELDER_WELD.getMainEvent(), SoundSource.PLAYERS, 1f, Mth.randomBetween(new Random(), 0.8f, 1.2f));
                    return InteractionResult.SUCCESS;
                } else {
                    Player user = context.getPlayer();
                    if (user != null) {
                        user.sendMessage(new TextComponent("Connection failed...").withStyle(ChatFormatting.RED), user.getUUID());
                    }
                    firstTargetPos = null;
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
