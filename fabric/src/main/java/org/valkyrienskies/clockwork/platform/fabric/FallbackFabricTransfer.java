package org.valkyrienskies.clockwork.platform.fabric;

import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTransferable;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import org.valkyrienskies.clockwork.platform.SmartFluidTankBlockEntity;

public class FallbackFabricTransfer {

    public static void init() {
        FluidStorage.SIDED.registerFallback((world, pos, state, be, face) -> {
            if (be instanceof SmartFluidTankBlockEntity t) {
                return ((FabricCWFluidTankBehaviour) t.getFluidTankBehaviour()).getCapability();
            }
            return null;
        });
    }

}
