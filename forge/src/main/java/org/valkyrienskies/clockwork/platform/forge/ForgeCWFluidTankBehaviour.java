package org.valkyrienskies.clockwork.platform.forge;

import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.tileEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.valkyrienskies.clockwork.util.fluid.CWFluidTank;
import org.valkyrienskies.clockwork.util.fluid.CWFluidTankBehaviour;

import java.util.function.Consumer;

public class ForgeCWFluidTankBehaviour extends CWFluidTankBehaviour {
    private LazyOptional<? extends IFluidHandler> capability;

    protected ForgeCWFluidTankBehaviour(BehaviourType<CWFluidTankBehaviour> type, SmartTileEntity te, int tanks, long tankCapacity, boolean enforceVariety) {
        super(type, te, tanks, tankCapacity, enforceVariety);

        IFluidHandler[] handlers = new IFluidHandler[tanks];

        for(int i = 0; i < tanks; ++i) {
            handlers[i] = (ForgeCWFluidTank) this.tanks[i].getTank();
        }

        this.capability = LazyOptional.of(() -> new InternalFluidHandler(handlers, enforceVariety));
    }

    @Override
    protected CWFluidTank makeFluidTank(long capacity, Consumer<Fluid> updateCallback) {
        return new ForgeCWFluidTank((int) capacity, f -> updateCallback.accept(f.getFluid()));
    }

    public LazyOptional<? extends IFluidHandler> getCapability() {
        return capability;
    }

    private class InternalFluidHandler extends CombinedTankWrapper {
        public InternalFluidHandler(IFluidHandler[] handlers, boolean enforceVariety) {
            super(handlers);
            if (enforceVariety) {
                this.enforceVariety();
            }

        }

        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            return !insertionAllowed ? 0 : super.fill(resource, action);
        }

        public int forceFill(FluidStack resource, IFluidHandler.FluidAction action) {
            return super.fill(resource, action);
        }

        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            return !extractionAllowed ? FluidStack.EMPTY : super.drain(resource, action);
        }

        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            return !extractionAllowed ? FluidStack.EMPTY : super.drain(maxDrain, action);
        }
    }
}
