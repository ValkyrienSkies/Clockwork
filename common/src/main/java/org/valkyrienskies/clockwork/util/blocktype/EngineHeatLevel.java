package org.valkyrienskies.clockwork.util.blocktype;

import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.util.StringRepresentable;

public enum EngineHeatLevel implements StringRepresentable {
    SMOULDERING, FADING, KINDLED, SEETHING, INFURIATED;

    public static EngineHeatLevel byIndex(int index) {
        return values()[index];
    }

    public EngineHeatLevel nextActiveLevel() {
        return byIndex(ordinal() % (values().length - 1) + 1);
    }

    public boolean isAtLeast(EngineHeatLevel heatLevel) {
        return this.ordinal() >= heatLevel.ordinal();
    }

    @Override
    public String getSerializedName() {
        return Lang.asId(name());
    }
}
