package org.valkyrienskies.clockwork

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.Util
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.ClockworkShaders.crystal
import org.valkyrienskies.clockwork.ClockworkShaders.haze
import org.valkyrienskies.clockwork.ClockworkShaders.heat
import org.valkyrienskies.clockwork.platform.PlatformUtils

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
