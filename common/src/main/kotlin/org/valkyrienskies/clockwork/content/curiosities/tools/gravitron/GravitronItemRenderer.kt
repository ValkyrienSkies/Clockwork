package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
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
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck

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
//        ms.pushPose()
//        ms.translate(1.7441/16f, 4.0858/16f, 0.0)
//        ms.mulPose(Vector3f.ZP.rotationDegrees(135f))
//        ms.mulPose(Vector3f.XP.rotationDegrees(45f))
//        //ms.mulPose(Vector3f.YP.rotationDegrees(120f))
//        ms.translate(0.0, -0.300, 0.125)
//        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_ONE.get(), light)
//
//        //ms.mulPose(Vector3f.ZP.rotationDegrees(-120f))
//        ms.mulPose(Vector3f.XP.rotationDegrees(-15f))
//        //ms.mulPose(Vector3f.YP.rotationDegrees(120f))
//        ms.translate(0.0, 0.125, 0.025)
//        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_TWO.get(), light)
//
//        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_THREE.get(), light)
//        //ms.mulPose(Vector3f.YP.rotation(120f))
//        ms.popPose()
        ms.pushPose()

        //ms.translate(8.7559/16f, 4.0858/16f, 0.0)
        ms.translate(-7.7016/16f,-8.3536/16f,-1.6978/16f)
        ms.mulPose(Axis.ZN.rotationDegrees(135f))
        ms.pushPose()
        //ms.translate(7.7016/16f, 8.3536/16f, -1.6978/16f)
        //val rotatedVector2: Vector3d = Vector3d(7.7016/16f, 8.3536/16f, 1.6978/16f).rotate(Quaterniond(AxisAngle4d(Math.toRadians(135.0) ,0.0, 0.0, -1.0)))
        //ms.translate(rotatedVector2.x, rotatedVector2.y, rotatedVector2.z)
        ms.mulPose(Quaternionf(AxisAngle4f(45f, 1f, 0f, 0f)))
        //ms.mulPose(Vector3f.ZP.rotationDegrees(-240f))
        ms.translate(0.0, -0.300, 0.125)
        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_ONE.get(), light)

        //ms.mulPose(Vector3f.ZP.rotationDegrees(240f))
        ms.mulPose(Quaternionf(AxisAngle4f(Math.toRadians(-15.0).toFloat(), 1f, 0f, 0f)))
        ms.translate(0.0, 0.125, 0.025)
        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_TWO.get(), light)

        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_THREE.get(), light)
        //todo: update to 1.20 math?
        //ms.mulPose(Vector3f.YP.rotation(240f))
        ms.popPose()
        ms.translate(7.7016/16f,8.3536/16f,1.6978/16f)
        ms.popPose()
        ms.pushPose()
        ms.pushPose()
        ms.translate(-7.7016/16f,-8.3536/16f,-1.6978/16f)
        ms.mulPose(Axis.XP.rotationDegrees(45f))
        ms.translate(7.7016/16f,8.3536/16f,1.6978/16f)
        //ms.translate(0.0, -0.300, 0.125)
        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_ONE.get(), light)
        ms.popPose()
        ms.pushPose()
        ms.translate(-7.6944, -8.4963, 0.9806)
        ms.mulPose(Axis.XP.rotationDegrees(-15f))
        ms.translate(7.6944, 8.4963, -0.9806)
        //ms.translate(0.0, 0.125, 0.025)
        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_TWO.get(), light)
        ms.popPose()
        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_THREE.get(), light)
        ms.popPose()
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
