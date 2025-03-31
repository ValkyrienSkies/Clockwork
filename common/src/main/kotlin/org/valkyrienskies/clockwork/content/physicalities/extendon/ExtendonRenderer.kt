package org.valkyrienskies.clockwork.content.physicalities.extendon

import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.math.Matrix3f
import com.mojang.math.Matrix4f
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.outliner.Outliner
import com.simibubi.create.foundation.outliner.Outline
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
import org.joml.Vector3d
import org.lwjgl.opengl.GL11
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.*
import org.valkyrienskies.clockwork.util.*

class ExtendonRenderer(context: BlockEntityRendererProvider.Context?) : SmartBlockEntityRenderer<ExtendonBlockEntity>(context) {
    override fun shouldRenderOffScreen(blockEntity: ExtendonBlockEntity) = true

    override fun renderSafe(
        be: ExtendonBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        val vb = buffer.getBuffer(RenderType.cutout())

        var axis0 = CachedBufferer.partial(ClockworkPartials.EXTENDON_AXIS0,be.blockState)
        var axis1 = CachedBufferer.partial(ClockworkPartials.EXTENDON_AXIS1,be.blockState)

        if (be.connectedBe != null) {
            val thisShip = be.level!!.getShipManagingPos(be.blockPos) as ClientShip?
            val thisPos = if (thisShip == null) be.blockPos.toJOMLD() + 0.5 else thisShip.renderTransform.shipToWorld.transformPosition(be.blockPos.toJOMLD() + 0.5)!!

            val otherShip = be.level!!.getShipManagingPos(be.connectedBe!!.pos) as ClientShip?
            val otherPos = if (otherShip == null)be.connectedBe!!.pos.toJOMLD() + 0.5 else otherShip.renderTransform.shipToWorld.transformPosition(be.connectedBe!!.pos.toJOMLD() + 0.5)!!

            val direction = otherPos - thisPos

            val angles = if (thisShip == null) getEulerAngles(direction) else getEulerAngles(thisShip.renderTransform.worldToShip.transformDirection(direction, Vector3d()))

            //Rotate Partials
            axis0 = axis0.rotateCentered(Direction.UP, angles.second.toFloat())

            axis1 = axis1.rotateCentered(Direction.UP, angles.second.toFloat())
            axis1 = axis1.rotateCentered(Direction.WEST, angles.first.toFloat())

            if (be.main) {
                renderTubes(direction.length().toFloat(), ms, angles, be.blockPos, be.connectedBe!!.blockPos)
            }
        }

        axis0.light().renderInto(ms,vb)
        axis1.light().renderInto(ms,vb)

        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)
    }

    fun renderTubes(length: Float,
                    ms: PoseStack,
                    angles: Triple<Double, Double, Double>,
                    pos1: BlockPos, pos2: BlockPos,
                    ) {
        val (pitch, yaw, roll) = angles
        val level = Minecraft.getInstance().level!!

        ms.pushPose();
        var chain = TransformStack.cast(ms)
        chain.centre();

        chain.rotateYRadians(yaw)
        chain.rotateXRadians(-pitch)
        chain.rotateYRadians(PI / 4.0)

        chain.translate(0.5, 8 / 16.0, 0.5)
        chain.unCentre()

        //==========

        var radius = 13f / 16f / 2f

        var minU = 0f
        var maxU = 1f
        var minV = 0f
        var maxV = length / (15f/16f)

        var light1 = LightTexture.pack(
            level.getBrightness(LightLayer.BLOCK, pos1),
            level.getBrightness(LightLayer.SKY, pos1)
        )
        var light2 = LightTexture.pack(
            level.getBrightness(LightLayer.BLOCK, pos2),
            level.getBrightness(LightLayer.SKY, pos2)
        )

        //stupidity
        val tesselator = Tesselator.getInstance()
        val buf = tesselator.builder

        RenderSystem.disableBlend()
        RenderSystem.disableCull()
        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.setShader(GameRenderer::getRendertypeTranslucentShader)
        RenderSystem.setShaderTexture(0, texture)

        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer()

        buf.begin(VertexFormat.Mode.QUADS, GameRenderer.getRendertypeTranslucentShader()!!.vertexFormat)

        renderPart(buf, ms, length,
            0f, radius, -radius, 0f,
            radius, 0f, 0f, -radius,
            minU, maxU, minV, maxV,
            light1, light2
        )

        tesselator.end()

        RenderSystem.enableBlend()
        RenderSystem.enableCull()

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

        renderQuad(buf, matrix, normal, 0f, maxY, x0, x2, z0, z2, minU, maxU, minV, maxV, light1, light2)
        renderQuad(buf, matrix, normal, 0f, maxY, x1, x0, z1, z0, minU, maxU, minV, maxV, light1, light2)
        renderQuad(buf, matrix, normal, 0f, maxY, x3, x1, z3, z1, minU, maxU, minV, maxV, light1, light2)
        renderQuad(buf, matrix, normal, 0f, maxY, x2, x3, z2, z3, minU, maxU, minV, maxV, light1, light2)
    }

    fun renderQuad(
        buf: VertexConsumer,
        matrix: Matrix4f,
        normal: Matrix3f,
        minY: Float, maxY: Float,
        minX: Float, maxX: Float,
        minZ: Float, maxZ: Float,
        minU: Float, maxU: Float,
        minV: Float, maxV: Float,
        light1: Int, light2: Int
    ) {
        addVertex(buf, matrix, normal, minX, maxY, minZ, maxU, minV, light2)
        addVertex(buf, matrix, normal, minX, minY, minZ, maxU, maxV, light1)
        addVertex(buf, matrix, normal, maxX, minY, maxZ, minU, maxV, light1)
        addVertex(buf, matrix, normal, maxX, maxY, maxZ, minU, minV, light2)
    }

    fun addVertex(
        buf: VertexConsumer,
        matrix: Matrix4f,
        normal: Matrix3f,
        x: Float, y: Float, z: Float,
        u: Float, v: Float,
        light: Int,
    ) = buf
        .vertex(matrix, x, y, z)
        .color(255, 255, 255, 255)
        .uv(u, v)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light)
        .normal(normal, 0f, 1f, 0f)
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

// TODO: MOVE THIS TO UTILS
public fun Outliner.showCustomOutline(key: Any, outline: Outline) {
    this::class.java.getDeclaredMethod("addOutline", Any::class.java, Outline::class.java)
        .apply { isAccessible = true }
        .invoke(this, key, outline)
}

public fun Outliner.editCustomOutline(key: Any, outline: Outline) {
    @Suppress("UNCHECKED_CAST")
    this::class.java.getDeclaredField("outlines")
        .apply { isAccessible = true }
        .get(this)
        .let { it as MutableMap<Any, Outliner.OutlineEntry> }[key] = Outliner.OutlineEntry(outline)
}

public fun Outline.OutlineParams.disableFadeLineWidth() {
    this::class.java.getDeclaredField("fadeLineWidth")
        .apply { isAccessible = true }
        .set(this, false)

}