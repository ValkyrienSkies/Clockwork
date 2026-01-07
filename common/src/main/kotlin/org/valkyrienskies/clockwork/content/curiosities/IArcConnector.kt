package org.valkyrienskies.clockwork.content.curiosities

import net.minecraft.world.phys.Vec3
import org.valkyrienskies.mod.common.toWorldCoordinates

interface IArcConnector {

    /**
     * Used on the server and client. Unused for the debug arcer since it has no function and is entirely visual.
     */
    fun getConnectedArcs(): MutableList<IArcConnector> {
        return ArrayList()
    }

    fun getMaxConnections(): Int

    fun getMaxRange(): Double {
        return 16.0
    }

    fun canConnect(other: IArcConnector): Boolean {
        return (getConnectedArcs().contains(other) || getConnectedArcs().size < getMaxConnections()) && other.getWorldPos().distanceTo(getWorldPos()) <= getMaxRange()
    }

    fun getWorldPos(): Vec3
}
