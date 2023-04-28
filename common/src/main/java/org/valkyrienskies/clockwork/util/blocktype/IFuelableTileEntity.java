package org.valkyrienskies.clockwork.util.blocktype;

import net.minecraft.world.level.block.state.properties.EnumProperty;

public interface IFuelableTileEntity {

    LiquidFuelType getFuelQuality();

    int getRemainingFuel();

    int getDrainRate();

    FuelBoosterType getFuelBooster();

}
