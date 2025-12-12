package org.valkyrienskies.clockwork.forge.content.curiosities.tools.aeronaut;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautArmorLayer;

public class ForgeAeronautArmorLayer extends AeronautArmorLayer {
    public ForgeAeronautArmorLayer(@Nullable RenderLayerParent renderer) {
        super(renderer);
    }

    public static void registerOn(EntityRenderer<?> entityRenderer) {
        if (!(entityRenderer instanceof LivingEntityRenderer))
            return;
        LivingEntityRenderer<?, ?> livingRenderer = (LivingEntityRenderer<?, ?>) entityRenderer;
        if (!(livingRenderer.getModel() instanceof HumanoidModel))
            return;
        AeronautArmorLayer<?, ?> layer = new AeronautArmorLayer<>(livingRenderer);
        livingRenderer.addLayer((AeronautArmorLayer) layer);
    }

    public static void registerOnAll(EntityRenderDispatcher renderManager) {
        for (EntityRenderer<? extends Player> renderer : renderManager.getSkinMap().values())
            registerOn(renderer);
        for (EntityRenderer<?> renderer : renderManager.renderers.values())
            registerOn(renderer);
    }
}
