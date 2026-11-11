package org.valkyrienskies.clockwork.mixin.content.wanderwand;

import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "org.valkyrienskies.mod.common.assembly.ShipWelder$Move", remap = false)
public interface WeldMoveAccessor {
    @Accessor("from") BlockPos clockwork$getFrom();
    @Accessor("to") BlockPos clockwork$getTo();
}
