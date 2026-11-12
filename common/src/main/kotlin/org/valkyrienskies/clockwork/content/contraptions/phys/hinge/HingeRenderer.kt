package org.valkyrienskies.clockwork.content.contraptions.phys.hinge

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.AllItems
import com.simibubi.create.content.equipment.goggles.GogglesItem
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class HingeRenderer(context: BlockEntityRendererProvider.Context) : SafeBlockEntityRenderer<HingeBlockEntity>() {
    override fun renderSafe(be: HingeBlockEntity, partialTicks: Float, pose: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        if (mc.options.hideGui || (mc.hitResult as? BlockHitResult)?.blockPos != be.blockPos) return
        if (!GogglesItem.isWearingGoggles(player) && !AllItems.WRENCH.isIn(player.mainHandItem)) return
        val level = be.level as? ClientLevel ?: return
        val reference = frame(level, be.guideBase ?: be.blockPos) ?: return
        val ownerRotation = Quaterniond(level.getLoadedShipManagingPos(be.blockPos)?.renderTransform?.shipToWorldRotation ?: Quaterniond())
        val localRotation = ownerRotation.invert().mul(reference)
        val billboard = Quaterniond(reference).invert().mul(Quaterniond(mc.entityRenderDispatcher.cameraOrientation()))
        val current = be.guideHead?.let { frame(level, it) }?.let { HingeGeometry.angle(reference, it) } ?: 0.0

        pose.pushPose()
        pose.translate(.5, .5, .5)
        pose.mulPose(Quaternionf(localRotation))
        val lines = buffers.getBuffer(RenderType.lines())
        arc(pose, lines, -180.0, 180.0, 1.04, 0x59636C, .4f)
        arc(pose, lines, 0.0, -be.lower.value.toDouble(), 1.04, HingeBlockEntity.NEGATIVE_COLOR, 1f)
        arc(pose, lines, 0.0, be.upper.value.toDouble(), 1.04, HingeBlockEntity.POSITIVE_COLOR, 1f)
        line(pose, lines, point(0.0, .8), point(0.0, 1.13), 0xDDE7EC)
        line(pose, lines, point(current, .45), point(current, 1.02), 0xFFFFFF)
        for (positive in listOf(false, true)) {
            val limit = (if (positive) be.upper.value else -be.lower.value).toDouble()
            val color = if (positive) HingeBlockEntity.POSITIVE_COLOR else HingeBlockEntity.NEGATIVE_COLOR
            line(pose, lines, point(limit, .91), point(limit, 1.18), color)
            val arrow = if (positive) 32.0 else -32.0
            arc(pose, lines, if (positive) 12.0 else -12.0, arrow, 1.4, color, 1f)
            val tip = point(arrow, 1.4)
            val back = arrow - if (positive) 7.0 else -7.0
            line(pose, lines, tip, point(back, 1.3), color)
            line(pose, lines, tip, point(back, 1.5), color)
        }
        label(pose, buffers, billboard, point(-48.0, 1.4), "−", HingeBlockEntity.NEGATIVE_COLOR)
        label(pose, buffers, billboard, point(48.0, 1.4), "+", HingeBlockEntity.POSITIVE_COLOR)
        label(pose, buffers, billboard, point(-be.lower.value.toDouble(), 1.23).add(-.14, 0.0, 0.0), "−${be.lower.value}°", HingeBlockEntity.NEGATIVE_COLOR)
        label(pose, buffers, billboard, point(be.upper.value.toDouble(), 1.23).add(.14, 0.0, 0.0), "+${be.upper.value}°", HingeBlockEntity.POSITIVE_COLOR)
        if (min(be.lower.value, be.upper.value) >= 20) label(pose, buffers, billboard, point(0.0, 1.23), "0°", 0xDDE7EC)
        pose.popPose()
    }

    private fun frame(level: ClientLevel, pos: BlockPos): Quaterniond? {
        val state = level.getBlockState(pos)
        if (state.block !is HingeBlock) return null
        return Quaterniond(level.getLoadedShipManagingPos(pos)?.renderTransform?.shipToWorldRotation ?: Quaterniond())
            .mul(HingeGeometry.orientation(state))
    }

    private fun point(degrees: Double, radius: Double): Vec3 {
        val angle = Math.toRadians(degrees)
        return Vec3(0.0, cos(angle) * radius, sin(angle) * radius)
    }

    private fun arc(pose: PoseStack, lines: VertexConsumer, start: Double, end: Double, radius: Double, color: Int, alpha: Float) {
        val segments = ceil(abs(end - start) / 5.0).toInt().coerceAtLeast(1)
        for (i in 0 until segments) line(pose, lines,
            point(start + (end - start) * i / segments, radius),
            point(start + (end - start) * (i + 1) / segments, radius), color, alpha)
    }

    private fun line(pose: PoseStack, lines: VertexConsumer, a: Vec3, b: Vec3, color: Int, alpha: Float = 1f) {
        val direction = b.subtract(a).normalize()
        if (direction.lengthSqr() < 1e-8) return
        for (point in listOf(a, b)) lines.vertex(pose.last().pose(), point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
            .color((color shr 16 and 255) / 255f, (color shr 8 and 255) / 255f, (color and 255) / 255f, alpha)
            .normal(pose.last().normal(), direction.x.toFloat(), direction.y.toFloat(), direction.z.toFloat()).endVertex()
    }

    private fun label(pose: PoseStack, buffers: MultiBufferSource, billboard: Quaterniond, position: Vec3, text: String, color: Int) {
        val font = Minecraft.getInstance().font
        pose.pushPose()
        pose.translate(position.x, position.y, position.z)
        pose.mulPose(Quaternionf(billboard))
        pose.scale(.018f, -.018f, .018f)
        font.drawInBatch(text, -font.width(text) / 2f, -4f, color or -0x1000000, false,
            pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0x40000000, LightTexture.FULL_BRIGHT)
        pose.popPose()
    }
}
