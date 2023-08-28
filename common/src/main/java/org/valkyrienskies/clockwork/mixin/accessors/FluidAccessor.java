package org.valkyrienskies.clockwork.mixin.accessors;

import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Fluid.class)
public interface FluidAccessor {

    @Invoker("isEmpty")
    boolean getIfEmpty();

}
