package org.valkyrienskies.clockwork.mixin.client;


import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.util.render.BlockRenderTypeRegistry;

@Mixin(RenderType.class)
public abstract class MixinRenderType extends RenderStateShard {


    public MixinRenderType(String name, Runnable setupState, Runnable clearState) {
        super(name, setupState, clearState);
    }

    @Inject(
            method = "chunkBufferLayers",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void getBlockLayers(CallbackInfoReturnable<ImmutableList<Object>> info) {
        info.setReturnValue(
                ImmutableList.builder()
                        .addAll(info.getReturnValue())
                        .addAll(BlockRenderTypeRegistry.INSTANCE.getLayers()
                        ).build());
    }
}