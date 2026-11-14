package org.valkyrienskies.clockwork.api.thinkable;

/**
 * HOW 2 USE:
 * Declare a separate public, no-argument class under the `clockwork:thinkables` metadata key.
 * - Fabric uses entrypoints
 * - Forge uses modproperties.your_mod_id in mods.toml
 * Clockwork calls it on both physical sides after block registration. Keep references to this
 * interface OUT of your mod's normal entrypoint and block classes so you dont have to depend on clockwork being present
 */
public interface ThinkableEntrypoint {
    String KEY = "clockwork:thinkables";
    int API_VERSION = 2;

    void registerThinkables(ThinkableRegistry registry);
}
