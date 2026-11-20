package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisChunkProgramOverrides;
import net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisChunkShaderInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.compat.gatemakyr.GateShaderPass;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrView;

@Pseudo
@Mixin(value = IrisChunkProgramOverrides.class, remap = false)
public abstract class MixinGateIrisChunks {
    @Inject(method = "getProgramOverride", at = @At("HEAD"), cancellable = true)
    private void clockwork$gateTerrainProgram(TerrainRenderPass pass, ChunkVertexType type, CallbackInfoReturnable<GlProgram<IrisChunkShaderInterface>> cir) {
        if (GatemakyrView.isolateShaders && GateShaderPass.current() != null && GateShaderPass.chunks() != (Object)this) {
            cir.setReturnValue(GateShaderPass.chunks().getProgramOverride(pass, type));
        }
    }
}
