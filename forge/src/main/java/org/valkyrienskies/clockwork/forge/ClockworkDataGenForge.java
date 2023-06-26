package org.valkyrienskies.clockwork.forge;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.datagen.ClockworkDataGen;

@Mod.EventBusSubscriber(modid = ClockWorkMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClockworkDataGenForge {
    @SubscribeEvent
    public static void onDatagen(GatherDataEvent evt) {
        ClockworkDataGen.register(evt.getGenerator(), evt.getExistingFileHelper(), evt.includeClient(), evt.includeServer());
    }
}
