package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.core.api.ships.LoadedServerShip

/** Cheap, read-only design readout. Called at most four times per second on an observed ship. */
interface AeronautForceSource {
    fun addPotentialForces(level: ServerLevel, ship: LoadedServerShip, thrust: AeronautForceAccumulator)
}
