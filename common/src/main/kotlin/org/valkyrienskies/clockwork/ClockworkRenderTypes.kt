package org.valkyrienskies.clockwork

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.Util
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.client.renderer.texture.TextureAtlas
import org.valkyrienskies.clockwork.ClockworkShaders.crystal
import org.valkyrienskies.clockwork.ClockworkShaders.haze
import org.valkyrienskies.clockwork.ClockworkShaders.heat
import org.valkyrienskies.clockwork.platform.PlatformUtils
import net.minecraft.client.renderer.MultiBufferSource
import com.mojang.blaze3d.vertex.VertexConsumer
import org.valkyrienskies.clockwork.util.render.EmissiveEnergyVertexConsumer
import org.valkyrienskies.clockwork.util.render.ShaderPackCompat
import org.valkyrienskies.clockwork.util.render.SurfaceEffectAtlas

class ClockworkRenderTypes(
    name: String,
    format: VertexFormat,
    mode: VertexFormat.Mode,
    bufferSize: Int,
    affectsCrumbling: Boolean,
    sortOnUpload: Boolean,
    setupState: Runnable,
    clearState: Runnable
) : RenderType(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState) {



    companion object {
        val BUFFER_SIZE = if (PlatformUtils.isModLoaded("rubidium") || PlatformUtils.isModLoaded("sodium") || PlatformUtils.isModLoaded("embeddium")) {
            262144
        } else {
            256
        }

        private val TEX = ResourceLocation("minecraft", "textures/misc/white.png")
        private val BEAM_TEX = ResourceLocation(ClockworkMod.MOD_ID, "textures/effects/beam.png")

        val CRYSTAL = Util.memoize { resourceLocation: ResourceLocation? ->
            val compositeState: CompositeState? =
                CompositeState.builder()
                    .setShaderState(ShaderStateShard(::crystal))
                    .setTextureState(TextureStateShard(resourceLocation!!, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .createCompositeState(true)
            create(
                ClockworkMod.MOD_ID + "crystal",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                BUFFER_SIZE,
                true,
                false,
                compositeState!!
            )
        }

        val HEAT = create(
            ClockworkMod.MOD_ID + "heat",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            BUFFER_SIZE,
            true,
            true,
            CompositeState.builder()
                .setShaderState(ShaderStateShard(::heat))
                .setTransparencyState(NO_TRANSPARENCY)
                .setCullState(CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setOutputState(TRANSLUCENT_TARGET)
                .createCompositeState(true)
        )

        val HAZE = create(
            ClockworkMod.MOD_ID + "haze",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            BUFFER_SIZE,
            true,
            true,
            CompositeState.builder()
                .setShaderState(ShaderStateShard(::haze))
                .setTransparencyState(ADDITIVE_TRANSPARENCY)
                .setCullState(CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setOutputState(TRANSLUCENT_TARGET)
                .createCompositeState(true)
        )

        val WANDER_LIGHTNING: RenderType = create(
            "wander_lightning_depthed",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(RenderStateShard.TextureStateShard(TEX, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY) // NOT additive
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                .setOverlayState(RenderStateShard.NO_OVERLAY)
                .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE) // don't write depth (optional)
                .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST) // depth test ON
                .createCompositeState(true)
        )

        // Reuses the ribbon vertex format and vanilla shader; soft energy never occludes later strands.
        val GRAVITRON_ENERGY: RenderType = create(
            "gravitron_energy", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS,
            4096, false, true,
            CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(TextureStateShard(TEX, false, false))
                .setTransparencyState(ADDITIVE_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .createCompositeState(false)
        )

        // Vanilla's emissive translucent pass blends texture alpha and only writes color.
        // Transparent areas of the energy model must not occlude the body or articulated prongs.
        val GRAVITRON_OVERCHARGE: RenderType = RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS, false)

        // These vanilla shader getters are intercepted by both Iris and Oculus and write the
        // shader pack's expected outputs. Custom core shaders cannot write its deferred buffers.
        private val TOOL_ENERGY_COMPAT = RenderType.entityTranslucentEmissive(TEX, false)
        val TOOL_SURFACE_COMPAT = toolSurface(false)
        val TOOL_FROZEN_COMPAT = toolSurface(true)

        fun energyType(): RenderType = if (ShaderPackCompat.enabled()) TOOL_ENERGY_COMPAT else GRAVITRON_ENERGY

        fun energyBuffer(buffers: MultiBufferSource): VertexConsumer {
            val type = energyType()
            val consumer = buffers.getBuffer(type)
            return if (type === TOOL_ENERGY_COMPAT) EmissiveEnergyVertexConsumer(consumer) else consumer
        }

        private fun toolSurface(frozen: Boolean): RenderType = create(
            if (frozen) "tool_frozen_compat" else "tool_surface_compat",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 262144, false, true,
            CompositeState.builder()
                .setShaderState(if (frozen) RENDERTYPE_ENTITY_TRANSLUCENT_SHADER else RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(TextureStateShard(SurfaceEffectAtlas.LOCATION, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setCullState(NO_CULL)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .setWriteMaskState(COLOR_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .createCompositeState(false)
        )

        val GRAVITRON_SURFACE: RenderType = create(
            "gravitron_surface", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
            262144, false, true,
            CompositeState.builder()
                .setShaderState(ShaderStateShard(ClockworkShaders::gravitronSurface))
                .setTextureState(TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(NO_OVERLAY)
                .setCullState(NO_CULL)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .setWriteMaskState(COLOR_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .createCompositeState(false)
        )

        val BEAM: RenderType = RenderType.create(
            "beam_depthed",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                .setTextureState(RenderStateShard.TextureStateShard(BEAM_TEX, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.NO_OVERLAY)
                .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                .createCompositeState(true)
        )

        val METEOR_TRAIL: RenderType = RenderType.entityTranslucent(ResourceLocation(ClockworkMod.MOD_ID, "textures/effects/meteor_trail.png"))
        val METEOR_PLASMA: RenderType = RenderType.entityTranslucent(ResourceLocation(ClockworkMod.MOD_ID, "textures/effects/meteor_plasma.png"))
    }
}
