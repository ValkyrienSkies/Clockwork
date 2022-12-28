package org.valkyrienskies.clockwork.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ForgeModelBakery;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.valkyrienskies.core.impl.config.VSConfigClass;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;
import org.valkyrienskies.clockwork.ClockWorkConfig;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.block.WoodType;
import org.valkyrienskies.clockwork.blockentity.renderer.ShipHelmBlockEntityRenderer;
import org.valkyrienskies.clockwork.blockentity.renderer.WheelModels;
import org.valkyrienskies.mod.compat.clothconfig.VSClothConfig;

@Mod(ClockWorkMod.MOD_ID)
public class ClockWorkForge {
    boolean happendClientSetup = false;
    static IEventBus MOD_BUS;

    public ClockWorkForge() {
        // Submit our event bus to let architectury register our content on the right time
        MOD_BUS = FMLJavaModLoadingContext.get().getModEventBus();
        MOD_BUS.addListener(this::clientSetup);

        ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((Minecraft client, Screen parent) ->
                        VSClothConfig.createConfigScreenFor(parent,
                                VSConfigClass.Companion.getRegisteredConfig(ClockWorkConfig.class)))
        );

        MOD_BUS.addListener(this::onModelRegistry);
        MOD_BUS.addListener(this::clientSetup);
        MOD_BUS.addListener(this::entityRenderers);

        ClockWorkMod.init();
    }

    void clientSetup(final FMLClientSetupEvent event) {
        if (happendClientSetup) return;
        happendClientSetup = true;

        ClockWorkMod.initClient();

        WheelModels.INSTANCE.setModelGetter(woodType -> ForgeModelBakery.instance().getBakedTopLevelModels()
                .getOrDefault(
                        new ResourceLocation(ClockWorkMod.MOD_ID, "block/" + woodType.getResourceName() + "_ship_helm_wheel"),
                        Minecraft.getInstance().getModelManager().getMissingModel()
                ));
    }

    void entityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ClockWorkBlockEntities.INSTANCE.getSHIP_HELM().get(),
                ShipHelmBlockEntityRenderer::new
        );
    }

    void onModelRegistry(final ModelRegistryEvent event) {
        for (WoodType woodType : WoodType.values()) {
            ForgeModelBakery.addSpecialModel(new ResourceLocation(ClockWorkMod.MOD_ID, "block/" + woodType.getResourceName() + "_ship_helm_wheel"));
        }
    }
}
