package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.AllParticleTypes;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.valkyrienskies.clockwork.*;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluper.BluperGlueSelectionHandler;
import org.valkyrienskies.clockwork.content.curiosities.tools.designator.AuricDesignatorClusterRenderer;
import org.valkyrienskies.clockwork.forge.config.AllClockworkConfigs;
import org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron.ForgeGravitronHandler;

import static org.valkyrienskies.clockwork.ClockworkMod.MOD_ID;

@Mod(MOD_ID)
public class ClockworkModForge {

    //final DeferredRegister<EntityDataSerializer<?>> DATA_SERIALIZER_REGISTER = DeferredRegister.create(ForgeRegistries.Keys.DATA_SERIALIZERS, MOD_ID);

    public static final ForgeGravitronHandler GRAVITRON_HANDLER = new ForgeGravitronHandler();
    public static final BluperGlueSelectionHandler BLUPER_CLUSTER_HANDLER = new BluperGlueSelectionHandler();
    public static final AuricDesignatorClusterRenderer AURIC_HANDLER = new AuricDesignatorClusterRenderer();

    public ClockworkModForge() {
        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        AllClockworkConfigs.register(modLoadingContext);
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ClockworkMod.INSTANCE.getREGISTRATE().registerEventListeners(modEventBus);

        ClockworkBlocks.register();
        ClockworkItems.register();
        ClockworkBlockEntities.register();

        ForgeClockworkFluids.register();

        ClockworkEntities.register();
        ForgeClockworkEntities.register();

        ClockworkParticles.init();

        ClockworkSounds.register();

        ClockworkMod.init();
        ClockworkPackets.init();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClockworkPartials.INSTANCE.init();
            ClockworkMod.initClient();
            modEventBus.addListener(AllParticleTypes::registerFactories);
            ClockworkShaders.INSTANCE.init();
            OverlayRegistry.registerOverlayAbove(ForgeIngameGui.HOTBAR_ELEMENT, "Gravitron",
                    ClockworkModForge.GRAVITRON_HANDLER.getOverlayRenderer());
        });

        //DATA_SERIALIZER_REGISTER.register("area", () -> ClockworkEntityDataSerializers.AREA_TOOLKIT_SERIALIZER);
        EntityDataSerializers.registerSerializer(ClockworkEntityDataSerializers.AREA_TOOLKIT_SERIALIZER);
        //DATA_SERIALIZER_REGISTER.register(modEventBus);
    }
}
