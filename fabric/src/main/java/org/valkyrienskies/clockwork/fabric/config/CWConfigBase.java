package org.valkyrienskies.clockwork.fabric.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraftforge.common.ForgeConfigSpec;

public abstract class CWConfigBase extends ConfigBase {
    @Override
    public void registerAll(final ForgeConfigSpec.Builder builder) {
        if (this.allValues != null) super.registerAll(builder);
    }
}
