package org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.forge.ClockworkModForge;
import org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.GravitronHandler;

abstract class GravitronToolBase implements IGravitronTool {

    protected GravitronHandler gravitronHandler = null;

    @Override
    public void init() {
        gravitronHandler = ClockworkModForge.GRAVITRON_HANDLER;
    }

    @Override
    public void renderTool(PoseStack ms, SuperRenderTypeBuffer buffer,Vec3 camera) {

    }

    @Override
    public void renderOverlay(GuiGraphics graphics, float partialTicks, int width, int height) {

    }
}