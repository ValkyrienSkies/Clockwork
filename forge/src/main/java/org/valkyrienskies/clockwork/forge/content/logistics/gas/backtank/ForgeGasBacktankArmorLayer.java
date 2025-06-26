package org.valkyrienskies.clockwork.forge.content.logistics.gas.backtank;

import com.simibubi.create.content.equipment.armor.BacktankArmorLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.logistics.gas.backtank.GasBacktankArmorLayer;

public class ForgeGasBacktankArmorLayer extends GasBacktankArmorLayer  {
    public ForgeGasBacktankArmorLayer(@Nullable RenderLayerParent renderer) {
        super(renderer);
    }

    public static void registerOn(EntityRenderer<?> entityRenderer) {
        if (!(entityRenderer instanceof LivingEntityRenderer))
            return;
        LivingEntityRenderer<?, ?> livingRenderer = (LivingEntityRenderer<?, ?>) entityRenderer;
        if (!(livingRenderer.getModel() instanceof HumanoidModel))
            return;
        BacktankArmorLayer<?, ?> layer = new BacktankArmorLayer<>(livingRenderer);
        livingRenderer.addLayer((BacktankArmorLayer) layer);
    }

    public static void registerOnAll(EntityRenderDispatcher renderManager) {
        for (EntityRenderer<? extends Player> renderer : renderManager.getSkinMap().values())
            registerOn(renderer);
        for (EntityRenderer<?> renderer : renderManager.renderers.values())
            registerOn(renderer);
    }
}
