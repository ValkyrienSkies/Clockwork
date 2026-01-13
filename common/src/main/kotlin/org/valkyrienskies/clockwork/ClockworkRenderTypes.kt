package org.valkyrienskies.clockwork

import com.google.common.collect.ImmutableList
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexSorting
import net.minecraft.Util
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.ClockworkShaders.crystal
import org.valkyrienskies.clockwork.ClockworkShaders.haze
import org.valkyrienskies.clockwork.ClockworkShaders.heat
import org.valkyrienskies.clockwork.ClockworkShaders.reentry
import org.valkyrienskies.clockwork.mixin.accessors.RenderStateShardAccessor
import org.valkyrienskies.clockwork.mixin.accessors.RenderTypeAccessor
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

        private val ALPHA_BLENDING_TRANSPARENCY = TransparencyStateShard("translucent_transparency", {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ZERO);
        }, {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        })

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
                ClockworkMod.MOD_ID + ":crystal",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                BUFFER_SIZE,
                true,
                false,
                compositeState!!
            )
        }

        val HEAT = create(
            ClockworkMod.MOD_ID + ":heat",
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
            ClockworkMod.MOD_ID + ":haze",
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
            ClockworkMod.MOD_ID + ":wander_lightning_depthed",
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
            ClockworkMod.MOD_ID + ":beam_depthed",
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

        val REENTRY_FIRST: RenderType = RenderType.create(
            ClockworkMod.MOD_ID + ":reentry/pass_1",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            786432,
            true,
            true,
            RenderType.CompositeState.builder()
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setTextureState(BLOCK_SHEET_MIPPED)
                .setShaderState(ShaderStateShard(reentry(1)))
                .createCompositeState(true)
        )

        val REENTRY_SECOND: RenderType = RenderType.create(
            ClockworkMod.MOD_ID + ":reentry/pass_2",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            786432,
            true,
            true,
            RenderType.CompositeState.builder()
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .setTransparencyState(ALPHA_BLENDING_TRANSPARENCY)
                .setShaderState(ShaderStateShard(reentry(2)))
                .createCompositeState(true)

        )

        val REENTRY_THIRD: RenderType = RenderType.create(
            ClockworkMod.MOD_ID + ":reentry/pass_3",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            786432,
            true,
            false,
            RenderType.CompositeState.builder()
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .setTransparencyState(ALPHA_BLENDING_TRANSPARENCY)
                .setShaderState(ShaderStateShard(reentry(3)))
                .createCompositeState(true)
        )

        val REENTRY_FINAL = layered(
            REENTRY_FIRST,
            REENTRY_SECOND,
            REENTRY_THIRD
        )



        // Registry method for a layered render type. Originally from Veil, all credits to the Veil team.
        /**
         * Creates a render type that uses a single draw buffer, but re-uses the data to draw the specified layers.
         *
         * @param layers The layers to use
         * @return A render type that draws all layers from a single buffer
         * @throws IllegalStateException If there are zero layers, the vertex formats don't all match, or the primitive modes don't match
         */
        fun layered(vararg layers: RenderType): RenderType {
            if (layers.size == 0) {
                throw IllegalArgumentException("At least 1 render type must be specified")
            }
            if (layers.size == 1) {
                return layers[0]
            }
            val builder = ImmutableList.builder<RenderType>()
            val format = layers[0].format()
            val mode = layers[0].mode()
            var bufferSize = layers[0].bufferSize()
            var sortOnUpload = (layers[0] as RenderTypeAccessor).isSortOnUpload()

            for (i in 1 until layers.size) {
                val layer = layers[i]
                if (layer.format() != format) {
                    throw IllegalArgumentException("Expected $layer to use $format, but was ${layer.format()}")
                }
                if (layer.mode() != mode) {
                    throw IllegalArgumentException("Expected $layer to use $mode, but was ${layer.mode()}")
                }
                bufferSize = maxOf(bufferSize, layer.bufferSize())
                if ((layer as RenderTypeAccessor).isSortOnUpload()) {
                    sortOnUpload = true
                }
                builder.add(layer)
            }
            return LayeredRenderType(
                layers[0],
                builder.build(),
                "LayeredRenderType[" + layers.joinToString(", ") { getName(it) } + "]",
                bufferSize,
                sortOnUpload
            )
        }

        public fun getName(shard: RenderStateShard): String {
            return (shard as RenderStateShardAccessor).getName()
        }

        class LayeredRenderType : RenderType {
            private val base: RenderType
            private val layers: List<RenderType>

            constructor(
                base: RenderType,
                layers: List<RenderType>,
                name: String,
                bufferSize: Int,
                sortOnUpload: Boolean
            ) : super(
                name,
                base.format(),
                base.mode(),
                bufferSize,
                base.affectsCrumbling(),
                sortOnUpload,
                {
                    base.setupRenderState()
                },
                {
                    base.clearRenderState()
                }
            ) {
                this.base = base
                this.layers = layers
            }

            override fun end(bufferBuilder: BufferBuilder, quadSorting: VertexSorting) {
                super.end(bufferBuilder, quadSorting)
                val shader = RenderSystem.getShader()
                if (shader == null || BufferUploader.lastImmediateBuffer == null) {
                    return
                }
                val modelViewMatrix = RenderSystem.getModelViewMatrix()
                val projectionMatrix = RenderSystem.getProjectionMatrix()

                for (layer in layers) {
                    layer.setupRenderState()
                    BufferUploader.lastImmediateBuffer?.drawWithShader(modelViewMatrix, projectionMatrix, shader)
                    layer.clearRenderState()
                }
            }
        }
    }
}
