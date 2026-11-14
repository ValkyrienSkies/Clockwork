package org.valkyrienskies.clockwork

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import dev.architectury.event.events.client.ClientReloadShadersEvent
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceProvider
import org.valkyrienskies.clockwork.content.schematics.VdexBlueprintPreview
import java.io.IOException

object ClockworkShaders {

    private var blueprintGeometry: ShaderInstance? = null
    private var blueprintSketch: ShaderInstance? = null
    private var blueprintGhost: ShaderInstance? = null
    private var blueprintInk: ShaderInstance? = null
    fun blueprintGeometry() = blueprintGeometry
    fun blueprintSketch() = blueprintSketch
    fun blueprintGhost() = blueprintGhost
    fun blueprintInk() = blueprintInk

    private var crystal: ShaderInstance? = null
    private var heat: ShaderInstance? = null
    private var haze: ShaderInstance? = null
    private var meteorTrail: ShaderInstance? = null
    private var meteorGlow: ShaderInstance? = null
    private var meteorShieldDepth: ShaderInstance? = null
    private var meteorHeatShield: ShaderInstance? = null
    private var gravitronSurface: ShaderInstance? = null

    fun gravitronSurface(): ShaderInstance? = gravitronSurface

    fun crystal(): ShaderInstance {
        return crystal!!
    }

    fun heat(): ShaderInstance? {
        return heat
    }

    fun haze(): ShaderInstance? {
        return haze
    }

    fun meteorTrail(): ShaderInstance? = meteorTrail

    fun meteorGlow(): ShaderInstance? = meteorGlow

    fun meteorShieldDepth(): ShaderInstance? = meteorShieldDepth

    fun meteorHeatShield(): ShaderInstance? = meteorHeatShield

    fun setHeatShader(shader: ShaderInstance) {
        heat = shader
    }

    fun setHazeShader(shader: ShaderInstance) {
        haze = shader
    }

    fun setMeteorTrailShader(shader: ShaderInstance) {
        meteorTrail = shader
    }

    fun setMeteorGlowShader(shader: ShaderInstance) {
        meteorGlow = shader
    }

    fun setMeteorShieldDepthShader(shader: ShaderInstance) {
        meteorShieldDepth = shader
    }

    fun setMeteorHeatShieldShader(shader: ShaderInstance) {
        meteorHeatShield = shader
    }

    fun init() {
        ClientReloadShadersEvent.EVENT.register { resourceProvider: ResourceProvider, shadersSink: ClientReloadShadersEvent.ShadersSink ->
            try {
                shadersSink.registerShader(ShaderInstance(resourceProvider, "clockwork_blueprint_geometry", DefaultVertexFormat.NEW_ENTITY)) {
                    blueprintGeometry = it
                    VdexBlueprintPreview.clear()
                }
                shadersSink.registerShader(ShaderInstance(resourceProvider, "clockwork_blueprint_sketch", DefaultVertexFormat.POSITION_TEX)) { blueprintSketch = it }
                shadersSink.registerShader(ShaderInstance(resourceProvider, "clockwork_blueprint_ghost", DefaultVertexFormat.NEW_ENTITY)) { blueprintGhost = it }
                shadersSink.registerShader(ShaderInstance(resourceProvider, "clockwork_blueprint_ink", DefaultVertexFormat.POSITION_COLOR_TEX)) { blueprintInk = it }
                shadersSink.registerShader(
                    ShaderInstance(resourceProvider, "clockwork_gravitron_surface", DefaultVertexFormat.NEW_ENTITY)
                ) { inst ->
                    gravitronSurface = inst
                    org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronSurfaceEffects.clear()
                    org.valkyrienskies.clockwork.content.contraptions.phys.infuser.InfuserEffects.clear()
                    ClockworkModClient.WANDERWAND_EFFECT_RENDERER.reload()
                    org.valkyrienskies.clockwork.util.render.SurfaceEffectAtlas.clear()
                }
                shadersSink.registerShader(
                    ShaderInstance(
                        resourceProvider,
                        "crystal",
                        DefaultVertexFormat.NEW_ENTITY
                    )
                ) { inst -> crystal = inst }

                shadersSink.registerShader(
                    ShaderInstance(
                        resourceProvider,
                        "heat",
                        DefaultVertexFormat.NEW_ENTITY
                    )
                ) { inst -> heat = inst }

                shadersSink.registerShader(
                    ShaderInstance(
                        resourceProvider,
                        "haze",
                        DefaultVertexFormat.NEW_ENTITY
                    )
                ) { inst -> haze = inst }

                shadersSink.registerShader(
                    ShaderInstance(
                        resourceProvider,
                        "meteor_trail",
                        DefaultVertexFormat.NEW_ENTITY
                    )
                ) { inst -> meteorTrail = inst }

                shadersSink.registerShader(
                    ShaderInstance(
                        resourceProvider,
                        "meteor_glow",
                        DefaultVertexFormat.NEW_ENTITY
                    )
                ) { inst -> meteorGlow = inst }

                shadersSink.registerShader(
                    ShaderInstance(
                        resourceProvider,
                        "meteor_shield_depth",
                        DefaultVertexFormat.NEW_ENTITY
                    )
                ) { inst -> meteorShieldDepth = inst }

                shadersSink.registerShader(
                    ShaderInstance(
                        resourceProvider,
                        "meteor_heat_shield",
                        DefaultVertexFormat.NEW_ENTITY
                    )
                ) { inst -> meteorHeatShield = inst }

            } catch (ex: IOException) {
                //System.err.println("Failed to load shader")
                ex.printStackTrace()
            }
        }
    }

}
