package org.valkyrienskies.clockwork.util.fluid;

import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;

public interface CWFluidTank {

    int getTotalCapacity();

    int getCurrentAmount();

    default int getSpaceLeft() {
        return Math.max(0, getTotalCapacity() - getCurrentAmount());
    }

    Fluid getFluidType();

    default boolean isEmpty() {
        return getCurrentAmount() <= 0;
    }

    CompoundTag store(CompoundTag tag);

    void read(CompoundTag tag);

    void shrink(int drainAmount);

    void grow(int fillAmount);

    default SmartFluidTank asSmartFluidTank() {
        return (SmartFluidTank) this;
    }
}
