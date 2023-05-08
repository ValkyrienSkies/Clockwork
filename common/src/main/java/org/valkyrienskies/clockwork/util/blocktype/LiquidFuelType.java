package org.valkyrienskies.clockwork.util.blocktype;

import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.Fluid;
import org.valkyrienskies.clockwork.data.ClockWorkTags;

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

    public static LiquidFuelType fromFluid(Fluid fuel) {
        if (ClockWorkTags.AllFluidTags.STALE.matches(fuel)) {
            return LiquidFuelType.STALE;
        } else if (ClockWorkTags.AllFluidTags.PLAIN.matches(fuel)) {
            return LiquidFuelType.PLAIN;
        } else if (ClockWorkTags.AllFluidTags.SWEET.matches(fuel)) {
            return LiquidFuelType.SWEET;
        } else if (ClockWorkTags.AllFluidTags.GOURMET.matches(fuel)) {
            return LiquidFuelType.GOURMET;
        } else if (ClockWorkTags.AllFluidTags.EXTRA.matches(fuel)) {
            return LiquidFuelType.EXTRA;
        } else {
            return LiquidFuelType.NONE;
        }
    }

    public boolean isAtLeast(LiquidFuelType fuelType) {
        return this.ordinal() >= fuelType.ordinal();
    }

    @Override
    public String getSerializedName() {
        return Lang.asId(name());
    }
}
