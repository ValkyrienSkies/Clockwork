package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import net.createmod.catnip.animation.AnimationTickHolder
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.Sheets
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.util.render.ShaderPackCompat
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.util.render.RenderUtil
import kotlin.math.sin
import kotlin.math.cos

class WanderwandItemRenderer : CustomRenderedItemModelRenderer() {
    override fun render(stack: ItemStack, model: CustomRenderedItemModel, renderer: PartialItemModelRenderer,
                        transformType: ItemDisplayContext, ms: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
        if (stack.item == ClockworkItems.INCOMPLETE_WANDERWAND.get()) {
            renderer.renderSolid(model.originalModel, light)
            return
        }
        val player = WanderwandHandEffects.player()
        val now = Minecraft.getInstance().level?.gameTime ?: 0
        val pt = AnimationTickHolder.getPartialTicks()
        val time = (now % 24000).toFloat() + pt
        val pose = WanderwandHandEffects.state(player)?.animation?.sample(now, pt) ?: WanderwandAnimation.Pose()
        val flip = if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) -1f else 1f
        ms.pushPose()
        ms.translate(0.0, -pose.lower * 0.45, pose.lower * 0.2)
        ms.mulPose(Axis.XP.rotationDegrees(pose.tilt))
        ms.mulPose(Axis.ZP.rotationDegrees(pose.roll * flip))
        renderer.renderSolid(model.originalModel, light)
        (buffer as? MultiBufferSource.BufferSource)?.endBatch(Sheets.solidBlockSheet())

        // All crystal layers rotate around their exported center (8, 12.5, 8), never the item origin.
        val centerY = 4.5f / 16f
        val bob = WandCrystalMotion.lift(now, pt) + pose.charge * 0.04f
        ms.pushPose()
        ms.translate(0.0, (centerY + bob).toDouble(), 0.0)
        if (player != null) WanderwandHandEffects.capture(player, transformType, ms, Vector3f())
        ms.mulPose(Axis.XP.rotationDegrees(WandCrystalMotion.angle(now, pt, 0)))
        ms.mulPose(Axis.YP.rotationDegrees(WandCrystalMotion.angle(now, pt, 1)))
        ms.mulPose(Axis.ZP.rotationDegrees(WandCrystalMotion.angle(now, pt, 2)))
        val size = 1f + pose.charge * 0.16f
        ms.scale(size, size, size)
        ms.translate(0.0, -centerY.toDouble(), 0.0)
        renderer.render(ClockworkPartials.CRYSTAL_INNER.get(), RenderType.endPortal(), 0xF000F0)
        val crystalType = if (ShaderPackCompat.enabled()) RenderType.entityTranslucentEmissive(RenderUtil.PURPLE_HUE)
            else ClockworkRenderTypes.CRYSTAL.apply(RenderUtil.CRYSTAL_MATRIX)
        renderer.render(ClockworkPartials.CRYSTAL.get(), crystalType, 0xF000F0)
        renderer.render(ClockworkPartials.CRYSTAL_OUTER.get(), RenderType.entityTranslucentEmissive(RenderUtil.PURPLE_HUE), 0xF000F0)
        ms.popPose()

        if (ShaderPackCompat.shadowPass()) {
            ms.popPose()
            return
        }
        val vc = ClockworkRenderTypes.energyBuffer(buffer)
        for (strand in 0..2) {
            var previous: Vec3? = null
            for (i in 0..8) {
                val angle = i / 8.0 * Math.PI * 1.3 + time * 0.08 + strand * 2.094
                val radius = 0.13 + sin(time * 0.35 + i * 1.8 + strand) * 0.014
                val point = Vec3(cos(angle) * radius, centerY + bob + sin(angle * 1.5) * 0.085, sin(angle) * radius)
                previous?.let { RenderUtil.addRibbonSegment(vc, ms.last().pose(), it, point, 0.003f + pose.charge * 0.002f,
                    0.765f, 0.627f, 0.89f, 0.25f + pose.charge * 0.5f) }
                previous = point
            }
        }
        ms.popPose()
    }
}
