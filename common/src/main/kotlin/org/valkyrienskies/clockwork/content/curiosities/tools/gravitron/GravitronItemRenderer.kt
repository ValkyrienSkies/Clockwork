package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import net.createmod.catnip.animation.AnimationTickHolder
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.Sheets
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Quaternionf
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class GravitronItemRenderer : CustomRenderedItemModelRenderer() {
    private data class Joint(val model: PartialModel, val x: Float, val y: Float, val z: Float)
    private data class Arm(val axisX: Float, val axisY: Float, val joints: List<Joint>)

    // Actual Blockbench pivots, in pixels. Child joints inherit the parent's motion.
    private val arms = listOf(
        Arm(1f, 0f, listOf(
            Joint(ClockworkPartials.GRAV_PRONG_TOP_ONE, 7.70161f, 8.35355f, 1.69781f),
            Joint(ClockworkPartials.GRAV_PRONG_TOP_TWO, 7.69435f, 8.49629f, -0.98055f),
            Joint(ClockworkPartials.GRAV_PRONG_TOP_THREE, 7.69435f, 8.49629f, -3.98055f))),
        Arm(-0.707107f, 0.707107f, listOf(
            Joint(ClockworkPartials.GRAV_PRONG_LEFT_ONE, 6.24412f, 4.5858f, 1.71192f),
            Joint(ClockworkPartials.GRAV_PRONG_LEFT_TWO, 6.38686f, 4.72854f, -0.96644f),
            Joint(ClockworkPartials.GRAV_PRONG_LEFT_THREE, 6.38686f, 4.72854f, -3.96644f))),
        Arm(-0.707107f, -0.707107f, listOf(
            Joint(ClockworkPartials.GRAV_PRONG_RIGHT_ONE, 9.25588f, 4.5858f, 1.71192f),
            Joint(ClockworkPartials.GRAV_PRONG_RIGHT_TWO, 9.11314f, 4.72854f, -0.96644f),
            Joint(ClockworkPartials.GRAV_PRONG_RIGHT_THREE, 9.11314f, 4.72854f, -3.96644f)))
    )

    override fun render(stack: ItemStack, model: CustomRenderedItemModel, renderer: PartialItemModelRenderer,
                        transformType: ItemDisplayContext, ms: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
        val mc = Minecraft.getInstance()
        val player = GravitronEffects.renderedPlayer()
        val state = GravitronEffects.state(player)
        val now = mc.level?.gameTime ?: 0L
        val pt = AnimationTickHolder.getPartialTicks()
        val time = (now % 24000).toFloat() + pt
        val pose = state?.animation?.sample(now, pt) ?: GravitronAnimation.Pose(dial = 10f)
        val creative = stack.item is CreativeGravitronItem
        ms.pushPose()
        if (state != null) {
            val flip = if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ||
                transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) -1f else 1f
            ms.translate(0.0, (-pose.lowering * 0.38f).toDouble(), (pose.recoil * 0.24f + pose.lowering * 0.2f).toDouble())
            pivot(ms, 7.75f, 5f, 12f) {
                ms.mulPose(Axis.XP.rotationDegrees(pose.recoil * 19f - pose.lowering * 28f))
                ms.mulPose(Axis.ZP.rotationDegrees(pose.roll * flip))
            }
        }
        renderer.renderSolid(model.originalModel, light)
        renderDial(pose.dial, ms, renderer, light)
        if (player != null) {
            GravitronEffects.captureTip(player, transformType, ms, 3,
                Vector3f(GravitronVisuals.EMITTER_X / 16f - 0.5f,
                    GravitronVisuals.EMITTER_Y / 16f - 0.5f, GravitronVisuals.EMITTER_Z / 16f - 0.5f))
        }
        arms.forEachIndexed { index, arm ->
            ms.pushPose()
            val holding = state?.animation?.holding == true
            val twitch = if (holding || pose.strain > 0) {
                // Each actuator moves independently, with faster chatter as the load approaches its limit.
                (sin(time * 0.31f + index * 2.4f) + 0.35f * sin(time * 1.7f + index)) *
                    (0.65f + pose.strain * 3.5f)
            } else 0f
            val opening = pose.opening + twitch
            arm.joints.forEachIndexed { jointIndex, joint ->
                val angle = when (jointIndex) {
                    0 -> opening
                    1 -> -opening * 0.42f + twitch * 0.35f
                    else -> -opening * 0.2f
                }
                pivot(ms, joint.x, joint.y, joint.z) {
                    ms.mulPose(Quaternionf().rotationAxis(angle * (PI / 180).toFloat(), arm.axisX, arm.axisY, 0f))
                }
                renderer.renderSolid(joint.model.get(), light)
                if (jointIndex == 2 && player != null) {
                    GravitronEffects.captureTip(player, transformType, ms, index,
                        Vector3f(joint.x / 16f - 0.5f, joint.y / 16f - 0.5f, (joint.z - 1.5f) / 16f - 0.5f))
                }
            }
            ms.popPose()
        }
        if (creative) {
            // Solid item parts use a fixed buffer, while this translucent pass uses the shared
            // buffer. Flush the solid parts first so the full-sized aura blends over their depth.
            (buffer as? MultiBufferSource.BufferSource)?.endBatch(Sheets.solidBlockSheet())
            ms.pushPose()
            pivot(ms, GravitronVisuals.OVERCHARGE_X, GravitronVisuals.OVERCHARGE_Y, GravitronVisuals.OVERCHARGE_Z) {
                ms.mulPose(Axis.ZP.rotationDegrees(GravitronVisuals.coreAngle(time)))
            }
            renderer.render(ClockworkPartials.OVERLOAD_FX.get(), ClockworkRenderTypes.GRAVITRON_OVERCHARGE, 0xF000F0)
            ms.popPose()
        }
        if (state != null) renderCore(ms, buffer, pose, time)
        ms.popPose()
    }

    private fun renderCore(ms: PoseStack, buffer: MultiBufferSource, pose: GravitronAnimation.Pose, time: Float) {
        val energy = 0.12f + pose.energy * 0.65f
        val rgb = GravitronVisuals.WANDERLITE
        val vc = buffer.getBuffer(ClockworkRenderTypes.GRAVITRON_ENERGY)
        val matrix = ms.last().pose()
        val radius = 0.045f + pose.energy * 0.025f
        // Narrow rotating arcs around the emitter, rather than an opaque muzzle flash.
        for (i in 0 until 32) {
            if ((i / 6) % 2 == 0) continue
            for ((angle, r) in listOf(
                i * PI / 16 to radius, (i + 1) * PI / 16 to radius,
                (i + 1) * PI / 16 to radius + 0.015f, i * PI / 16 to radius + 0.015f)) {
                val a = angle + time * 0.12
                vc.vertex(matrix, GravitronVisuals.EMITTER_X / 16f - 0.5f + cos(a).toFloat() * r,
                    GravitronVisuals.EMITTER_Y / 16f - 0.5f + sin(a).toFloat() * r,
                    GravitronVisuals.EMITTER_Z / 16f - 0.5f - 0.015f)
                    .color((rgb shr 16 and 255) / 255f, (rgb shr 8 and 255) / 255f, (rgb and 255) / 255f, energy)
                    .uv(0.5f, 0.5f).endVertex()
            }
        }
    }

    private inline fun pivot(ms: PoseStack, x: Float, y: Float, z: Float, rotate: () -> Unit) {
        val px = x / 16.0 - 0.5
        val py = y / 16.0 - 0.5
        val pz = z / 16.0 - 0.5
        ms.translate(px, py, pz)
        rotate()
        ms.translate(-px, -py, -pz)
    }

    private fun renderDial(angle: Float, ms: PoseStack, renderer: PartialItemModelRenderer, light: Int) {
        ms.pushPose()
        // Keep the original exported mesh position and rest pose; only turn it about the hub.
        pivot(ms, GravitronVisuals.DIAL_X, GravitronVisuals.DIAL_Y, GravitronVisuals.DIAL_Z) {
            ms.mulPose(GravitronVisuals.dialRotation(angle))
        }
        renderer.renderSolid(ClockworkPartials.GRAV_DIAL_HAND.get(), light)
        ms.popPose()
    }
}
