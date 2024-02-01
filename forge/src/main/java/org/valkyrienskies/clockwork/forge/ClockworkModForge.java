package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.AllParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.loading.FMLLoader;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderWandClusterRenderer;
import org.valkyrienskies.clockwork.forge.config.AllClockworkConfigs;
import org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron.ForgeGravitronHandler;
import org.valkyrienskies.clockwork.forge.integration.cc.ClockworkForgePeripheralProviders;

import static org.valkyrienskies.clockwork.ClockworkMod.MOD_ID;

@Mod(MOD_ID)
public class ClockworkModForge {

    final DeferredRegister<CreativeModeTab> TAB_REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final AuricDesignatorClusterRenderer AURIC_HANDLER = new AuricDesignatorClusterRenderer();
    public static final WanderWandClusterRenderer AURIC_HANDLER = new WanderWandClusterRenderer();

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

        modEventBus.addListener(this::onClientSetup);

        TAB_REGISTER.register("general", ClockworkMod.INSTANCE::createCreativeTab);
        TAB_REGISTER.register(modEventBus);
        if (FMLLoader.getLoadingModList().getModFileById("computercraft") != null) {
            ClockworkForgePeripheralProviders.register();
        }
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(ClockworkBlocks.GOO_BLOCK.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ClockworkBlocks.SLICKER.get(), RenderType.translucent());
    }
}
