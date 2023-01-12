package org.valkyrienskies.clockwork.fabric.config;

import com.simibubi.create.foundation.config.ConfigBase;

public class CWClient extends CWConfigBase {

    public final ConfigGroup client = group(0, "client");

    @Override
    public String getName() {
        return "client";
    }

}
