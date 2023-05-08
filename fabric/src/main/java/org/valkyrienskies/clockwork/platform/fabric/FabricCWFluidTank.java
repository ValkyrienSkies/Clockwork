package org.valkyrienskies.clockwork.platform.fabric;

import com.simibubi.create.foundation.fluid.SmartFluidTank;
import io.github.fabricators_of_create.porting_lib.util.FluidStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import org.valkyrienskies.clockwork.util.fluid.CWFluidTank;

import java.util.function.Consumer;

public class FabricCWFluidTank extends SmartFluidTank implements CWFluidTank {
    public FabricCWFluidTank(long capacity, Consumer<FluidStack> updateCallback) {
        super(capacity, updateCallback);
    }

    @Override
    public long getTotalCapacity() {
        return super.getCapacity();
    }

    @Override
    public long getSpaceLeft() {
        return getSpace();
    }

    @Override
    public Fluid getFluidType() {
        return getFluid().getFluid();
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
    public void shrink(long drainAmount) {
        amount -= drainAmount;
        updateStack();
    }

    @Override
    public void grow(long fillAmount) {
        amount += fillAmount;
        updateStack();
    }

    private void updateStack() {
        this.stack = new FluidStack(this.variant, this.amount);
    }
}
