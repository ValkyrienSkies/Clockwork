package org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.forge.ClockworkModForge;
import org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.GravitronHandler;
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck;

abstract class GravitronToolBase implements IGravitronTool {

    protected GravitronHandler gravitronHandler = null;

    public static GravitronItem.Companion.GravitronState getState(Player player) {
        MixinPlayerDuck p = (MixinPlayerDuck) player;
        GravitronItem.Companion.GravitronState s = p.cw_getGravitronState();

        if (s == null) {
            s = new GravitronItem.Companion.GravitronState();
            p.cw_setGravitronState(s);
        }

        return s;
    }

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