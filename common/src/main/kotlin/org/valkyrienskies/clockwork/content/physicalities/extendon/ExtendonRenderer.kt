package org.valkyrienskies.clockwork.content.physicalities.extendon

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.LightLayer
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.*
import org.valkyrienskies.clockwork.util.*
import org.valkyrienskies.core.api.util.GameTickOnly

class ExtendonRenderer(context: BlockEntityRendererProvider.Context?) : SmartBlockEntityRenderer<ExtendonBlockEntity>(context) {
    override fun shouldRenderOffScreen(blockEntity: ExtendonBlockEntity) = true
    override fun shouldRender(blockEntity: ExtendonBlockEntity, cameraPos: Vec3): Boolean = true

    @OptIn(GameTickOnly::class)
    override fun renderSafe(
        be: ExtendonBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        val vb = buffer.getBuffer(RenderType.cutout())
        val tubebuffer = buffer.getBuffer(RenderType.entitySolid(texture))

        var axis0 = CachedBuffers.partial(ClockworkPartials.EXTENDON_AXIS0,be.blockState)
        var axis1 = CachedBuffers.partial(ClockworkPartials.EXTENDON_AXIS1,be.blockState)

        axis0.light<SuperByteBuffer>(light).renderInto(ms,vb)
        axis1.light<SuperByteBuffer>(light).renderInto(ms,vb)

        if (be.connectedBe != null) {
            val thisShip = be.level!!.getShipManagingPos(be.blockPos) as ClientShip?
            val thisPos = if (thisShip == null) be.blockPos.toJOMLD() + 0.5 else thisShip.renderTransform.shipToWorld.transformPosition(be.blockPos.toJOMLD() + 0.5)!!

            val otherShip = be.level!!.getShipManagingPos(be.connectedBe!!.pos) as ClientShip?
            val otherPos = if (otherShip == null)be.connectedBe!!.pos.toJOMLD() + 0.5 else otherShip.renderTransform.shipToWorld.transformPosition(be.connectedBe!!.pos.toJOMLD() + 0.5)!!

            val direction = otherPos - thisPos

            val angles = if (thisShip == null) getEulerAngles(direction) else getEulerAngles(thisShip.renderTransform.worldToShip.transformDirection(direction, Vector3d()))

            //Rotate Partials
            axis0 = axis0.rotateCentered(angles.second.toFloat(), Direction.UP)

            axis1 = axis1.rotateCentered(angles.second.toFloat(), Direction.UP)
            axis1 = axis1.rotateCentered(angles.first.toFloat(), Direction.WEST)

            if (be.main) {
                val mainScale  = thisShip ?.transform?.scaling?.get(0) ?: 1.0
                val otherScale = otherShip?.transform?.scaling?.get(0) ?: 1.0

                val tubeRadiusMultiplier = if (otherScale < mainScale) (otherScale / mainScale).toFloat() else 1f
                val tubeLengthMultiplier = (1.0 / mainScale).toFloat()
                val tubeTextureMultiplier = if (otherScale > mainScale) 1f else (otherScale / mainScale).toFloat()

                renderTubes(direction.length().toFloat() * tubeLengthMultiplier, ms, angles, be.blockPos, be.connectedBe!!.blockPos, tubeRadiusMultiplier, tubeTextureMultiplier, tubebuffer)
            }
        }

        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)
    }

    fun renderTubes(length: Float,
                    ms: PoseStack,
                    angles: Triple<Double, Double, Double>,
                    pos1: BlockPos, pos2: BlockPos,
                    tubeRadiusScale: Float,
                    tubeLengthScale: Float,
                    buffer: VertexConsumer
                    ) {
        val (pitch, yaw, roll) = angles
        val level = Minecraft.getInstance().level!!

        ms.pushPose();
        val chain = TransformStack.of(ms)
        chain.center();

        chain.rotateY(yaw.toFloat())
        chain.rotateX((-pitch).toFloat())

        chain.translate(0.5, 8 / 16.0, 0.5)
        chain.uncenter()

        //==========

        val radius = 13f / 16f / 2f * tubeRadiusScale / sqrt(2f)

        val minU = 0f
        val maxU = 1f
        val minV = 0f
        val maxV = length / (15f/16f) / tubeLengthScale

        val light1 = LightTexture.pack(
            level.getBrightness(LightLayer.BLOCK, pos1),
            level.getBrightness(LightLayer.SKY, pos1)
        )
        val light2 = LightTexture.pack(
            level.getBrightness(LightLayer.BLOCK, pos2),
            level.getBrightness(LightLayer.SKY, pos2)
        )

        renderPart(buffer, ms, length,
            radius, radius, -radius, -radius,
            radius, -radius, radius, -radius,
            minU, maxU, minV, maxV,
            light1, light2
        )

        //==========

        ms.popPose()
    }

    fun renderPart(
        buf: VertexConsumer,
        poseStack: PoseStack,
        maxY: Float,
        x0: Float, x1: Float, x2: Float, x3: Float,
        z0: Float, z1: Float, z2: Float, z3: Float,
        minU: Float, maxU: Float,
        minV: Float, maxV: Float,
        light1: Int, light2: Int
    ) {
        val pose = poseStack.last()
        val matrix = pose.pose()
        val normal = pose.normal()

        renderQuad(buf, matrix, normal, 0f, maxY, x2, x0, z2, z0, 0f, 0f, 1f, minU, maxU, minV, maxV, light1, light2)
        renderQuad(buf, matrix, normal, 0f, maxY, x0, x1, z0, z1, 1f, 0f, 0f, minU, maxU, minV, maxV, light1, light2)
        renderQuad(buf, matrix, normal, 0f, maxY, x1, x3, z1, z3, 0f, 0f, -1f, minU, maxU, minV, maxV, light1, light2)
        renderQuad(buf, matrix, normal, 0f, maxY, x3, x2, z3, z2, -1f, 0f, 0f, minU, maxU, minV, maxV, light1, light2)
    }

    fun renderQuad(
        buf: VertexConsumer,
        matrix: Matrix4f,
        normal: Matrix3f,
        minY: Float, maxY: Float,
        minX: Float, maxX: Float,
        minZ: Float, maxZ: Float,
        normalX: Float, normalY: Float, normalZ: Float,
        minU: Float, maxU: Float,
        minV: Float, maxV: Float,
        light1: Int, light2: Int
    ) {
        addVertex(buf, matrix, normal, minX, maxY, minZ, normalX, normalY, normalZ, maxU, minV, light2)
        addVertex(buf, matrix, normal, minX, minY, minZ, normalX, normalY, normalZ, maxU, maxV, light1)
        addVertex(buf, matrix, normal, maxX, minY, maxZ, normalX, normalY, normalZ, minU, maxV, light1)
        addVertex(buf, matrix, normal, maxX, maxY, maxZ, normalX, normalY, normalZ, minU, minV, light2)
    }

    fun addVertex(
        buf: VertexConsumer,
        matrix: Matrix4f,
        normal: Matrix3f,
        x: Float, y: Float, z: Float,
        normalX: Float, normalY: Float, normalZ: Float,
        u: Float, v: Float,
        light: Int,
    ) = buf
        .vertex(matrix, x, y, z)
        .color(255, 255, 255, 255)
        .uv(u, v)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light)
        .normal(normal, normalX, normalY, normalZ)
        .endVertex()

    companion object {
        val texture = ResourceLocation(ClockworkMod.MOD_ID, "textures/block/hose.png")

        fun getEulerAngles(direction: Vector3d): Triple<Double, Double, Double> {
            // Calculate yaw (rotation around the Y-axis)
            val yaw = atan2(direction.x, direction.z)

            // Calculate pitch (rotation around the X-axis)
            val pitch = atan2(direction.y, sqrt(direction.x * direction.x + direction.z * direction.z))

            // Calculate roll (rotation around the Z-axis)
            // For a direction vector, roll can be calculated using the cross product
            val up = Vector3d(0.0, 1.0, 0.0)
            val right = direction.cross(up, Vector3d())
            val roll = atan2(right.y, right.x)

            // Return the angles in radians (pitch, yaw, roll)
            return Triple(pitch + Math.PI*3/2, yaw, roll)
        }
    }
}
