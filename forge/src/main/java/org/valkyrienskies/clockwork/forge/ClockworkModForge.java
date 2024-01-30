package org.valkyrienskies.clockwork.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderWandClusterRenderer;
import org.valkyrienskies.clockwork.forge.config.AllClockworkConfigs;
import org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron.ForgeGravitronHandler;
import org.valkyrienskies.clockwork.forge.integration.cc.ClockworkForgePeripheralProviders;

import static org.valkyrienskies.clockwork.ClockworkMod.MOD_ID;

@Mod(MOD_ID)
public class ClockworkModForge {

    //final DeferredRegister<EntityDataSerializer<?>> DATA_SERIALIZER_REGISTER = DeferredRegister.create(ForgeRegistries.Keys.DATA_SERIALIZERS, MOD_ID);

    public static final WanderWandClusterRenderer WANDER_HANDLER = new WanderWandClusterRenderer();

    public ClockworkModForge() {
        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ClockworkMod.INSTANCE.getREGISTRATE().registerEventListeners(modEventBus);

        ClockworkBlocks.register();
        ClockworkItems.register();
        ClockworkBlockEntities.register();
        ForgeClockworkBlockEntities.register();

        ForgeClockworkFluids.register();

        ClockworkEntities.register();
        ForgeClockworkEntities.register();

        ClockworkParticles.init();

        AllClockworkConfigs.register(modLoadingContext);

        ClockworkSounds.register();

        ClockworkMod.init();
        ClockworkPackets.init();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClockworkModForgeClient.onCtorClient(modEventBus));

        if (FMLLoader.getLoadingModList().getModFileById("computercraft") != null) {
            ClockworkForgePeripheralProviders.register();
        }
    }
}
