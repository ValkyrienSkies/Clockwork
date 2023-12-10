package org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.tool

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.schematics.client.SchematicHandler
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.GravitronHandler

abstract class GravitronToolBase : IGravitronTool {

    protected var gravitronHandler: GravitronHandler? = null

    override fun init() {
        gravitronHandler = ClockworkMod.GRAVITRON_HANDLER
    }

    override fun renderTool(ms: PoseStack?, buffer: SuperRenderTypeBuffer?, camera: Vec3?) {

    }

    override fun renderOverlay(graphics: GuiGraphics?, partialTicks: Float, width: Int, height: Int) {

    }
}