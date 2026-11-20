package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface GateFramebufferAccessor {
    @Mutable
    @Accessor("mainRenderTarget")
    void clockwork$setGateFramebuffer(RenderTarget target);
}
