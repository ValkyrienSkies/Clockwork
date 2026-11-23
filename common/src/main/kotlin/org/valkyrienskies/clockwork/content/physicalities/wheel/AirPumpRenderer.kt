package org.valkyrienskies.clockwork.content.physicalities.wheel

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import net.createmod.catnip.animation.AnimationTickHolder
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.ClockworkPartials
import kotlin.math.PI
import kotlin.math.sin

class AirPumpRenderer : CustomRenderedItemModelRenderer() {
    override fun render(stack: ItemStack, model: CustomRenderedItemModel, renderer: PartialItemModelRenderer,
                        transformType: ItemDisplayContext, ms: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        renderer.render(model.originalModel, light)
        ms.pushPose()
        val world = Minecraft.getInstance().level
        val tag = stack.tag
        if (world != null && tag != null && tag.contains("PumpStroke") &&
            (transformType.firstPerson() || transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ||
                transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND)) {
            val age = (world.gameTime - tag.getLong("PumpStroke")).toDouble() + AnimationTickHolder.getPartialTicks()
            if (age in 0.0..AirPumpItem.STROKE_TICKS.toDouble()) {
                val stroke = sin(PI * age / AirPumpItem.STROKE_TICKS)
                ms.translate(0.0, -2.5 / 16 * stroke * stroke, 0.0)
            }
        }
        renderer.render(ClockworkPartials.AIR_PUMP_HANDLE.get(), light)
        ms.popPose()
    }
}
