package org.valkyrienskies.clockwork.content.logistics.gas.backtank

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import net.createmod.catnip.animation.AnimationTickHolder
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.Sheets
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.ClockworkPartials

class GasBacktankItemRenderer : CustomRenderedItemModelRenderer() {

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
        val sheet = Sheets.cutoutBlockSheet()
        renderer.render(model.originalModel, sheet, light)

        val pressure = GasBacktankGauge.pressureFromItemStack(stack)
        val fill = GasBacktankGauge.fillFromPressure(pressure)
        val needleAngle = GasBacktankGauge.needleAngleDegrees(fill, pressure, AnimationTickHolder.getRenderTime())
        val px = (GasBacktankGauge.NEEDLE_PIVOT_X - 0.5f).toDouble()
        val py = (GasBacktankGauge.NEEDLE_PIVOT_Y - 0.5f).toDouble()
        val pz = (GasBacktankGauge.NEEDLE_PIVOT_Z - 0.5f).toDouble()

        ms.pushPose()
        ms.translate(px, py, pz)
        ms.mulPose(Axis.XP.rotationDegrees(needleAngle))
        ms.translate(-px, -py, -pz)
        renderer.render(ClockworkPartials.GAS_BACKTANK_NEEDLE.get(), sheet, light)
        ms.popPose()
    }
}
