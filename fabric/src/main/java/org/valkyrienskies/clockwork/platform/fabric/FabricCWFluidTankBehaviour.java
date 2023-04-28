package org.valkyrienskies.clockwork.platform.fabric;

import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.tileEntity.behaviour.fluid.SmartFluidTankBehaviour;
import io.github.fabricators_of_create.porting_lib.util.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.level.material.Fluid;
import org.valkyrienskies.clockwork.util.fluid.CWFluidTankBehaviour;

import java.util.function.Consumer;

public class FabricCWFluidTankBehaviour extends CWFluidTankBehaviour {
    private InternalFluidHandler capability;

    protected FabricCWFluidTankBehaviour(BehaviourType<CWFluidTankBehaviour> type, SmartTileEntity te, int tanks, long tankCapacity, boolean enforceVariety) {
        super(type, te, tanks, tankCapacity, enforceVariety);
        Storage<FluidVariant>[] handlers = new Storage[tanks];
        for (int i = 0; i < tanks; i++) {
            handlers[i] = (FabricCWFluidTank) this.tanks[i].getTank();
        }

        capability = new InternalFluidHandler(handlers, enforceVariety);
    }

    @Override
    protected FabricCWFluidTank makeFluidTank(long capacity, Consumer<Fluid> updateCallback) {
        return new FabricCWFluidTank(capacity, f -> updateCallback.accept(f.getFluid()));
    }

    public Storage<FluidVariant> getCapability() {
        return capability;
    }

    public class InternalFluidHandler extends CombinedTankWrapper {

        public InternalFluidHandler(Storage<FluidVariant>[] handlers, boolean enforceVariety) {
            super(handlers);
            if (enforceVariety)
                enforceVariety();
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (!insertionAllowed)
                return 0;
            return super.insert(resource, maxAmount, transaction);
        }

        public long forceFill(FluidStack resource, TransactionContext ctx) {
            return super.insert(resource.getType(), resource.getAmount(), ctx);
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (!extractionAllowed)
                return 0;
            return super.extract(resource, maxAmount, transaction);
        }
    }
}
