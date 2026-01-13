package org.valkyrienskies.clockwork.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.valkyrienskies.clockwork.ClockworkRenderTypes;

import java.util.ArrayList;
import java.util.List;

@Mixin(RenderType.class)
public class MixinRenderType {

    @WrapMethod(
            method = "chunkBufferLayers"
    )
    private static List<RenderType> onChunkBufferLayers(Operation<List<RenderType>> original) {
        ArrayList<RenderType> types = new ArrayList<>(original.call());
        types.add(ClockworkRenderTypes.Companion.getREENTRY_FIRST());
        types.add(ClockworkRenderTypes.Companion.getREENTRY_SECOND());
        types.add(ClockworkRenderTypes.Companion.getREENTRY_THIRD());
        types.add(ClockworkRenderTypes.Companion.getREENTRY_FINAL());
        return types;
    }
}
