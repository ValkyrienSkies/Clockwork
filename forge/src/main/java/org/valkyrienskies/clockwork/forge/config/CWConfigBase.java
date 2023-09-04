package org.valkyrienskies.clockwork.forge.config;

import com.simibubi.create.foundation.config.ConfigBase;
import net.minecraftforge.common.ForgeConfigSpec;

public abstract class CWConfigBase extends ConfigBase {
    @Override
    public void registerAll(final ForgeConfigSpec.Builder builder) {
        if (this.allValues != null) super.registerAll(builder);
    }
}
