package org.valkyrienskies.clockwork.fabric;

import net.fabricmc.loader.api.FabricLoader;
import org.valkyrienskies.clockwork.api.thinkable.ThinkableEntrypoint;
import org.valkyrienskies.clockwork.compat.thinkable.ThinkableCompat;
import java.util.Comparator;

public final class FabricThinkableCompat {
    private FabricThinkableCompat() {}

    public static void initialize() {
        ThinkableCompat.initialize(registry -> {
            var entries = FabricLoader.getInstance().getEntrypointContainers(ThinkableEntrypoint.KEY, ThinkableEntrypoint.class);
            entries.stream().sorted(Comparator.comparing(entry -> entry.getProvider().getMetadata().getId())).forEach(entry -> {
                try {
                    entry.getEntrypoint().registerThinkables(registry);
                } catch (RuntimeException | LinkageError error) {
                    throw new IllegalStateException("Failed to register Thinkable compatibility for " + entry.getProvider().getMetadata().getId(), error);
                }
            });
        });
    }
}
