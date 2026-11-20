package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.targets.RenderTargets", remap = false)
public interface GateIrisClearAccessor {
    @Accessor("fullClearRequired")
    void clockwork$requireGateClear(boolean clear);
}
