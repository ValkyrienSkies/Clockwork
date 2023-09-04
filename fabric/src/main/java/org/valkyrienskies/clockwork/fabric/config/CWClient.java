package org.valkyrienskies.clockwork.fabric.config;

public class CWClient extends CWConfigBase {

    public final ConfigGroup client = group(0, "client");

    @Override
    public String getName() {
        return "client";
    }

}
