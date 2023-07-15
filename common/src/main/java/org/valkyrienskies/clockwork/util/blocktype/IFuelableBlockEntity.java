package org.valkyrienskies.clockwork.util.blocktype;

public interface IFuelableBlockEntity {

    LiquidFuelType getFuelQuality();

    int getRemainingFuel();

    int getDrainRate();

}
