package org.valkyrienskies.clockwork.kelvin.api

enum class ConnectionType {
    NONE,
    PIPE,
    ONEWAY,
    FILTERED,
    FILTERED_ONEWAY,
    APERTURE,
    APERTURE_ONEWAY,
    APERTURE_FILTERED,
    APERTURE_FILTERED_ONEWAY,
    OTHER,

    ;

    fun nextScrewdrivable(): ConnectionType {
        return when (this) {
            PIPE -> ONEWAY
            ONEWAY -> FILTERED
            FILTERED -> FILTERED_ONEWAY
            FILTERED_ONEWAY -> PIPE
            else -> PIPE
        }
    }
}