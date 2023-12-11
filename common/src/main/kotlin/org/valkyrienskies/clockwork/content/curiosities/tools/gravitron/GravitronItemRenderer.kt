package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import com.simibubi.create.foundation.utility.AnimationTickHolder
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.util.Mth
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPartials

class GravitronItemRenderer : CustomRenderedItemModelRenderer() {
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
        renderer.render(ClockworkPartials.GRAV_DIAL_HAND.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_LEFT_ONE.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_RIGHT_ONE.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_LEFT_TWO.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_RIGHT_TWO.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_LEFT_THREE.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_RIGHT_THREE.get(), light)
        ms.mulPose(Axis.XP.rotationDegrees(45f))
        ms.translate(0.0, -0.300, 0.125)
        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_ONE.get(), light)
        ms.mulPose(Axis.XP.rotationDegrees(-15f))
        ms.translate(0.0, 0.125, 0.025)
        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_TWO.get(), light)
        renderer.render(ClockworkPartials.GRAV_PRONG_TOP_THREE.get(), light)
    }

    protected fun renderDial() {}
    protected fun renderProngs() {}
}
