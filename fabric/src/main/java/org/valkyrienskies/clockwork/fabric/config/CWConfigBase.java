package org.valkyrienskies.clockwork.fabric.config;

import com.simibubi.create.foundation.config.ConfigBase;
import net.minecraftforge.common.ForgeConfigSpec;

public abstract class CWConfigBase extends ConfigBase {
    @Override
    protected void registerAll(final ForgeConfigSpec.Builder builder) { super.registerAll(builder); }
}
