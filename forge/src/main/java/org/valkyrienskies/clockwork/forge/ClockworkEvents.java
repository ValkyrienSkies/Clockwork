package org.valkyrienskies.clockwork.forge;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.clockwork.content.curiosities.tools.designator.AuricDesignatorItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.GrabTool;


@Mod.EventBusSubscriber
public class ClockworkEvents {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player) {
            GrabTool.tick(player);
        }
    }

    @SubscribeEvent
    public static void playerLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        GravitronItem.leftClickItem(event.getEntity(), GravitronItem.getState(event.getEntity()));
        AuricDesignatorItem.onAttack(event.getEntity());
    }
}
