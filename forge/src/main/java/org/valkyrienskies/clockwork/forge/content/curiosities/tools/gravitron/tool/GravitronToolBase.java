package org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron.tool;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.RaycastHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.forge.ClockworkModForge;
import org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron.GravitronHandler;
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck;

public abstract class GravitronToolBase implements IGravitronTool {

    protected GravitronHandler gravitronHandler = null;
    public BlockPos clickedPos;
    public Vec3 clickedLocation;
    public static byte GRAB = 1;
    public static byte ASSEMBLE = 2;
    public static byte GRABSSEMBLE = 3;

    public void updateTargetPos() {
        LocalPlayer player = Minecraft.getInstance().player;

        BlockHitResult trace = RaycastHelper.rayTraceRange(player.level(), player, player.getBlockReach() + 2);
        if (trace == null || trace.getType() != HitResult.Type.BLOCK) {
            return;
        }

        clickedPos = trace.getBlockPos().immutable();
        clickedLocation = clickedPos.getCenter();
    }

    @Override
    public boolean handleRightClick() {
        return false;
    }

    @Override
    public void init() {
        gravitronHandler = ClockworkModForge.GRAVITRON_HANDLER;
    }

    @Override
    public void renderTool(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera) {

    }

    @Override
    public void renderOverlay(GuiGraphics graphics, float partialTicks, int width, int height) {

    }
}