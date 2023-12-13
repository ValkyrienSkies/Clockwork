package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.phys.Vec3

interface IGravitronTool {
    fun init()

    fun handleRightClick(): Boolean

    fun handleMouseWheel(delta: Double): Boolean

    fun renderTool(ms: PoseStack?, buffer: SuperRenderTypeBuffer?, camera: Vec3?)

    fun renderOverlay(graphics: GuiGraphics?, partialTicks: Float, width: Int, height: Int)
}