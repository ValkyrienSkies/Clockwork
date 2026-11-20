package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import net.createmod.catnip.animation.AnimationTickHolder
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.Sheets
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.util.render.ShaderPackCompat
import kotlin.math.sin

class GatemakyrItemRenderer : CustomRenderedItemModelRenderer() {
    override fun render(stack: ItemStack, model: CustomRenderedItemModel, renderer: PartialItemModelRenderer,
                        context: ItemDisplayContext, ms: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        val mc = Minecraft.getInstance()
        val now = mc.level?.gameTime ?: 0L
        val pt = AnimationTickHolder.getPartialTicks()
        val time = (now % 24000).toFloat() + pt
        val state = GatemakyrClient.state(GatemakyrClient.renderedPlayer())
        val pose = state?.animation?.sample(now, pt) ?: GatemakyrAnimation.Pose(0f, 0f, 0f, 0f)
        ms.pushPose()
        ms.translate(0.0, -pose.recoil * 0.025, pose.recoil * 0.16)
        pivot(ms, 8f, 5f, 12f) { ms.mulPose(Axis.XP.rotationDegrees(-pose.recoil * 9)) }
        renderer.renderSolid(model.originalModel, light)
        for (i in 0..3) {
            ms.pushPose()
            pivot(ms, 8f, 5f, 0f) { ms.mulPose(Axis.ZP.rotationDegrees(i * 90f)) }
            val twitch = (sin(time * 0.43f + i * 1.7f) + sin(time * 1.17f + i) * 0.35f) * pose.charge * 2f
            val opening = pose.opening + twitch
            pivot(ms, 8f, 8.5f, 1f) { ms.mulPose(Axis.XP.rotationDegrees(opening)) }
            renderer.renderSolid(ClockworkPartials.GATEMAKYR_PRONG_BASE.get(), light)
            pivot(ms, 8f, 8f, -1.25f) { ms.mulPose(Axis.XP.rotationDegrees(-opening * 0.55f)) }
            renderer.renderSolid(ClockworkPartials.GATEMAKYR_PRONG_ARM.get(), light)
            pivot(ms, 8f, 8f, -3.75f) { ms.mulPose(Axis.XP.rotationDegrees(-opening * 0.3f + twitch)) }
            renderer.renderSolid(ClockworkPartials.GATEMAKYR_PRONG_TIP.get(), light)
            ms.popPose()
        }
        if (state != null && !ShaderPackCompat.shadowPass()) {
            val muzzle = Vec3(0.0, 5.0 / 16 - 0.5, -5.0 / 16 - 0.5)
            val transformed = ms.last().pose().transformPosition(Vector3f(muzzle.x.toFloat(), muzzle.y.toFloat(), muzzle.z.toFloat()))
            state.muzzle = Vec3(transformed.x.toDouble(), transformed.y.toDouble(), transformed.z.toDouble())
            state.muzzleTime = now
            state.muzzleFirstPerson = context.firstPerson()
            (buffers as? MultiBufferSource.BufferSource)?.endBatch(Sheets.solidBlockSheet())
            GatemakyrEffects.matrix(ms, buffers, muzzle, time, pose.energy, state.animation.side)
            if (state.animation.firedAt != Long.MIN_VALUE)
                GatemakyrEffects.firingPulse(ms, buffers, muzzle, (now - state.animation.firedAt + pt), state.animation.side)
        }
        ms.popPose()
    }

    private inline fun pivot(ms: PoseStack, x: Float, y: Float, z: Float, action: () -> Unit) {
        val px = x / 16.0 - 0.5; val py = y / 16.0 - 0.5; val pz = z / 16.0 - 0.5
        ms.translate(px, py, pz)
        action()
        ms.translate(-px, -py, -pz)
    }
}
