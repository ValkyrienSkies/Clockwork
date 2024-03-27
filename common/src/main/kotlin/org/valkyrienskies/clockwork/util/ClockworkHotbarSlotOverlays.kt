package org.valkyrienskies.clockwork.util;

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import org.valkyrienskies.clockwork.ClockworkGuiTextures

class ClockworkHotbarSlotOverlays {

    private var wanderliteFrame = 0
    private var wanderliteFrameCounter = 2.0

    fun renderBrass(poseStack: PoseStack, slot: Int) {
        val mainWindow = Minecraft.getInstance().getWindow()
        val x = mainWindow.getGuiScaledWidth() / 2 - 91
        val y = mainWindow.getGuiScaledHeight() - 22
        ClockworkGuiTextures.BRASS_SELECTED.render(poseStack, x + 20 * slot, y)
    }

    fun renderWanderlite(poseStack: PoseStack, slot: Int, partialTicks: Float) {
        val mainWindow = Minecraft.getInstance().getWindow()
        val x = mainWindow.getGuiScaledWidth() / 2 - 91
        val y = mainWindow.getGuiScaledHeight() - 22

        if (wanderliteFrame == 0) {
            ClockworkGuiTextures.WANDERLITE_SELECTED_1.render(poseStack, x + 20 * slot, y)
        } else {
            ClockworkGuiTextures.WANDERLITE_SELECTED_2.render(poseStack, x + 20 * slot, y)
        }

        wanderliteFrameCounter -= partialTicks

        if (wanderliteFrameCounter <= 0.0) {
            wanderliteFrame = (wanderliteFrame + 1) % 2
            wanderliteFrameCounter = 2.0
        }
    }
}
