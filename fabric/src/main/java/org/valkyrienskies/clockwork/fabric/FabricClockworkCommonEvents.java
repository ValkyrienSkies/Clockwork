package org.valkyrienskies.clockwork.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronState;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderWandItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.GrabTool;

public class FabricClockworkCommonEvents {



    public static void onLivingTick(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            GrabTool.tick(player);
        }
    }

    public static InteractionResult playerLeftClick(Player player, Level level, InteractionHand interactionHand, BlockPos blockPos, Direction direction) {
        GravitronState.leftClickItem(player, GravitronState.getState(player));

        boolean bl = WanderWandItem.onAttack(player);
        if (bl) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }
}
