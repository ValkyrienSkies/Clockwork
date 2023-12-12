package org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron.tool;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

public interface IGravitronTool {

    void init();

    boolean handleRightClick();

    boolean handleMouseWheel(double delta);

    void renderTool(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera);

    void renderOverlay(GuiGraphics graphics, float partialTicks, int width, int height);
}