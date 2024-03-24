package org.valkyrienskies.clockwork.client.render.scanner


import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.GlConst
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.platform.TextureUtil
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import com.mojang.math.Matrix4f
import com.mojang.math.Vector3f
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.world.phys.Vec3
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkShaders
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserRenderer
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.impl.networking.RegisteredHandler
import org.valkyrienskies.mod.common.hooks.VSGameEvents
import org.valkyrienskies.mod.common.hooks.VSGameEvents.postRenderShip
import org.valkyrienskies.mod.common.hooks.VSGameEvents.renderShip
import org.valkyrienskies.mod.common.util.toJOML


@Environment(EnvType.CLIENT)
class ShipScannerRenderer : ScannerRenderer {
    // --------------------------------------------------------------------- //
    // Settings
    // --------------------------------------------------------------------- //
    // Frame buffer and depth texture IDs.
    private var depthCopyFbo = 0
    private var depthCopyColorBuffer = 0
    private var depthCopyDepthBuffer = 0

    // --------------------------------------------------------------------- //
    // State of the scanner, set when triggering a ping.
    private var currentStart: Long = 0
    private var currentCenter: Vec3? = null
    private var currentBlockEntity: PhysicsInfuserBlockEntity? = null
    private var ship: ClientShip? = null

    // --------------------------------------------------------------------- //
    override fun ping(ship: ClientShip?, pos: Vec3?, te: PhysicsInfuserBlockEntity) {
        currentStart = System.currentTimeMillis()
        currentCenter = pos
        currentBlockEntity = te
        if (ship == null) return
        ClockworkMod.LOGGER.info("Pinging ship: " + ship.id) // TODO implment it actualy using a ship
        this.ship = ship
    }

    override fun doRender(poseStack: PoseStack?) {
        val adjustedDuration: Int
        adjustedDuration = if (currentBlockEntity != null) {
            currentBlockEntity!!.scanGrowthDuration
        } else {
            PhysicsInfuserRenderer.Companion.SCAN_GROWTH_DURATION * Minecraft.getInstance().options.renderDistance / 12
        }
        val shouldRender = currentStart > 0 && adjustedDuration > (System.currentTimeMillis() - currentStart).toInt()
        if (shouldRender) {
            if (depthCopyFbo == 0) {
                createDepthCopyBuffer()
            }
            render(poseStack!!.last().pose())
        } else {
            if (depthCopyFbo != 0) {
                deleteDepthCopyBuffer()
            }
            currentStart = 0
        }
    }

    private fun render(viewMatrix: Matrix4f) {
        /*
        val scanEffect = ClockworkShaders.scan_effect
        if (scanEffect != null && ship != null) {
            val oldShader = RenderSystem.getShader()
            renderShip.on { event: VSGameEvents.ShipRenderEvent, _: RegisteredHandler? ->
                if (event.ship == ship) {
                    val target =
                        Minecraft.getInstance().mainRenderTarget
                    updateDepthTexture(target)
                    updateShaderUniforms(scanEffect, viewMatrix)
                    val width = target.width
                    val height = target.height
                    RenderSystem.depthMask(false)
                    RenderSystem.disableDepthTest()
                    RenderSystem.enableBlend()
                    RenderSystem.setShader(ClockworkShaders::crystal)
                    RenderSystem.backupProjectionMatrix()

                    RenderSystem.setProjectionMatrix(
                        Matrix4f.orthographic(
                            0f,
                            width.toFloat(),
                            0f,
                            height.toFloat(),
                            1f,
                            100f
                        )
                    )

                    val tesselator =
                        Tesselator.getInstance()
                    val buffer = tesselator.builder
                    buffer.begin(
                        VertexFormat.Mode.QUADS,
                        DefaultVertexFormat.POSITION_TEX
                    )
                    buffer.vertex(0.0, height.toDouble(), -50.0).uv(0f, 0f).endVertex()
                    buffer.vertex(width.toDouble(), height.toDouble(), -50.0).uv(1f, 0f).endVertex()
                    buffer.vertex(width.toDouble(), 0.0, -50.0).uv(1f, 1f).endVertex()
                    buffer.vertex(0.0, 0.0, -50.0).uv(0f, 1f).endVertex()
                    tesselator.end()
                }
            }
            postRenderShip.on { event: VSGameEvents.ShipRenderEvent, _: RegisteredHandler? ->
                if (event.ship == ship) {
                    RenderSystem.restoreProjectionMatrix()
                    RenderSystem.setShader { oldShader }
                    RenderSystem.depthMask(true)
                    RenderSystem.enableDepthTest()
                    RenderSystem.disableBlend()
                }
            }
        }

         */
    }

