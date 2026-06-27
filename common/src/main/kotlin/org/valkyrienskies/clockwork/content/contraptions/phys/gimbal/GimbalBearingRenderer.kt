package org.valkyrienskies.clockwork.content.contraptions.phys.gimbal

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.atan2
import kotlin.math.sqrt

@OptIn(GameTickOnly::class)
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

        val level = be.level ?: return
        val blockState = be.blockState
        val vertexConsumer = buffer.getBuffer(RenderType.cutout())
        val hostShip = level.getLoadedShipManagingPos(be.blockPos) as? ClientShip
        val hostPose = createHostPose(be)
        val platePose = createPlatePose(be, hostShip) ?: hostPose

        renderPlate(blockState, platePose, ms, vertexConsumer, light, overlay)
        renderArms(blockState, hostPose, platePose, ms, vertexConsumer, light, overlay)

        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)
    }

    private fun createHostPose(be: GimbalBearingBlockEntity): RenderPose {
        val basis = basisForDirection(be.getRenderDirection())
        val faceCenter = Vector3d(0.5, 0.5, 0.5).fma(0.5, basis.normal)
        return RenderPose(faceCenter, basis.u, basis.normal, basis.v)
    }

    private fun createPlatePose(be: GimbalBearingBlockEntity, hostShip: ClientShip?): RenderPose? {
        if (!be.isRunning || be.shiptraptionID == GimbalBearingBlockEntity.NO_SHIPTRAPTION_ID) return null
        val level = be.level ?: return null
        val subShip = level.shipObjectWorld?.loadedShips?.getById(be.shiptraptionID) as? ClientShip ?: return null
        val direction = be.getRenderDirection()
        val savedAxis = be.getRenderBearingAxisLocal()
        val axisLocal = if (savedAxis.lengthSquared() > 1e-12) savedAxis else direction.normal.toJOMLD()
        val localBasis = basisForDirection(direction)

        val faceCenterWorld = subShip.renderTransform.shipToWorld.transformPosition(be.getRenderBearingPosInSub(), Vector3d())
        val normalWorld = subShip.renderTransform.rotation.transform(axisLocal, Vector3d()).normalize()
        val uWorld = subShip.renderTransform.rotation.transform(localBasis.u, Vector3d()).normalize()
        val vWorld = subShip.renderTransform.rotation.transform(localBasis.v, Vector3d()).normalize()

        return RenderPose(
            worldPointToRenderLocal(be, hostShip, faceCenterWorld),
            worldDirectionToRenderLocal(hostShip, uWorld),
            worldDirectionToRenderLocal(hostShip, normalWorld),
            worldDirectionToRenderLocal(hostShip, vWorld)
        ).orthonormalized()
    }

    private fun renderPlate(
        blockState: BlockState,
        platePose: RenderPose,
        ms: PoseStack,
        vertexConsumer: VertexConsumer,
        light: Int,
        overlay: Int
    ) {
        ms.pushPose()
        applyModelPose(ms, platePose, PLATE_FACE_CENTER_MODEL)
        CachedBuffers.partial(ClockworkPartials.GIMBAL_PLATE, blockState)
            .light<SuperByteBuffer>(light)
            .overlay<SuperByteBuffer>(overlay)
            .renderInto(ms, vertexConsumer)
        ms.popPose()
    }

    private fun renderArms(
        blockState: BlockState,
        hostPose: RenderPose,
        platePose: RenderPose,
        ms: PoseStack,
        vertexConsumer: VertexConsumer,
        light: Int,
        overlay: Int
    ) {
        val plateCenter = modelPoint(platePose, 0.5, ARM_TOP_Y, 0.5)
        val hostCenter = modelPoint(hostPose, 0.5, ARM_BOTTOM_Y, 0.5)
        for (corner in CORNERS) {
            val x = if (corner.xSign < 0) ARM_INSET else 1.0 - ARM_INSET
            val z = if (corner.zSign < 0) ARM_INSET else 1.0 - ARM_INSET
            val base = modelPoint(hostPose, x, ARM_BOTTOM_Y, z)
            val plateCorner = modelPoint(platePose, x, ARM_TOP_Y, z)
            val target = slidingTarget(base, plateCorner, plateCenter, ARM_LENGTH)
            val link = target.sub(base, Vector3d())
            if (link.lengthSquared() < 1e-10) continue

            val linkDir = link.normalize(Vector3d())
            val desiredRadial = target.sub(plateCenter, Vector3d())
            if (desiredRadial.lengthSquared() < 1e-10) {
                desiredRadial.set(base).sub(hostCenter)
            }
            val cornerRotation = Quaterniond(AxisAngle4d(corner.rotationRad, 0.0, 1.0, 0.0))
            val bottomModel = rotateModelCorner(ARM_BOTTOM_MODEL, cornerRotation)
            val modelRadial = rotateDirection(DEFAULT_CORNER_RADIAL, cornerRotation)
            val armRotation = quaternionFromDirectionAndRoll(DEFAULT_NORMAL, linkDir, modelRadial, desiredRadial)

            ms.pushPose()
            ms.translate(base.x, base.y, base.z)
            ms.mulPose(armRotation.toFloat())
            ms.translate(
                ARM_ROTATION_CENTER.x - bottomModel.x,
                ARM_ROTATION_CENTER.y - bottomModel.y,
                ARM_ROTATION_CENTER.z - bottomModel.z
            )
            ms.mulPose(cornerRotation.toFloat())
            ms.translate(-ARM_ROTATION_CENTER.x, -ARM_ROTATION_CENTER.y, -ARM_ROTATION_CENTER.z)
            CachedBuffers.partial(ClockworkPartials.GIMBAL_ARM, blockState)
                .light<SuperByteBuffer>(light)
                .overlay<SuperByteBuffer>(overlay)
                .renderInto(ms, vertexConsumer)
            ms.popPose()
        }
    }

    private fun applyModelPose(ms: PoseStack, pose: RenderPose, modelPivot: Vector3dc) {
        ms.translate(pose.origin.x, pose.origin.y, pose.origin.z)
        ms.mulPose(quaternionFromModelAxes(pose.normal, pose.v).toFloat())
        ms.translate(-modelPivot.x(), -modelPivot.y(), -modelPivot.z())
    }

    private fun modelPoint(pose: RenderPose, x: Double, y: Double, z: Double): Vector3d =
        Vector3d(pose.origin)
            .fma(x - PLATE_FACE_CENTER_MODEL.x, pose.u)
            .fma(y - PLATE_FACE_CENTER_MODEL.y, pose.normal)
            .fma(z - PLATE_FACE_CENTER_MODEL.z, pose.v)

    private fun slidingTarget(base: Vector3dc, corner: Vector3dc, center: Vector3dc, length: Double): Vector3d {
        val diagonal = center.sub(corner, Vector3d())
        val fromBase = corner.sub(base, Vector3d())
        val a = diagonal.lengthSquared()
        if (a < 1e-12) return Vector3d(corner)

        val b = 2.0 * fromBase.dot(diagonal)
        val c = fromBase.lengthSquared() - length * length
        val discriminant = b * b - 4.0 * a * c
        if (discriminant >= 0.0) {
            val sqrtDiscriminant = sqrt(discriminant)
            val t0 = (-b - sqrtDiscriminant) / (2.0 * a)
            val t1 = (-b + sqrtDiscriminant) / (2.0 * a)
            val t = listOf(t0, t1)
                .filter { it in 0.0..1.0 }
                .minByOrNull { kotlin.math.abs(it) }
            if (t != null) return Vector3d(corner).fma(t, diagonal)
        }

        val nearest = (-fromBase.dot(diagonal) / a).coerceIn(0.0, 1.0)
        return Vector3d(corner).fma(nearest, diagonal)
    }

    private fun worldPointToRenderLocal(be: GimbalBearingBlockEntity, hostShip: ClientShip?, world: Vector3dc): Vector3d {
        val blockOrigin = Vector3d(be.blockPos.x.toDouble(), be.blockPos.y.toDouble(), be.blockPos.z.toDouble())
        return if (hostShip != null) {
            hostShip.renderTransform.worldToShip.transformPosition(world, Vector3d()).sub(blockOrigin)
        } else {
            Vector3d(world).sub(blockOrigin)
        }
    }

    private fun worldDirectionToRenderLocal(hostShip: ClientShip?, worldDirection: Vector3dc): Vector3d =
        if (hostShip != null) {
            hostShip.renderTransform.worldToShip.transformDirection(worldDirection, Vector3d()).normalize()
        } else {
            Vector3d(worldDirection).normalize()
        }

    private fun quaternionFromModelAxes(normal: Vector3dc, v: Vector3dc): Quaterniond {
        val normalUnit = Vector3d(normal).normalize()
        val vUnit = Vector3d(v).normalize()
        val base = Quaterniond().rotationTo(DEFAULT_NORMAL, normalUnit)
        val currentV = base.transform(DEFAULT_V, Vector3d()).normalize()
        val roll = signedAngle(currentV, vUnit, normalUnit)
        return Quaterniond(AxisAngle4d(roll, normalUnit.x, normalUnit.y, normalUnit.z)).mul(base)
    }

    private fun quaternionFromDirectionAndRoll(
        defaultDirection: Vector3dc,
        targetDirection: Vector3dc,
        modelRadial: Vector3dc,
        desiredRadial: Vector3dc
    ): Quaterniond {
        val targetUnit = Vector3d(targetDirection).normalize()
        val base = Quaterniond().rotationTo(defaultDirection, targetUnit)
        val currentRadial = projectPerpendicular(base.transform(modelRadial, Vector3d()), targetUnit)
        val desired = projectPerpendicular(desiredRadial, targetUnit)
        if (currentRadial.lengthSquared() < 1e-10 || desired.lengthSquared() < 1e-10) return base
        currentRadial.normalize()
        desired.normalize()
        val roll = signedAngle(currentRadial, desired, targetUnit)
        return Quaterniond(AxisAngle4d(roll, targetUnit.x, targetUnit.y, targetUnit.z)).mul(base)
    }

    private fun signedAngle(from: Vector3dc, to: Vector3dc, axis: Vector3dc): Double {
        val cross = from.cross(to, Vector3d())
        return atan2(axis.dot(cross), from.dot(to))
    }

    private fun projectPerpendicular(vector: Vector3dc, normal: Vector3dc): Vector3d =
        Vector3d(vector).fma(-vector.dot(normal), normal)

    private fun rotateModelCorner(point: Vector3dc, rotation: Quaterniond): Vector3d =
        Vector3d(point).sub(ARM_ROTATION_CENTER).rotate(rotation).add(ARM_ROTATION_CENTER)

    private fun rotateDirection(direction: Vector3dc, rotation: Quaterniond): Vector3d =
        Vector3d(direction).rotate(rotation).normalize()

    private fun basisForDirection(direction: Direction): RenderBasis {
        val normal = direction.normal.toJOMLD()
        val u = when (direction) {
            Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH -> Vector3d(1.0, 0.0, 0.0)
            Direction.EAST -> Vector3d(0.0, 0.0, -1.0)
            Direction.WEST -> Vector3d(0.0, 0.0, 1.0)
        }
        val v = u.cross(normal, Vector3d()).normalize()
        return RenderBasis(u.normalize(), normal.normalize(), v)
    }

    private fun Quaterniond.toFloat() = Quaternionf(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    private data class RenderBasis(val u: Vector3d, val normal: Vector3d, val v: Vector3d)

    private data class RenderPose(val origin: Vector3d, val u: Vector3d, val normal: Vector3d, val v: Vector3d) {
        fun orthonormalized(): RenderPose {
            val normalUnit = Vector3d(normal).normalize()
            val uUnit = Vector3d(u).fma(-u.dot(normalUnit), normalUnit)
            if (uUnit.lengthSquared() < 1e-10) uUnit.set(1.0, 0.0, 0.0)
            uUnit.normalize()
            val vUnit = uUnit.cross(normalUnit, Vector3d()).normalize()
            return RenderPose(origin, uUnit, normalUnit, vUnit)
        }
    }

    private data class Corner(val xSign: Int, val zSign: Int, val rotationRad: Double)

    companion object {
        private val DEFAULT_NORMAL = Vector3d(0.0, 1.0, 0.0)
        private val DEFAULT_V = Vector3d(0.0, 0.0, 1.0)
        private val DEFAULT_CORNER_RADIAL = Vector3d(-1.0, 0.0, -1.0).normalize()

        private const val ARM_INSET = 5 / 16.0
        private const val ARM_VERTICAL_OFFSET = 8.5 / 16.0
        private const val ARM_BOTTOM_Y = 7.0 / 16.0 + ARM_VERTICAL_OFFSET
        private const val ARM_TOP_Y = 14.0 / 16.0 + ARM_VERTICAL_OFFSET
        private const val ARM_LENGTH = ARM_TOP_Y - ARM_BOTTOM_Y

        private val PLATE_FACE_CENTER_MODEL = Vector3d(0.5, 1.0, 0.5)
        private val ARM_ROTATION_CENTER = Vector3d(0.5, 0.0, 0.5)
        private val ARM_BOTTOM_MODEL = Vector3d(ARM_INSET, ARM_BOTTOM_Y, ARM_INSET)

        private val CORNERS = listOf(
            Corner(-1, -1, 0.0),
            Corner(-1, 1, Math.PI / 2.0),
            Corner(1, 1, Math.PI),
            Corner(1, -1, -Math.PI / 2.0)
        )
    }
}
