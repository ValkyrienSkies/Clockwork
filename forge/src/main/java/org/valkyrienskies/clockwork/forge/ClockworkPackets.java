package org.valkyrienskies.clockwork.forge;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.forge.capability.SyncPlayerCapabilityDataPacket;
import org.valkyrienskies.clockwork.platform.SharedValues;

@Mod.EventBusSubscriber(modid = ClockworkMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClockworkPackets {

    @SubscribeEvent
    public static void registerNetworkStuff(FMLCommonSetupEvent event) {
        SyncPlayerCapabilityDataPacket.register(SharedValues.getPacketChannel());
    }
}
