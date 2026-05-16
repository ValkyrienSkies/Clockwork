package org.valkyrienskies.clockwork.content.curiosities.tools.saw

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueHandler
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import net.createmod.catnip.animation.AnimationTickHolder
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.ClockworkPartials

class HandheldSawRenderer : CustomRenderedItemModelRenderer() {
    override fun render(
        stack: ItemStack,
        model: CustomRenderedItemModel,
        renderer: PartialItemModelRenderer,
        transformType: ItemDisplayContext,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        renderer.render(model.getOriginalModel(), light);

        val xOffset = -1/16f
        //ms.translate(-xOffset, 0f, 0f)
        ms.pushPose()
        ms.mulPose(Axis.ZP.rotationDegrees(ScrollValueHandler.getScroll(AnimationTickHolder.getPartialTicks())))
        //ms.translate(xOffset, 0f, 0f)

        renderer.render(ClockworkPartials.HANDHELD_SAW_COG.get(), light)
        ms.popPose()
        //if holding left click, speen
        ms.pushPose()

        renderer.render(ClockworkPartials.HANDHELD_SAW_BUZZSAW.get(), light)
        if (Minecraft.getInstance().options.keyAttack.isDown) {
            ms.mulPose(Axis.XP.rotationDegrees(AnimationTickHolder.getTicks() * 40f % 360f))
        }
        ms.popPose()
    }
}
