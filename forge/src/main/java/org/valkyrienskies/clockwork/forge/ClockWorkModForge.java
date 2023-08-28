package org.valkyrienskies.clockwork.forge;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.valkyrienskies.clockwork.ClockworkMod;

@Mod(ClockworkMod.MOD_ID)
public class ClockworkModForge {
    boolean happendClientSetup = false;
    static IEventBus MOD_BUS;

    public ClockworkModForge() {
        // Submit our event bus to let architectury register our content on the right time
        MOD_BUS = FMLJavaModLoadingContext.get().getModEventBus();
        MOD_BUS.addListener(this::clientSetup);


        ClockworkMod.init();
    }

    void clientSetup(final FMLClientSetupEvent event) {
        if (happendClientSetup) {
            return;
        }
        happendClientSetup = true;

        ClockworkMod.initClient();
    }
}
