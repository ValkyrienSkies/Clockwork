package org.valkyrienskies.clockwork.content.kinetics.universal_shaft

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.render.CachedBuffers
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3d
import org.lwjgl.opengl.GL11
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.minus
import org.valkyrienskies.clockwork.util.plus
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

class UniversalShaftRenderer(context: BlockEntityRendererProvider.Context?) : KineticBlockEntityRenderer<UniversalShaftBlockEntity>(context) {
    override fun shouldRenderOffScreen(blockEntity: UniversalShaftBlockEntity) = true
    override fun shouldRender(blockEntity: UniversalShaftBlockEntity, cameraPos: Vec3): Boolean = true

    override fun renderSafe(
        be: UniversalShaftBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)

        //if (Backend.canUseInstancing(be!!.level)) return

        val blockState = be.blockState

        val speed: Float = be.speed

        val tubeBuffer = buffer.getBuffer(RenderType.entitySolid(texture))

        if (be.connectedBe != null) {
            val thisShip = be.level!!.getShipManagingPos(be.blockPos) as ClientShip?
            val thisPos = if (thisShip == null) be.blockPos.toJOMLD() + 0.5 else thisShip.renderTransform.shipToWorld.transformPosition(be.blockPos.toJOMLD() + 0.5)!!

            val otherShip = be.level!!.getShipManagingPos(be.connectedBe!!.pos) as ClientShip?
            val otherPos = if (otherShip == null)be.connectedBe!!.pos.toJOMLD() + 0.5 else otherShip.renderTransform.shipToWorld.transformPosition(be.connectedBe!!.pos.toJOMLD() + 0.5)!!

            val direction = otherPos - thisPos

            val angles = if (thisShip == null) getEulerAngles(direction) else getEulerAngles(
                thisShip.renderTransform.worldToShip.transformDirection(direction, Vector3d())
            )

            if (be.main) {
                val mainScale  = thisShip ?.renderTransform?.scaling?.get(0) ?: 1.0
                val otherScale = otherShip?.renderTransform?.scaling?.get(0) ?: 1.0

                val tubeRadiusMultiplier = if (otherScale < mainScale) (otherScale / mainScale).toFloat() else 1f
                val tubeLengthMultiplier = (1.0 / mainScale).toFloat()
                val tubeTextureMultiplier = if (otherScale > mainScale) 1f else (otherScale / mainScale).toFloat()

                val roll = getAngleForBe(be, be.pos, be.blockState.getValue<Direction>(BlockStateProperties.FACING).axis)
                val realAngles = Triple(angles.first, angles.second, roll.toDouble())

                renderTubes(direction.length().toFloat() * tubeLengthMultiplier, ms, realAngles, be.blockPos, be.connectedBe!!.blockPos, tubeRadiusMultiplier, tubeTextureMultiplier, tubeBuffer)
            }
        }

        val vb = buffer.getBuffer(RenderType.cutout())
        val wheel = CachedBuffers.block(blockState)
        standardKineticRotationTransform(wheel, be, light)
        wheel.renderInto(ms, vb)
    }

    fun renderTubes(
        length: Float,
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
        chain.rotateX(-pitch.toFloat())
        chain.rotateY(-roll.toFloat())
        //chain.rotateY((PI / 4.0f).toFloat())


        chain.translate(0.5, 8 / 16.0, 0.5)
        chain.uncenter()

        //==========

        val radius = 6f / 16f / 2f * tubeRadiusScale / sqrt(2f)

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
        val texture = ResourceLocation(ClockworkMod.MOD_ID, "textures/block/compressible_axis.png")

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

//    private fun renderShaft(
//        be: UniversalShaftBlockEntity, ms: PoseStack, light: Int, blockState: BlockState, angle: Float,
//        vb: VertexConsumer
//    ) {
//        val wheel = CachedBuffers.block(blockState)
//        kineticRotationTransform(wheel, be, getRotationAxisOf(be), AngleHelper.rad(angle.toDouble()), light)
//        wheel.renderInto(ms, vb)
//    }

//    override fun getRenderedBlockState(be: UniversalShaftBlockEntity): BlockState {
//        return shaft(getRotationAxisOf(be))
//    }
}