package org.valkyrienskies.clockwork.content.curiosities.meteor

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.createmod.catnip.render.CachedBuffers
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.client.renderer.texture.OverlayTexture
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f
import org.joml.Vector4fc
import org.lwjgl.system.NativeResource
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.ClockworkShaders
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.hooks.VSGameEvents
import org.valkyrienskies.mod.common.util.toFloat
import kotlin.math.absoluteValue

object MeteorRenderer : NativeResource {

    fun onShipRender(ship: ClientShip, ms: PoseStack, camera: Camera, bufferSource: MultiBufferSource, pt: Float) {
        if (!ClockworkModClient.METEOR_SHIP_IDS.contains(ship.id)) return
        renderMeteor(ship, ms, camera, bufferSource, pt)
    }

    fun renderMeteor(ship: ClientShip, ms: PoseStack, camera: Camera, bufferSource: MultiBufferSource, pt: Float) {
        val state = MeteorManager.getState(ship.id) ?: return

        val shaderPass2 = ClockworkShaders.reentry(2)()
        val shaderPass3 = ClockworkShaders.reentry(3)()
        if (shaderPass2 != null) {
            attachUniforms(shaderPass2, state)
        }
        if (shaderPass3 != null) {
            attachUniforms(shaderPass3, state)
        }
    }

    fun attachUniforms(pass: ShaderInstance, state: MeteorVfxState) {
        pass.safeGetUniform("Direction").set(state.direction.normalize(Vector3f()))
        pass.safeGetUniform("Length").set(state.length)
        pass.safeGetUniform("Velocity").set(state.velocity)
        pass.safeGetUniform("Strength").set(state.strength)
        pass.safeGetUniform("Increment").set(state.increment)
        pass.safeGetUniform("TaperSize").set(state.taperSize)
        pass.safeGetUniform("LightColor").set(Vector4f(state.lightColor))
        pass.safeGetUniform("TrailColor").set(Vector4f(state.trailColor))
        pass.safeGetUniform("TrailColorHot").set(Vector4f(state.trailColorHot))
        pass.safeGetUniform("BowShockColor").set(Vector4f(state.bowShockColor))
        pass.safeGetUniform("BowShockOffset").set(state.bowShockOffset)
        pass.safeGetUniform("BowShockColorLerpOffset").set(state.bowShockColorLerpOffset)
    }

    fun tick() {
        val direction = Vector3f(0f, 0f, -1f)
        val length = 75f
        val velocity = 1.0f
        val strength = 1f
        val increment = 0.25f
        val taperSize = 0.5f
        val lightColor = Vector4f(1.0f, 0.5f, 0.2f, 1.0f)
        val trailColor = Vector4f(1.0f, 0.15f, 0.0f, 0.01f)
        val trailColorHot = Vector4f(1.0f, 0.85f, 0.3f, 0.05f)
        val bowShockColor = Vector4f(1.0f, 0.85f, 0.3f, 0.05f)
        val bowShockOffset = 0.5f
        val bowShockColorLerpOffset = 0.5f

        val state = MeteorVfxState(
            direction,
            length,
            velocity,
            strength,
            increment,
            taperSize,
            lightColor,
            trailColor,
            trailColorHot,
            bowShockColor,
            bowShockOffset,
            bowShockColorLerpOffset,
        )

        val shaderPass2 = ClockworkShaders.reentry(2)()
        val shaderPass3 = ClockworkShaders.reentry(3)()
        if (shaderPass2 != null) {
            attachUniforms(shaderPass2, state)
        }
        if (shaderPass3 != null) {
            attachUniforms(shaderPass3, state)
        }
    }

    override fun free() {

    }

    data class MeteorVfxState(
        var direction: Vector3fc,
        var length: Float,
        var velocity: Float,
        var strength: Float,
        var increment: Float,
        var taperSize: Float,
        var lightColor: Vector4fc,
        var trailColor: Vector4fc,
        var trailColorHot: Vector4fc,
        var bowShockColor: Vector4fc,
        var bowShockOffset: Float,
        var bowShockColorLerpOffset: Float,
    )
}
