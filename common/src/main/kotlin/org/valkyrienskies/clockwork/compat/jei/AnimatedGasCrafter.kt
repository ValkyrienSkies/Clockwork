package org.valkyrienskies.clockwork.compat.jei

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.AllBlocks
import com.simibubi.create.AllPartialModels
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics
import net.createmod.catnip.animation.AnimationTickHolder
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkPartials

class AnimatedGasCrafter: AnimatedKinetics() {

    override fun draw(graphics: GuiGraphics, xOffset: Int, yOffset: Int) {
        val matrixStack: PoseStack = graphics.pose()
        matrixStack.pushPose()
        matrixStack.translate(xOffset.toFloat(), yOffset.toFloat(), 200f)
        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f))
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f))
        val scale = 25


        blockElement(ClockworkBlocks.GAS_CRAFTER.getDefaultState())
            .atLocal(0.0, 0.5, 0.0)
            .scale(scale.toDouble())
            .render(graphics)

        val animation = ((Mth.sin(AnimationTickHolder.getRenderTime() / 4f) + 1) / 5) - 0.1

        blockElement(ClockworkPartials.GAS_CRAFTER_FRAME)
            .atLocal(0.0, 0.5, 0.0)
            .rotateBlock(90.0,0.0,0.0)
            .scale(scale.toDouble())
            .render(graphics)

        blockElement(ClockworkPartials.GAS_CRAFTER_MESH)
            .rotateBlock(90.0, 180.0, 0.0)
            .atLocal(0.0, animation+0.5, 0.0)
            .scale(scale.toDouble())
            .render(graphics)

        blockElement(ClockworkPartials.GAS_CRAFTER_TUBE)
            .rotateBlock(90.0, 180.0, 0.0)
            .atLocal(0.0, 0.2 + 0.5, 0.0)
            .scale(scale.toDouble())
            .render(graphics)

        blockElement(ClockworkPartials.GAS_CRAFTER_GLOW)
            .rotateBlock(90.0, 180.0, 0.0)
            .atLocal(0.0, 0.2 + 0.5, 0.0)
            .scale(scale.toDouble())
            .render(graphics)

        blockElement(AllBlocks.BASIN.getDefaultState())
            .atLocal(0.0, 1.0 + 0.5, 0.0)
            .scale(scale.toDouble())
            .render(graphics)

        matrixStack.popPose()
    }
}