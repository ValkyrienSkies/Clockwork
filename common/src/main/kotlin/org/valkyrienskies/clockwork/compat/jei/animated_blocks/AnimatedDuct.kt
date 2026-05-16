package org.valkyrienskies.clockwork.compat.jei.animated_blocks

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.AllBlocks
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics
import net.createmod.catnip.animation.AnimationTickHolder
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkPartials

class AnimatedDuct: AnimatedKinetics() {

    override fun draw(graphics: GuiGraphics, xOffset: Int, yOffset: Int) {
        val matrixStack: PoseStack = graphics.pose()
        matrixStack.pushPose()
        matrixStack.translate(xOffset.toFloat(), yOffset.toFloat(), 200f)
        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f))
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f))
        val scale = 25


        blockElement(ClockworkBlocks.DUCT.defaultState)
            .atLocal(0.0, 0.5, 0.0)
            .scale(scale.toDouble())
            .render(graphics)

        matrixStack.popPose()
    }
}