package org.valkyrienskies.clockwork.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.GravitronHandler;

public class GravitronHandlerForge extends GravitronHandler implements @NotNull IGuiOverlay {


    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {
        if (Minecraft.getInstance().options.hideGui || !active)
            return;
        if (activeSchematicItem != null)
            this.overlay.renderOn(graphics, activeHotbarSlot);
        currentTool.getTool()
                .renderOverlay(gui, graphics, partialTicks, width, height);
        selectionScreen.renderPassive(graphics, partialTicks);
    }
}
