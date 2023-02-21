package org.valkyrienskies.clockwork.util.fluid;

import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;

public interface CWFluidTank {

    long getTotalCapacity();

    long getAmount();

    default long getSpaceLeft() {
        return Math.max(0, getTotalCapacity() - getAmount());
    }

    Fluid getFluidType();

    default boolean isEmpty() {
        return getAmount() <= 0;
    }

    CompoundTag store(CompoundTag tag);

    void read(CompoundTag tag);

    void shrink(long drainAmount);

    void grow(long fillAmount);

    default SmartFluidTank asSmartFluidTank() {
        return (SmartFluidTank) this;
    }
}
