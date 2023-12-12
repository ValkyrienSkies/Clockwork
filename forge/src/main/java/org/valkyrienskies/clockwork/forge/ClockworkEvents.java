package org.valkyrienskies.clockwork.forge;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.clockwork.AreaData;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.SelectedAreaToolkit;
import org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron.tool.GrabTool;
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck;

import java.util.Optional;


@Mod.EventBusSubscriber
public class ClockworkEvents {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        GrabTool.tick(event);
    }

    @SubscribeEvent
    public static void playerLeftClick(PlayerInteractEvent.LeftClickBlock event) {

    }

    @SubscribeEvent
    public static void playerJoin(EntityJoinLevelEvent event) {
       if (event.getEntity() instanceof Player player) {
           MixinPlayerDuck p = ((MixinPlayerDuck) player);
           p.cw_setGravitronState(new GravitronItem.Companion.GravitronState());

           AreaData.of(player).get().setArea(new SelectedAreaToolkit());
           AreaData.of(player).get().setFirstPos(Optional.empty());
           AreaData.of(player).get().setSecondPos(Optional.empty());
       }
    }
}
