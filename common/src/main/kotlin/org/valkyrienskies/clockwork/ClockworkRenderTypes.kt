package org.valkyrienskies.clockwork

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.Util
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.ClockworkShaders.crystal

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
                256,
                true,
                false,
                compositeState!!
            )
        }
    }
}