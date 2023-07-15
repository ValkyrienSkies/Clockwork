package org.valkyrienskies.clockwork.platform;

import org.valkyrienskies.clockwork.util.fluid.CWFluidTankBehaviour;

public interface SmartFluidTankBlockEntity {

    CWFluidTankBehaviour getFluidTankBehaviour();
    void setFluidTankBehaviour(CWFluidTankBehaviour tank);

}
