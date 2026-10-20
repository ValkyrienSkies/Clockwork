package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool

import net.createmod.catnip.render.SuperRenderTypeBuffer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandSelectionPacket
import org.valkyrienskies.clockwork.platform.SharedValues

open class WanderwandToolBase : IWanderwandTool {
    private fun click(left: Boolean): Boolean {
        val tool = SharedValues.wanderwandHandler.currentTool ?: return false
        ClockworkPackets.sendToServer(WandSelectionPacket(BlockPos.ZERO, null, tool, left))
        return true
    }
    override fun handleLeftClick() = click(true)
    override fun handleRightClick(crouching: Boolean) = click(false)
    override fun handleMouseWheel(delta: Double) = false
    override fun init() {}
    override fun renderTool(ms: GuiGraphics?, buffer: SuperRenderTypeBuffer?, camera: Vec3?) {}
    override fun renderOverlay(poseStack: GuiGraphics, partialTicks: Float, width: Int, height: Int) {}
}
