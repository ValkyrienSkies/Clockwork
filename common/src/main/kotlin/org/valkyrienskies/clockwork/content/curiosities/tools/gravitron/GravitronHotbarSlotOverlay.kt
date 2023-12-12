package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron;

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import org.valkyrienskies.clockwork.ClockworkGuiTextures

class GravitronHotbarSlotOverlay {

    fun renderOn(graphics: GuiGraphics, slot: Int) {
        val mainWindow = Minecraft.getInstance().getWindow();
        val x = mainWindow.getGuiScaledWidth() / 2 - 91;
        val y = mainWindow.getGuiScaledHeight() - 22;
        ClockworkGuiTextures.GRAVITRON_SELECTED.render(graphics, x + 20 * slot, y);
    }
}
