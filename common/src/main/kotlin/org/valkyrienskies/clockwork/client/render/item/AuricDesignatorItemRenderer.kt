package org.valkyrienskies.clockwork.client.renderer.item

import com.jozufozu.flywheel.core.PartialModel
import com.jozufozu.flywheel.util.AnimationTickHolder
import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkMod.asResource
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.content.curiosities.tools.designator.AreaDesignatorItem
import org.valkyrienskies.clockwork.util.EaseHelper.easeInOutSine
import kotlin.math.sin


class AuricDesignatorItemRenderer() : CustomRenderedItemModelRenderer(){

    protected val CRYSTAL: PartialModel = PartialModel(asResource("item/auric_designator/crystal"))
    protected val CRYSTAL_OUTER: PartialModel = PartialModel(asResource("item/auric_designator/crystal_outer"))

    protected val POLE: PartialModel = PartialModel(asResource("item/auric_designator/pole"))

    protected val WAVE: PartialModel = PartialModel(asResource("item/auric_designator/wave"))

    private var crystalAngle = 0f

    override fun render(
        stack: ItemStack,
        model: CustomRenderedItemModel?,
        renderer: PartialItemModelRenderer?,
        transformType: ItemDisplayContext?,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        if (!stack.`is`(ClockworkItems.DESIGNATOR.get())) {
            return
        }
        val adi: AreaDesignatorItem = stack.item as AreaDesignatorItem

        val stacker = TransformStack.cast(ms)

        ms.pushPose()

        renderer!!.renderSolid(model!!.originalModel, light)


        animateIdle(ms, stacker, buffer, light, overlay, adi.idleProgress, renderer)

        ms.popPose()
    }

    private fun animateIdle(
        ms: PoseStack,
        stacker: TransformStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int,
        progress: Float,
        renderer: PartialItemModelRenderer
    ) {
        ms.pushPose()
        val partialTicks: Float = AnimationTickHolder.getPartialTicks() - 1
        val heightAlt = 3f / 16f + sin(easeInOutSine(progress).toDouble()).toFloat() / 16f
        stacker.translateY((heightAlt * 0.1f).toDouble())
        val nextCrystalAngle = Mth.clamp(this.crystalAngle + 0.1f, 0f, 360f)
        if (nextCrystalAngle == 360f) {
            this.crystalAngle = 0f
        }
        stacker.rotateY(Mth.lerp(partialTicks, this.crystalAngle, nextCrystalAngle).toDouble())
        this.crystalAngle = nextCrystalAngle
        renderer.render(CRYSTAL.get(), ClockworkRenderTypes.CRYSTAL.apply(asResource("textures/block/empty.png")), light)
        renderer.render(CRYSTAL_OUTER.get(), RenderType.entityTranslucent(asResource("textures/block/blue_hue.png")), light)
        stacker.translateY((-heightAlt * 0.1f).toDouble())
        ms.popPose()
        ms.pushPose()
        stacker.translateY((heightAlt * 0.05f).toDouble())
        renderer.renderSolid(POLE.get(), light)
        stacker.translateY((-heightAlt * 0.05f).toDouble())
        ms.popPose()
    }
}