package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron;

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import org.valkyrienskies.clockwork.ClockworkGuiTextures

class GravitronHotbarSlotOverlay {

    fun renderOn(poseStack: PoseStack, slot: Int) {
        val mainWindow = Minecraft.getInstance().getWindow();
        val x = mainWindow.getGuiScaledWidth() / 2 - 91;
        val y = mainWindow.getGuiScaledHeight() - 22;
        ClockworkGuiTextures.GRAVITRON_SELECTED.render(poseStack, x + 20 * slot, y);
    }
}
