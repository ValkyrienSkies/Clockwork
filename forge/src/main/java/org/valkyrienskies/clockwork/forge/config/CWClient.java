package org.valkyrienskies.clockwork.forge.config;

import com.simibubi.create.foundation.config.ConfigBase;

public class CWClient extends CWConfigBase {

    public final ConfigGroup client = group(0, "client");

    @Override
    public String getName() {
        return "client";
    }

}
