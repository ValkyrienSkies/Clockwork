package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import net.createmod.catnip.math.AngleHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.util.Mth
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck
import kotlin.math.abs

open class GravitronItemRenderer : CustomRenderedItemModelRenderer() {

    var tempAngle: Float = 0f

    override fun render(
        stack: ItemStack,
        model: CustomRenderedItemModel,
        renderer: PartialItemModelRenderer,
        transformType: ItemDisplayContext,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int,
    ) {
        renderer.renderSolid(model.originalModel, light)
        val player = Minecraft.getInstance().player!!

        val prevAngle = GravitronState.getPrevDialAngle(player)
        val angle = GravitronState.getDialAngle(player)
        val needsRefresh = GravitronState.getNeedRefresh(player)

        if (needsRefresh) {
            this.tempAngle = prevAngle
            val duckPlayer = player as MixinPlayerDuck
            duckPlayer.needsRefresh = false
        }

        if (tempAngle != angle) {
            // Increment prevAngle by the step increment
            //TODO maybe easing
            if (prevAngle > angle) {
                tempAngle -= 1f
                if (tempAngle <= angle) tempAngle = angle
            } else {
                tempAngle += 1f
                if (tempAngle >= angle) tempAngle = angle
            }
        }

        tempAngle %= 360f

        renderDial(tempAngle, ms, renderer, light)

        renderer.render(ClockworkPartials.GRAV_PRONG_LEFT_ONE.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_RIGHT_ONE.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_LEFT_TWO.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_RIGHT_TWO.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_LEFT_THREE.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_RIGHT_THREE.get(), light)

        ms.mulPose(Quaternionf(AxisAngle4f(45f, 1f, 0f, 0f)))
        ms.translate(0.0, -0.300, 0.125)
        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_ONE.get(), light)
        ms.mulPose(Quaternionf(AxisAngle4f(Math.toRadians(-15.0).toFloat(), 1f, 0f, 0f)))
        ms.translate(0.0, 0.125, 0.025)
        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_TWO.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_THREE.get(), light)
    }

    private fun renderDial(angle: Float, matrices: PoseStack, renderer: PartialItemModelRenderer, light: Int) {

        matrices.pushPose()

        matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(22.5), -1f, 0f, 0f)))
        matrices.translate(0.275,0.2275,-0.115)
        matrices.pushPose()
        val x = -7.9/16
        val y = -7.3/16
        val z = -22.0/16

        matrices.translate(x,y,y)
        matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(angle - 180.0 + 10.0), 0f, 0f, -1f)))
        matrices.translate(-x,-y,-z)

        matrices.pushPose()

        renderer.render(ClockworkPartials.GRAV_DIAL_HAND.get(), light)
        matrices.popPose()
        matrices.popPose()
        matrices.popPose()

    }

    protected fun renderProngs() {}
}
