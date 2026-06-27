package org.valkyrienskies.clockwork.content.contraptions.phys.gimbal

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import kotlin.math.ln
import kotlin.math.min

class GimbalBearingRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<GimbalBearingBlockEntity>(
    context
) {
    override fun shouldRenderOffScreen(blockEntity: GimbalBearingBlockEntity): Boolean = true

    override fun shouldRender(blockEntity: GimbalBearingBlockEntity, cameraPos: Vec3): Boolean = true

    override fun renderSafe(
        be: GimbalBearingBlockEntity?,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        if (be == null) return
        if (!be.debugHasForceData) return
        if (!ClockworkConfig.CLIENT.debugRender && !Minecraft.getInstance().options.renderDebug) return

        val level = be.level ?: return
        val shipOn = level.getLoadedShipManagingPos(be.blockPos) as? ClientShip
        var blockOriginWorld = Vector3d(be.blockPos.x.toDouble(), be.blockPos.y.toDouble(), be.blockPos.z.toDouble())
        if (shipOn != null) {
            blockOriginWorld = shipOn.renderTransform.shipToWorld.transformPosition(blockOriginWorld, Vector3d())
        }

        fun worldToRenderLocal(world: Vector3dc): Vector3d = Vector3d(world).sub(blockOriginWorld)

        ms.pushPose()
        if (shipOn != null) {
            ms.mulPose(shipOn.renderTransform.rotation.invert(Quaterniond()).toFloat())
        }

        val vc = buffer.getBuffer(RenderType.lines())
        val pose = ms.last()

        val subAnchor = Vector3d(be.debugSubAnchorWorld)
        val hostAnchor = Vector3d(be.debugHostAnchorWorld)
        val currentPoint = Vector3d(be.debugCurrentPointWorld)
        val targetPoint = Vector3d(be.debugTargetPointWorld)

        renderCross(pose, vc, worldToRenderLocal(subAnchor), 0.16, CYAN)
        renderCross(pose, vc, worldToRenderLocal(hostAnchor), 0.16, BLUE)
        renderCross(pose, vc, worldToRenderLocal(currentPoint), 0.12, WHITE)
        renderCross(pose, vc, worldToRenderLocal(targetPoint), 0.12, GREEN)

        renderLine(pose, vc, worldToRenderLocal(currentPoint), worldToRenderLocal(targetPoint), YELLOW)

        renderWorldArrow(
            pose,
            vc,
            currentPoint,
            scaledDebugVector(be.debugForceWorld, be.debugMaxForce, FORCE_ARROW_LENGTH),
            blockOriginWorld,
            RED
        )
        renderWorldArrow(
            pose,
            vc,
            hostAnchor,
            scaledDebugVector(be.debugOppositeForceWorld, be.debugMaxForce, FORCE_ARROW_LENGTH),
            blockOriginWorld,
            MAGENTA
        )

        renderWorldArrow(pose, vc, subAnchor, normalizedDebugVector(be.debugCurrentAxisWorld, AXIS_ARROW_LENGTH), blockOriginWorld, CYAN)
        renderWorldArrow(pose, vc, hostAnchor, normalizedDebugVector(be.debugTargetAxisWorld, AXIS_ARROW_LENGTH), blockOriginWorld, GREEN)
        renderWorldArrow(pose, vc, hostAnchor, normalizedDebugVector(be.debugRedstoneWorld, REDSTONE_ARROW_LENGTH), blockOriginWorld, ORANGE)

        renderWorldArrow(
            pose,
            vc,
            currentPoint,
            scaledVelocityVector(be.debugControlledPointVelocityWorld),
            blockOriginWorld,
            WHITE
        )
        renderWorldArrow(
            pose,
            vc,
            targetPoint,
            scaledVelocityVector(be.debugTargetPointVelocityWorld),
            blockOriginWorld,
            GREEN_FAINT
        )
        renderWorldArrow(
            pose,
            vc,
            currentPoint,
            scaledVelocityVector(be.debugRelativeVelocityWorld),
            blockOriginWorld,
            YELLOW_FAINT
        )
        renderWorldArrow(
            pose,
            vc,
            subAnchor,
            scaledVelocityVector(be.debugSubAnchorVelocityWorld),
            blockOriginWorld,
            CYAN_FAINT
        )
        renderWorldArrow(
            pose,
            vc,
            hostAnchor,
            scaledVelocityVector(be.debugHostAnchorVelocityWorld),
            blockOriginWorld,
            BLUE_FAINT
        )
        renderWorldArrow(
            pose,
            vc,
            subAnchor,
            scaledVelocityVector(be.debugAnchorRelativeVelocityWorld),
            blockOriginWorld,
            ORANGE_FAINT
        )

        ms.popPose()
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)
    }

    private fun renderWorldArrow(
        pose: PoseStack.Pose,
        vc: VertexConsumer,
        startWorld: Vector3dc,
        vectorWorld: Vector3dc,
        blockOriginWorld: Vector3dc,
        color: Int
    ) {
        if (vectorWorld.lengthSquared() < 1e-12) return
        val start = Vector3d(startWorld).sub(blockOriginWorld)
        val end = Vector3d(startWorld).add(vectorWorld).sub(blockOriginWorld)
        renderArrow(pose, vc, start, end, color)
    }

    private fun renderArrow(
        pose: PoseStack.Pose,
        vc: VertexConsumer,
        start: Vector3dc,
        end: Vector3dc,
        color: Int
    ) {
        renderLine(pose, vc, start, end, color)

        val dir = end.sub(start, Vector3d())
        val len = dir.length()
        if (len < 1e-6) return
        dir.div(len)

        val sideSeed = if (kotlin.math.abs(dir.y()) < 0.9) Vector3d(0.0, 1.0, 0.0) else Vector3d(1.0, 0.0, 0.0)
        val side = dir.cross(sideSeed, Vector3d()).normalize()
        val back = Vector3d(end).fma(-min(0.25, len * 0.35), dir)
        val spread = min(0.12, len * 0.18)

        renderLine(pose, vc, end, Vector3d(back).fma(spread, side), color)
        renderLine(pose, vc, end, Vector3d(back).fma(-spread, side), color)
    }

    private fun renderCross(
        pose: PoseStack.Pose,
        vc: VertexConsumer,
        center: Vector3dc,
        radius: Double,
        color: Int
    ) {
        renderLine(pose, vc, Vector3d(center).add(-radius, 0.0, 0.0), Vector3d(center).add(radius, 0.0, 0.0), color)
        renderLine(pose, vc, Vector3d(center).add(0.0, -radius, 0.0), Vector3d(center).add(0.0, radius, 0.0), color)
        renderLine(pose, vc, Vector3d(center).add(0.0, 0.0, -radius), Vector3d(center).add(0.0, 0.0, radius), color)
    }

    private fun renderLine(
        pose: PoseStack.Pose,
        vc: VertexConsumer,
        start: Vector3dc,
        end: Vector3dc,
        argb: Int
    ) {
        val normal = end.sub(start, Vector3d())
        if (normal.lengthSquared() < 1e-12) normal.set(0.0, 1.0, 0.0) else normal.normalize()

        vc.vertex(pose.pose(), start.x().toFloat(), start.y().toFloat(), start.z().toFloat())
            .color((argb ushr 16) and 0xFF, (argb ushr 8) and 0xFF, argb and 0xFF, (argb ushr 24) and 0xFF)
            .normal(pose.normal(), normal.x().toFloat(), normal.y().toFloat(), normal.z().toFloat())
            .endVertex()
        vc.vertex(pose.pose(), end.x().toFloat(), end.y().toFloat(), end.z().toFloat())
            .color((argb ushr 16) and 0xFF, (argb ushr 8) and 0xFF, argb and 0xFF, (argb ushr 24) and 0xFF)
            .normal(pose.normal(), normal.x().toFloat(), normal.y().toFloat(), normal.z().toFloat())
            .endVertex()
    }

    private fun scaledDebugVector(vec: Vector3dc, referenceMagnitude: Double, maxLength: Double): Vector3d {
        val len = vec.length()
        if (len < 1e-9) return Vector3d()
        val displayLen = if (referenceMagnitude > 1e-9) {
            (len / referenceMagnitude).coerceIn(0.0, 1.0) * maxLength
        } else {
            min(maxLength, ln(1.0 + len) * 0.35)
        }
        return Vector3d(vec).normalize(displayLen)
    }

    private fun scaledVelocityVector(vec: Vector3dc): Vector3d {
        val len = vec.length()
        if (len < 1e-9) return Vector3d()
        return Vector3d(vec).normalize(min(VELOCITY_ARROW_LENGTH, ln(1.0 + len) * 0.45))
    }

    private fun normalizedDebugVector(vec: Vector3dc, length: Double): Vector3d {
        if (vec.lengthSquared() < 1e-12) return Vector3d()
        return Vector3d(vec).normalize(length)
    }

    private fun Quaterniond.toFloat() = org.joml.Quaternionf(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    companion object {
        private const val FORCE_ARROW_LENGTH = 3.0
        private const val VELOCITY_ARROW_LENGTH = 2.0
        private const val AXIS_ARROW_LENGTH = 1.25
        private const val REDSTONE_ARROW_LENGTH = 1.0

        private const val RED = 0xFFFF3030.toInt()
        private const val MAGENTA = 0xFFFF40FF.toInt()
        private const val CYAN = 0xFF30FFFF.toInt()
        private const val BLUE = 0xFF4070FF.toInt()
        private const val GREEN = 0xFF30FF50.toInt()
        private const val YELLOW = 0xFFFFFF30.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val ORANGE = 0xFFFFA030.toInt()

        private const val CYAN_FAINT = 0xAA30FFFF.toInt()
        private const val BLUE_FAINT = 0xAA4070FF.toInt()
        private const val GREEN_FAINT = 0xAA30FF50.toInt()
        private const val YELLOW_FAINT = 0xAAFFFF30.toInt()
        private const val ORANGE_FAINT = 0xAAFFA030.toInt()
    }
}
