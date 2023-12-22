package org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronHandler;

public class ForgeGravitronHandler extends GravitronHandler implements IGuiOverlay {

    @Override
    public void render(ForgeGui forgeGui, PoseStack ms, float partialTicks, int width, int height) {
        render(ms, partialTicks, width, height);
    }
}