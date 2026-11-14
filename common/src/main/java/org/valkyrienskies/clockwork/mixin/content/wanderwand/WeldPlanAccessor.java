package org.valkyrienskies.clockwork.mixin.content.wanderwand;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.valkyrienskies.mod.common.assembly.WeldRotation;

@Mixin(targets = "org.valkyrienskies.mod.common.assembly.ShipWelder$Plan", remap = false)
public interface WeldPlanAccessor {
    @Accessor("rotation") WeldRotation clockwork$getRotation();
    @Accessor("moves") List<?> clockwork$getMoves();
}
