package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat;

import java.util.Set;
import java.util.stream.Collectors;

public enum InputKey {
    FORWARD,
    BACKWARD,
    LEFT,
    RIGHT,
    JUMP;

    public static int asInt(Set<InputKey> keys) {
        return keys.stream()
                .map(InputKey::ordinal)
                .reduce(0, (a, b) -> a | (1 << b));
    }

    public static Set<InputKey> fromInt(int keys) {
        return Set.of(values()).stream()
                .filter(key -> (keys & (1 << key.ordinal())) != 0)
                .collect(Collectors.toSet());
    }
}
