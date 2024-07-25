package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import com.jozufozu.flywheel.util.AnimationTickHolder
import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.util.EaseHelper.easeInOutSine
import org.valkyrienskies.clockwork.util.render.RenderUtil
import org.valkyrienskies.clockwork.util.render.TransformData
import kotlin.math.sin


class WanderwandItemRenderer() : CustomRenderedItemModelRenderer() {

    private var crystalAngle = 0f
    private var idleProgress = 0f

    override fun render(
        stack: ItemStack,
        model: CustomRenderedItemModel?,
        renderer: PartialItemModelRenderer?,
        transformType: ItemTransforms.TransformType?,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        if (stack.`is`(ClockworkItems.INCOMPLETE_WANDERWAND.get())) {
            ms.pushPose()
            renderer!!.renderSolid(model!!.originalModel, light)
            ms.popPose()
        }

        if (stack.`is`(ClockworkItems.WANDERWAND.get())) {
            val stacker = TransformStack.cast(ms)
            ms.pushPose()
            renderer!!.renderSolid(model!!.originalModel, light)
            val ww: WanderwandItem = stack.item as WanderwandItem
            animateIdle(ms, stacker, light, idleProgress, renderer)
            idleProgress += 0.1f
            if (idleProgress > 1.0f) {
                idleProgress = 0.0f
            }
            ms.popPose()
        }

    }

    private fun animateIdle(
        ms: PoseStack,
        stacker: TransformStack,
        light: Int,
        progress: Float,
        renderer: PartialItemModelRenderer
    ) {
        val partialTicks: Float = AnimationTickHolder.getPartialTicks() - 1
        val nextCrystalAngle = this.crystalAngle + 0.1f % 360
        val i = Mth.lerp(partialTicks, this.crystalAngle, nextCrystalAngle);


        val innerData = TransformData(Vector3f(0f, 0f, 0f), Vector3f(i, i, 0f))
        val data = TransformData(Vector3f(0f, 0f, 0f), Vector3f(0f, i, 0f))
        val heightAlt = 6f / 16f + sin(easeInOutSine(progress).toDouble()).toFloat() / 16f
        stacker.translateY((heightAlt * 0.05f).toDouble())
        ms.translate(0.0,heightAlt + 0.5,0.0)
        ms.pushPose()
        RenderUtil.renderCubeMatrix(ms, renderer, innerData, data, 1.5f, light)

        ms.popPose()
        this.crystalAngle = nextCrystalAngle
    }
}