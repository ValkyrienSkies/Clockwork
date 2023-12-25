package org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronHandler;

public class ForgeGravitronHandler extends GravitronHandler  {

    private final IIngameOverlay overlayRenderer = this::forgeRender;

    @OnlyIn(Dist.CLIENT)
    private void forgeRender(ForgeIngameGui forgeIngameGui, PoseStack poseStack, float v, int i, int i1) {
        render(poseStack, v,i,i1);
    }

    public IIngameOverlay getOverlayRenderer() {
        return overlayRenderer;
    }
}