package org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeControllerRenderer.Companion.rotateByPivot

class BladeItemRenderer: CustomRenderedItemModelRenderer() {
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

        if (stack.tag == null) return
        var bladeLength = stack.tag!!.getDouble("BladeLength").toFloat()
        if (bladeLength == 0f) bladeLength = 1f

        val wide = ClockworkItems.WIDE_PROPELLER_BLADE.asStack().`is`(stack.item)

        val bladeBasePartial: PartialModel = if (wide) ClockworkPartials.WIDEBLADE_BASE else ClockworkPartials.BLADE_BASE
        val bladeExtensionPartial: PartialModel = if (wide) ClockworkPartials.WIDEBLADE_EXTENSION else ClockworkPartials.BLADE_EXTENSION
        val bladeTipPartial: PartialModel = if (wide) ClockworkPartials.WIDEBLADE_TIP else ClockworkPartials.BLADE_TIP

        val bladeBase = bladeBasePartial.get()
        val bladeExtension =  bladeExtensionPartial.get()
        val bladeTip =  bladeTipPartial.get()

        ms.pushPose()
        renderer.render(bladeBase, light)
        ms.popPose()

        ms.pushPose()
        ms.translate(0f,0f,(bladeLength-1)*0.5f)
        ms.scale(1f,1f,bladeLength)
        renderer.render(bladeExtension, light)
        ms.popPose()

        ms.pushPose()
        ms.translate(0f,0f,-bladeLength + 1f)
        ms.translate(0f,0f,(bladeLength-1)*0.5f)
        renderer.render(bladeTip, light)
        ms.popPose()


    }


}
