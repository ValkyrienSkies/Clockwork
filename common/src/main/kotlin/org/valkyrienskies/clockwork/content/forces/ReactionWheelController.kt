package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.physicalities.reactionwheel.data.ReactionWheelCreateData
import org.valkyrienskies.clockwork.content.physicalities.reactionwheel.data.ReactionWheelData
import org.valkyrienskies.clockwork.content.physicalities.reactionwheel.data.ReactionWheelUpdateData
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import java.util.concurrent.ConcurrentLinkedQueue

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class ReactionWheelController(
    override val appliers: HashMap<Int, ReactionWheelData> = HashMap(),
    override val applierUpdateData: ConcurrentLinkedQueue<Pair<Int, ReactionWheelUpdateData>> = ConcurrentLinkedQueue(),
    override val createdAppliers: ConcurrentLinkedQueue<Pair<Int, ReactionWheelCreateData>> = ConcurrentLinkedQueue(),
    override val removedAppliers: ConcurrentLinkedQueue<Int> = ConcurrentLinkedQueue(),
    override var nextApplierID: Int = 0
) : MultiInstanceForceApplier<ReactionWheelUpdateData, ReactionWheelData, ReactionWheelCreateData> {

    @JsonIgnore
    private var previousTickShipOmega: Vector3dc = Vector3d()

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        super.physTick(physShip, physLevel)

        for (wheelID in appliers.keys) {
            val data = appliers[wheelID]!!
            val wheelMass = ClockworkConfig.SERVER.reactionWheelEffectiveness * 64.0
            physShip.applyWorldTorque(
                physShip.transform.shipToWorld.transformDirection(Vector3d(data.direction), Vector3d())
                    .normalize()
                    .mul(data.currentRPM * wheelMass))
        }

        previousTickShipOmega = physShip.omega
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip): ReactionWheelController? {
            if (ship.getAttachment(ReactionWheelController::class.java) == null) {
                ship.setAttachment(ReactionWheelController())
            }
            return ship.getAttachment(ReactionWheelController::class.java)
        }
    }
}