    private fun updateDepthTexture(target: RenderTarget) {
        val oldBuffer = GlStateManager.getBoundFramebuffer()
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId)
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, depthCopyFbo)
        GL30.glBlitFramebuffer(
            0, 0, target.width, target.height,
            0, 0, target.width, target.height,
            GL30.GL_DEPTH_BUFFER_BIT, GL30.GL_NEAREST
        )
        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, oldBuffer)
    }

    private fun updateShaderUniforms(shader: ShaderInstance, viewMatrix: Matrix4f) {
        val invertedViewMatrix = Matrix4f(viewMatrix)
        invertedViewMatrix.invert()
        val invertedProjectionMatrix = com.mojang.math.Matrix4f(RenderSystem.getProjectionMatrix())
        invertedProjectionMatrix.invert()
        val cameraPosition = Minecraft.getInstance().gameRenderer.mainCamera.position
        val adjustedDuration: Int
        val radius: Float
        if (currentBlockEntity != null) {
            adjustedDuration = currentBlockEntity!!.scanGrowthDuration
            radius = currentBlockEntity!!.computeRadius(currentStart, adjustedDuration.toFloat())
        } else {
            adjustedDuration =
                PhysicsInfuserRenderer.SCAN_GROWTH_DURATION * Minecraft.getInstance().options.renderDistance / 12
            radius = 0f
        }
        shader.setSampler("depthTex", depthCopyDepthBuffer)
        shader.safeGetUniform("center").set(Vector3f(currentCenter!!))
        shader.safeGetUniform("invViewMat").set(invertedViewMatrix)
        shader.safeGetUniform("invProjMat").set(invertedProjectionMatrix)
        shader.safeGetUniform("pos").set(Vector3f(cameraPosition))
        shader.safeGetUniform("radius").set(radius)
    }

    private fun blit(target: RenderTarget) {}

    // --------------------------------------------------------------------- //
    private fun createDepthCopyBuffer() {
        val target = Minecraft.getInstance().mainRenderTarget
        depthCopyFbo = GlStateManager.glGenFramebuffers()

        // We don't use the color attachment on this FBO, but it's required for a complete FBO.
        depthCopyColorBuffer =
            createTexture(target.width, target.height, GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE)

        // Main reason why we create this FBO: readable depth buffer into which we can copy the MC one.
        depthCopyDepthBuffer =
            createTexture(target.width, target.height, GL30.GL_DEPTH_COMPONENT, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT)
        val oldBuffer = GlStateManager.getBoundFramebuffer()
        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, depthCopyFbo)
        GlStateManager._glFramebufferTexture2D(
            GlConst.GL_FRAMEBUFFER,
            GlConst.GL_COLOR_ATTACHMENT0,
            GL11.GL_TEXTURE_2D,
            depthCopyColorBuffer,
            0
        )
        GlStateManager._glFramebufferTexture2D(
            GlConst.GL_FRAMEBUFFER,
            GlConst.GL_DEPTH_ATTACHMENT,
            GL11.GL_TEXTURE_2D,
            depthCopyDepthBuffer,
            0
        )
        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, oldBuffer)
    }

    private fun deleteDepthCopyBuffer() {
        GlStateManager._glDeleteFramebuffers(depthCopyFbo)
        depthCopyFbo = 0
        TextureUtil.releaseTextureId(depthCopyColorBuffer)
        depthCopyColorBuffer = 0
        TextureUtil.releaseTextureId(depthCopyDepthBuffer)
        depthCopyDepthBuffer = 0
    }

    private fun createTexture(width: Int, height: Int, internalFormat: Int, format: Int, type: Int): Int {
        val texture = TextureUtil.generateTextureId()
        GlStateManager._bindTexture(texture)
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT)
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
        GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, null)
        GlStateManager._bindTexture(0)
        return texture
    }
}