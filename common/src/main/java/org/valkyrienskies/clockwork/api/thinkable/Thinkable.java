package org.valkyrienskies.clockwork.api.thinkable;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public interface Thinkable {
    default boolean providesThinkable(Direction side) { return false; }
    default boolean acceptsThinkable(Direction side) { return false; }

    /** Server-side reading; null means unavailable. Values must be finite or kablooey */
    @Nullable
    default Double thinkableValue(Direction side) { return null; }

    /** Available readings for this face, queried on either side. Use stable IDs and include units in names. */
    default List<ThinkableReading> thinkableReadings(Direction side) { return List.of(); }

    /** Empty ID selects whatever default is. Unknown/unavailable IDs return null. */
    @Nullable
    default Double thinkableValue(Direction side, String reading) {
        return reading.isEmpty() ? thinkableValue(side) : null;
    }

    /** Reading requested by this input */
    default String thinkableInputReading(Direction side) { return ""; }

    /** Value delivery, null clears */
    default void receiveThinkable(Direction side, @Nullable Double value) {}

    default void onThinkablePulse(Direction side) {}

    /** note: this gets called on both server/client so make sure it does not try to call something side specific */
    @Nullable
    default Component thinkablePortName(Direction side) {
        if (acceptsThinkable(side)) return Component.translatable("thinkable.vs_clockwork.port.input");
        if (providesThinkable(side)) return Component.translatable("thinkable.vs_clockwork.port.output");
        return null;
    }
}
