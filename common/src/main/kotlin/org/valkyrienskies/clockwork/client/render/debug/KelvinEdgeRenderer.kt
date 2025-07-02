package org.valkyrienskies.clockwork.client.render.debug

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import dev.architectury.platform.Platform
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.core.BlockPos
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.mod.api.vsApi

object KelvinEdgeRenderer {

    @JvmStatic
    fun render(level: ClientLevel, poseStack: PoseStack, camera: Camera) {
        val network = if (Minecraft.getInstance().isLocalServer && Platform.isFabric()) ClockworkMod.getKelvin() else ClockworkModClient.getKelvin()

        val cameraPos = camera.position

        poseStack.pushPose()

//        poseStack.mulPose(Vector3f.XP.rotationDegrees(camera.getXRot()))
//        poseStack.mulPose(Vector3f.YP.rotationDegrees(camera.getYRot() + 180.0f))
        poseStack.translate(-cameraPos.x,-cameraPos.y,-cameraPos.z)

        val tesselator = Tesselator.getInstance()
        val buf = tesselator.builder


        val matrix = poseStack.last().pose()

        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        RenderSystem.depthMask(false)
        RenderSystem.disableDepthTest()

        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR_LIGHTMAP)

        val r = 0
        val g = 255



        val edges = network.edges.clone() as HashMap<Pair<DuctNodePos, DuctNodePos>, DuctEdge>
        for (edge in edges) {


            var firstPosition = Vector3d(edge.key.first.x+0.5, edge.key.first.y+0.5, edge.key.first.z+0.5)
            val firstShip = vsApi.getShipManagingBlock(level, BlockPos(edge.key.first.x.toInt(), edge.key.first.y.toInt(), edge.key.first.z.toInt()))
            if (firstShip != null) firstPosition = (firstShip as ClientShip).renderTransform.shipToWorld.transformPosition(firstPosition)

            var secondPosition = Vector3d(edge.key.second.x+0.5, edge.key.second.y+0.5, edge.key.second.z+0.5)
            val secondShip = vsApi.getShipManagingBlock(level, BlockPos(edge.key.second.x.toInt(), edge.key.second.y.toInt(), edge.key.second.z.toInt()))
            if (secondShip != null) secondPosition = (secondShip as ClientShip).renderTransform.shipToWorld.transformPosition(secondPosition)

            val b = if (edge.value is PipeDuctEdge) 0 else 255

            val A = Vector3f(firstPosition.x.toFloat(), firstPosition.y.toFloat(), firstPosition.z.toFloat())
            val B = Vector3f(secondPosition.x.toFloat(), secondPosition.y.toFloat(), secondPosition.z.toFloat())

            renderLine(matrix, buf, A, B, r, g, b)
        }

        tesselator.end()
        poseStack.popPose()
    }

    fun renderLine(matrix: Matrix4f, buf: BufferBuilder, A: Vector3f, B: Vector3f, r: Int, g: Int, b: Int) {

        val light = Int.MAX_VALUE
        buf.vertex(matrix,  A.x(), A.y(), A.z()).color(r,g,b,255).uv2(light).endVertex()
        buf.vertex(matrix,  B.x(), B.y(), B.z()).color(r,g,b,255).uv2(light).endVertex()


    }

    // fun renderText(poseStack: PoseStack)
}