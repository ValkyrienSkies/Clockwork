package org.valkyrienskies.clockwork.util.blocktype;

import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.util.StringRepresentable;

public enum FuelBoosterType implements StringRepresentable {
    NONE,
    BLAZE,
    STRATO
    ;

    public static FuelBoosterType byIndex(int index) {
        return values()[index];
    }

    public boolean isAtLeast(FuelBoosterType fuelBooster) {
        return this.ordinal() >= fuelBooster.ordinal();
    }

    @Override
    public String getSerializedName() {
        return Lang.asId(name());
    }
}
