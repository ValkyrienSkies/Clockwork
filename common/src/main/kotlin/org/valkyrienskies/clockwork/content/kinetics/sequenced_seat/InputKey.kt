package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat

import java.util.stream.Collectors

enum class InputKey {
    FORWARD,
    BACKWARD,
    LEFT,
    RIGHT,
    JUMP;

    companion object {
        fun asInt(keys: Set<InputKey>): Int {
            return keys.stream()
                .map { obj: InputKey -> obj.ordinal }
                .reduce(0) { a: Int, b: Int -> a or (1 shl b) }
        }

        fun fromInt(keys: Int): MutableSet<InputKey> {
            return java.util.Set.of(*values()).stream()
                .filter { key: InputKey -> keys and (1 shl key.ordinal) != 0 }
                .collect(Collectors.toSet())
        }
    }
}