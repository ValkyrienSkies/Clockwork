package org.valkyrienskies.clockwork.mixin.effekseer.client;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker double invokeGetFov(Camera camera, float partial, boolean useFovSetting);
}
