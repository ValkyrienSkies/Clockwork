package org.valkyrienskies.clockwork.content.contraptions.phys.gimbal

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer.getAngleForBe
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.createmod.catnip.animation.AnimationTickHolder
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.AxisAngle4d
import org.joml.AxisAngle4f
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.gimbal.quad_link.QuadLinkRenderer
import org.valkyrienskies.clockwork.util.render.RenderUtil
import org.valkyrienskies.clockwork.util.render.TransformData
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.atan2

@OptIn(GameTickOnly::class)
class GimbalBearingRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<GimbalBearingBlockEntity>(
    context
) {
    override fun shouldRenderOffScreen(blockEntity: GimbalBearingBlockEntity): Boolean = true

    override fun shouldRender(blockEntity: GimbalBearingBlockEntity, cameraPos: Vec3): Boolean = true
    override fun renderSafe(be: GimbalBearingBlockEntity?, partialTicks: Float, ms: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)
        if (be !is GimbalBearingBlockEntity) return

        // Render Quad Link value boxes (frequency slots)
        QuadLinkRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay)


        val level = be.level ?: return
        val blockState = be.blockState
        val vertexConsumer = buffer.getBuffer(RenderType.translucent())
        val hostShip = level.getLoadedShipManagingPos(be.blockPos) as? ClientShip
        val hostPose = createHostPose(be)
        val platePose = createPlatePose(be, hostShip) ?: hostPose
        val gimbal_plate = CachedBuffers.partial(ClockworkPartials.GIMBAL_PLATE, blockState).light<SuperByteBuffer>(light)
        val gimbal_attacher = CachedBuffers.partial(ClockworkPartials.GIMBAL_ATTACHER, blockState).light<SuperByteBuffer>(light)
        val gimbal_top_attacher = CachedBuffers.partial(ClockworkPartials.GIMBAL_ATTACHER, blockState).light<SuperByteBuffer>(light)
        val gimbal_shaft = CachedBuffers.partial(ClockworkPartials.PHYS_SHAFT, blockState).light<SuperByteBuffer>(light)
        val gimbal_top_shaft = CachedBuffers.partial(ClockworkPartials.PHYS_SHAFT, blockState).light<SuperByteBuffer>(light)

        val axis = (blockState.block as IRotate).getRotationAxis(blockState)
        val angle = getAngleForBe(be, be.blockPos, axis);

        ms.pushPose()

        // Top shaft

        applyModelPose(gimbal_plate, platePose, PLATE_FACE_CENTER_MODEL)
        applyModelPose(gimbal_top_shaft, platePose, PLATE_FACE_CENTER_MODEL)
        applyModelPose(gimbal_top_attacher, platePose, PLATE_FACE_CENTER_MODEL)

        // The shaft ends up not quite aligned with connected shafts but I don't know how to fix it

        for (i in 0..3) {
            val arm = CachedBuffers.partial(ClockworkPartials.GIMBAL_ARM, blockState).light<SuperByteBuffer>(light)
            val arm_offset = when(i) {
                0 -> Vector3f(3/16f,14/16f,3/16f)
                1 -> Vector3f(3/16f,14/16f,13/16f)
                2 -> Vector3f(13/16f,14/16f,13/16f)
                else -> Vector3f(13/16f,14/16f,3/16f)
            }

            applyModelPose(arm, platePose, PLATE_FACE_CENTER_MODEL)
            arm.translate(arm_offset)
            arm.rotate(Math.toRadians(i*90.0).toFloat(), Vector3f(0f,1f,0f))
            arm.rotate(Quaternionf(0.1294095, 0.0170371, -0.1294095, 0.9829629))
            arm.renderInto(ms, vertexConsumer)
        }

        rotateDirection(gimbal_attacher, be)
        rotateDirection(gimbal_shaft, be)
        RotateShaft(gimbal_attacher, be, angle)
        RotateShaft(gimbal_shaft, be, angle)
        RotateShaft(gimbal_top_shaft, be, (angle)*(3/4))
        RotateShaft(gimbal_top_attacher, be, angle)

        gimbal_top_shaft.rotateCentered(AngleHelper.rad(180.0), Direction.Axis.X)
        gimbal_top_attacher.rotateCentered(AngleHelper.rad(180.0), Direction.Axis.X)

        gimbal_top_attacher.translate(0.0, 0.05, 0.0)

        gimbal_plate.renderInto(ms, vertexConsumer)
        gimbal_attacher.renderInto(ms, vertexConsumer)
        gimbal_shaft.renderInto(ms, vertexConsumer)
        gimbal_top_shaft.renderInto(ms, vertexConsumer)
        gimbal_top_attacher.renderInto(ms, vertexConsumer)

        //RENDER CRYSTAL MATRIX start
        val interpolatedAngle = be.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1)
        val innerData = TransformData(org.joml.Vector3f(0.5f,-1.0f,0.5f), org.joml.Vector3f(interpolatedAngle, interpolatedAngle, 0f))
        val data = TransformData(org.joml.Vector3f(0.5f,-1.0f,0.5f), org.joml.Vector3f(-interpolatedAngle, -interpolatedAngle, 0f))
        val outerData = TransformData(org.joml.Vector3f(0.5f,-1.0f,0.5f), org.joml.Vector3f(-interpolatedAngle, -interpolatedAngle, 0f))

        //RENDER CRYSTAL MATRIX end
        RenderUtil.renderCubeMatrix(ms, buffer, blockState, innerData, data, outerData, 1.5f,light,overlay)

        ms.popPose()
    }

    private fun rotateDirection(partial: SuperByteBuffer, be: BlockEntity) {
        val facing = be.blockState.getValue(BlockStateProperties.FACING)
        rotateDirection(partial, facing)
    }

    private fun rotateDirection(partial: SuperByteBuffer, facing: Direction) {
        partial.translate(0.5f)
        when (facing) {
            Direction.NORTH -> partial.rotate(Quaternionf(AxisAngle4f(AngleHelper.rad(270.0), 1f, 0f, 0f)))
            Direction.EAST -> {
                partial.rotate(Quaternionf(AxisAngle4f(AngleHelper.rad(270.0), 0f, 0f, 1f)))
                partial.rotate(Quaternionf(AxisAngle4f(AngleHelper.rad(90.0), 0f, 1f, 0f)))
            }
            Direction.SOUTH -> partial.rotate(Quaternionf(AxisAngle4f(AngleHelper.rad(90.0), 1f, 0f, 0f)))
            Direction.WEST -> {
                partial.rotate(Quaternionf(AxisAngle4f(AngleHelper.rad(90.0), 0f, 0f, 1f)))
                partial.rotate(Quaternionf(AxisAngle4f(AngleHelper.rad(90.0), 0f, 1f, 0f)))
            }
            Direction.UP -> partial.rotate(Quaternionf(AxisAngle4f(AngleHelper.rad(0.0), 1f, 0f, 0f)))
            Direction.DOWN -> partial.rotate(Quaternionf(AxisAngle4f(AngleHelper.rad(180.0), -1f, 0f, 0f)))
        }
        partial.translate(-0.5f)
    }

    private fun createHostPose(be: GimbalBearingBlockEntity): RenderPose {
        val basis = basisForDirection(be.getRenderDirection())
        val faceCenter = when (be.getRenderDirection()) {
            Direction.UP -> Vector3d(0.5, 0.0, 0.5).fma(0.5, basis.normal)
            Direction.DOWN -> Vector3d(0.5, 1.0, 0.5).fma(0.5, basis.normal)
            Direction.NORTH -> Vector3d(0.5, 0.5, 1.0).fma(0.5, basis.normal)
            Direction.SOUTH -> Vector3d(0.5, 0.5, 0.0).fma(0.5, basis.normal)
            Direction.EAST -> Vector3d(0.0, 0.5, 0.5).fma(0.5, basis.normal)
            Direction.WEST -> Vector3d(1.0, 0.5, 0.5).fma(0.5, basis.normal)
        }
        return RenderPose(faceCenter, basis.u, basis.normal, basis.v)
    }

    private fun createPlatePose(be: GimbalBearingBlockEntity, hostShip: ClientShip?): RenderPose? {
        if (!be.isRunning || be.connectedShipId == -1L) return null
        val level = be.level ?: return null
        val subShip = level.shipObjectWorld?.loadedShips?.getById(be.connectedShipId) as? ClientShip ?: return null
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

    fun RotateShaft(partial: SuperByteBuffer, be: BlockEntity,angle: Float) {
        val facing = be.blockState.getValue(BlockStateProperties.FACING)
        if (facing == Direction.UP || facing == Direction.SOUTH || facing == Direction.EAST) {
            partial.rotateCentered(angle, Direction.Axis.Y)
        }
        else {
            partial.rotateCentered(-angle, Direction.Axis.Y)
        }
    }

    private fun applyModelPose(partial: SuperByteBuffer, pose: RenderPose, modelPivot: Vector3dc) {
        partial.translate(pose.origin.x, pose.origin.y, pose.origin.z)
        partial.mulPose(Matrix4f().rotation(quaternionFromModelAxes(pose.normal, pose.v).toFloat()))
        partial.translate(-modelPivot.x(), -modelPivot.y(), -modelPivot.z())
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

    private fun signedAngle(from: Vector3dc, to: Vector3dc, axis: Vector3dc): Double {
        val cross = from.cross(to, Vector3d())
        return atan2(axis.dot(cross), from.dot(to))
    }

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

        private val PLATE_FACE_CENTER_MODEL = Vector3d(0.5, 0.5, 0.5)

    }
}
