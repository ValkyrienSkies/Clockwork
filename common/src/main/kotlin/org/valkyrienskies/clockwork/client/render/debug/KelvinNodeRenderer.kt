package org.valkyrienskies.clockwork.client.render.debug

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import dev.architectury.platform.Platform
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge

object KelvinNodeRenderer {
    fun render(context: WorldRenderContext) {
        val network = if (Minecraft.getInstance().isLocalServer && Platform.isFabric()) ClockworkMod.getKelvin() else ClockworkModClient.getKelvin()

        val poseStack = context.matrixStack()
        val camera = context.camera()
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


        for (edge in network.edges) {
            val b = if (edge.value is PipeDuctEdge) 0 else 255

            val A = Vector3f((edge.key.first.x+0.5).toFloat(), (edge.key.first.y+0.5).toFloat(), (edge.key.first.z+0.5).toFloat())
            val B = Vector3f((edge.key.second.x+0.5).toFloat(), (edge.key.second.y.toFloat()+0.5).toFloat(), (edge.key.second.z.toFloat()+0.5).toFloat())
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