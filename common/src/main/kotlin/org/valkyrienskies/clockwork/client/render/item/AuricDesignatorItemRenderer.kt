package org.valkyrienskies.clockwork.client.renderer.item

import com.jozufozu.flywheel.core.PartialModel
import com.jozufozu.flywheel.util.AnimationTickHolder
import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod.asResource
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.content.curiosities.tools.designator.AreaDesignatorItem
import org.valkyrienskies.clockwork.util.EaseHelper.easeInOutSine
import kotlin.math.sin


class AuricDesignatorItemRenderer() : CustomRenderedItemModelRenderer(){

    protected val CRYSTAL: PartialModel = PartialModel(asResource("item/auric_designator/crystal"))
    protected val CRYSTAL_OUTER: PartialModel = PartialModel(asResource("item/auric_designator/crystal_outer"))
    protected val CRYSTAL_INNER: PartialModel = PartialModel(asResource("item/auric_designator/crystal_inner"))

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


        animateIdle(ms, stacker, light, adi.idleProgress, renderer)

        ms.popPose()
    }

    private fun animateIdle(
        ms: PoseStack,
        stacker: TransformStack,
        light: Int,
        progress: Float,
        renderer: PartialItemModelRenderer
    ) {
        ms.pushPose()
        val partialTicks: Float = AnimationTickHolder.getPartialTicks() - 1
        val heightAlt = 3f / 16f + sin(easeInOutSine(progress).toDouble()).toFloat() / 16f

        val nextCrystalAngle = this.crystalAngle + 0.1f % 360
        ms.pushPose()

        stacker.translateY((heightAlt * 4 + 0.055))

        renderCube(ms, partialTicks, nextCrystalAngle, renderer, light)
        ms.popPose()
        stacker.translateY((heightAlt * 0.1f).toDouble())
        stacker.rotateY(Mth.lerp(partialTicks, this.crystalAngle, nextCrystalAngle).toDouble())
        this.crystalAngle = nextCrystalAngle

        renderer.render(CRYSTAL.get(), ClockworkRenderTypes.CRYSTAL.apply(asResource("textures/block/empty.png")), light)
        renderer.render(CRYSTAL_OUTER.get(), RenderType.entityTranslucent(asResource("textures/block/purple_hue.png")), light)
        stacker.translateY((-heightAlt * 0.1f).toDouble())
        ms.popPose()
        ms.pushPose()
        stacker.translateY((heightAlt * 0.05f).toDouble())
        renderer.renderSolid(POLE.get(), light)
        stacker.translateY((-heightAlt * 0.05f).toDouble())
        ms.popPose()
    }

    private fun renderCube(ms: PoseStack, partialTicks: Float, nextCrystalAngle: Float, renderer: PartialItemModelRenderer, light: Int){
        ms.pushPose()
        val xOffset = 8 / 16f
        val i = Mth.lerp(partialTicks, this.crystalAngle, nextCrystalAngle);
        ms.translate(0f, -xOffset,  0f)
        ms.mulPose(Axis.YP.rotationDegrees(i))
        ms.mulPose(Axis.XP.rotationDegrees(i))
        ms.translate(0f, xOffset, 0f)
        renderer.render(CRYSTAL_INNER.get(), RenderType.endPortal(), light)
        ms.popPose()
    }
}