package org.valkyrienskies.clockwork.util.blocktype;

import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.util.StringRepresentable;

public enum LiquidFuelType implements StringRepresentable {
    NONE,
    STALE,
    PLAIN,
    SWEET,
    GOURMET,
    EXTRA
    ;


    public static LiquidFuelType byIndex(int index) {
        return values()[index];
    }

    public boolean isAtLeast(LiquidFuelType fuelType) {
        return this.ordinal() >= fuelType.ordinal();
    }

    @Override
    public String getSerializedName() {
        return Lang.asId(name());
    }
}
