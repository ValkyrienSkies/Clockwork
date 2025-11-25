package org.valkyrienskies.clockwork.forge.content.curiosities.tools.wanderwand;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronHandler;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandHandler;

public class ForgeWanderwandHandler extends WanderwandHandler {

    private final IGuiOverlay overlayRenderer = this::forgeRender;

    @OnlyIn(Dist.CLIENT)
    private void forgeRender(ForgeGui forgeIngameGui, GuiGraphics poseStack, float v, int i, int i1) {
        render(poseStack, v,i,i1);
    }

    public IGuiOverlay getOverlayRenderer() {
        return overlayRenderer;
    }
}
