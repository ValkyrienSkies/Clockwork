package org.valkyrienskies.clockwork.forge;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.valkyrienskies.clockwork.api.thinkable.ThinkableEntrypoint;
import org.valkyrienskies.clockwork.compat.thinkable.ThinkableCompat;
import java.util.Comparator;
import java.util.List;

public final class ForgeThinkableCompat {
    private ForgeThinkableCompat() {}

    public static void initialize() {
        ThinkableCompat.initialize(registry -> {
            for (var mod : ModList.get().getMods().stream().sorted(Comparator.comparing(IModInfo::getModId)).toList()) {
                var configured = mod.getModProperties().get(ThinkableEntrypoint.KEY);
                if (configured == null) continue;
                if (!(configured instanceof List<?> names))
                    throw new IllegalStateException(mod.getModId() + ": " + ThinkableEntrypoint.KEY + " must be a list of class names");
                for (var value : names) {
                    if (!(value instanceof String name) || name.isBlank())
                        throw new IllegalStateException(mod.getModId() + ": invalid Thinkable entrypoint " + value);
                    try {
                        var type = Class.forName(name, true, ForgeThinkableCompat.class.getClassLoader()).asSubclass(ThinkableEntrypoint.class);
                        type.getConstructor().newInstance().registerThinkables(registry);
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                        throw new IllegalStateException("Failed to register Thinkable compatibility for " + mod.getModId() + ": " + name, error);
                    }
                }
            }
        });
    }
}
