package org.valkyrienskies.clockwork.fabric;

import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronState;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.GrabTool;

public class FabricClockworkCommonEvents {

    public static void onLivingTick(LivingEntityEvents.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player) {
            GrabTool.tick(player);
        }
    }

    public static InteractionResult playerLeftClick(Player player, Level level, InteractionHand interactionHand, BlockPos blockPos, Direction direction) {
        GravitronState.leftClickItem(player, GravitronState.getState(player));

//        boolean bl = WanderWandItem.onAttack(player);
//        if (bl) {
//            return InteractionResult.FAIL;
//        }
        return InteractionResult.PASS;
    }
}
