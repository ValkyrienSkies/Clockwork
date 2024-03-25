package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
class WanderShipControl : ShipForcesInducer {

    @JsonIgnore
    internal var ship: ServerShip? = null

    var aurics = 0
        set(v) {
            field = v; deleteIfEmpty()
        }

    override fun applyForces(physShip: PhysShip) {
        if (aurics < 1) {
            ship?.saveAttachment<WanderShipControl>(null)
            return
        }

        physShip as PhysShipImpl

        val yForce = (aurics * 1.0)
        val force =  Vector3d(0.0, yForce,0.0).mul(1100.0, Vector3d())

        physShip.applyInvariantForce(force)
    }

    fun deleteIfEmpty() {
        if (aurics <= 0) {
            ship?.saveAttachment<WanderShipControl>(null)
        }
    }

    companion object {

        fun getOrCreate(ship: ServerShip): WanderShipControl? {
            if (ship.getAttachment(WanderShipControl::class.java) == null) {
                ship.saveAttachment(WanderShipControl::class.java, WanderShipControl())
            }
            return ship.getAttachment(WanderShipControl::class.java)
        }
    }
}