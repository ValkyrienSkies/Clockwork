package org.valkyrienskies.clockwork.platform.forge;

import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.util.fluid.CWFluidTank;

import java.util.function.Consumer;

public class ForgeCWFluidTank extends SmartFluidTank implements CWFluidTank {
    public ForgeCWFluidTank(int capacity, Consumer<FluidStack> updateCallback) {
        super(capacity, updateCallback);
    }

    @Override
    public int getTotalCapacity() {
        return super.getCapacity();
    }

    @Override
    public int getCurrentAmount() {
        return super.getCapacity();
    }

    @Override
    public int getSpaceLeft() {
        return super.getSpace();
    }

    @Override
    public Fluid getFluidType() {
        return super.getFluid().getFluid();
    }

    @Override
    public CompoundTag store(CompoundTag tag) {
        return super.writeToNBT(tag);
    }

    @Override
    public void read(CompoundTag tag) {
        super.readFromNBT(tag);
    }

    @Override
    public void shrink(int drainAmount) {
        this.getFluid().shrink(drainAmount);
    }

    @Override
    public void grow(int fillAmount) {
        this.getFluid().grow(fillAmount);
    }

    @Nullable
    @Override
    public SmartFluidTank asSmartFluidTank() {
        return (SmartFluidTank) this;
    }
}
