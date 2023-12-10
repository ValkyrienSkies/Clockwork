package org.valkyrienskies.clockwork.forge;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ClockworkRuntimeEvents {

    @SubscribeEvent
    public static void onAttachCapability(AttachCapabilitiesEvent<Entity> event) {
        //PlayerDataCapability.attachPlayerCapability(event);
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        //PlayerDataCapability.tick(event);

    }


    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        //PlayerDataCapability.syncPlayerCapability(event);
    }
}
