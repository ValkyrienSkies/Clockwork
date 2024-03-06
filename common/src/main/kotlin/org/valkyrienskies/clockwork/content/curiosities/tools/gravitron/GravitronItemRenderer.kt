package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Vector3f
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import com.simibubi.create.foundation.utility.AnimationTickHolder
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.core.Direction
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPartials
import kotlin.math.PI

class GravitronItemRenderer : CustomRenderedItemModelRenderer() {
    override fun render(
        stack: ItemStack,
        model: CustomRenderedItemModel,
        renderer: PartialItemModelRenderer,
        transformType: ItemTransforms.TransformType,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int,
    ) {
        val pt = AnimationTickHolder.getPartialTicks()
        val worldTime = AnimationTickHolder.getRenderTime() / 20
        renderer.renderSolid(model.getOriginalModel(), light)
        val player = Minecraft.getInstance().player
        val leftHanded = player!!.mainArm == HumanoidArm.LEFT
        val mainHand = player.mainHandItem == stack
        val offHand = player.offhandItem == stack
        val animation = 0
        var angle = worldTime * -25
        if (mainHand || offHand) angle += 360 * animation
        val offset = Vec3(7.7016, 8.3536, 1.6978)
        //renderer.render(ClockworkPartials.GRAV_DIAL_HAND.get(), light)
        renderDial(stack, ms, renderer, light)
        renderer.render(ClockworkPartials.GRAV_PRONG_LEFT_ONE.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_RIGHT_ONE.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_LEFT_TWO.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_RIGHT_TWO.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_LEFT_THREE.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_RIGHT_THREE.get(), light)
        ms.mulPose(Vector3f.XP.rotationDegrees(45f))
        ms.translate(0.0, -0.300, 0.125)
        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_ONE.get(), light)
        ms.mulPose(Vector3f.XP.rotationDegrees(-15f))
        ms.translate(0.0, 0.125, 0.025)
        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_TWO.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_THREE.get(), light)
    }

    protected fun renderDial(stack: ItemStack, ms: PoseStack, renderer: PartialItemModelRenderer, light: Int) {

        ms.pushPose()

        ms.mulPose(Vector3f.XN.rotationDegrees(22.5f))
        ms.translate(0.315,0.175,-0.115)
        ms.pushPose()
        val x = -8.517/16
        val y = -6.48/16
        val z = 14.0/16

        ms.translate(x,y,y)
        ms.mulPose(Vector3f.ZN.rotationDegrees(6 * AnimationTickHolder.getTicks() % 360.0f))
        ms.translate(-x,-y,-z)
        ms.pushPose()
        ms.popPose()
        ms.translate(.0015,.0,2.2)
        ms.pushPose()
        ms.translate(-.04,.05,.0)

        renderer.render(ClockworkPartials.GRAV_DIAL_HAND.get(), light)
        ms.popPose()
        ms.popPose()

        ms.popPose()

    }
    protected fun renderProngs() {}
}
