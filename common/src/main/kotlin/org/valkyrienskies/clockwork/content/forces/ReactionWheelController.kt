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
    @JsonIgnore
    val pendingMomentumConsumptionQueue = HashMap<Int, ConcurrentLinkedQueue<Double>>()

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        super.physTick(physShip, physLevel)

        for (wheelID in appliers.keys) {
            val (torque, torque2) = conserveMomentum(physShip as PhysShipImpl, appliers[wheelID]!!)
            if (torque.isFinite && !torque.length().isNaN()) {
                physShip.applyWorldTorque(torque)
                physShip.applyWorldTorque(torque2)
            }
        }

        previousTickShipOmega = physShip.omega
    }

    fun conserveMomentum(physShip: PhysShipImpl, wheel: ReactionWheelData): Pair<Vector3dc, Vector3dc> {
        val wheelPos: Vector3dc = Vector3d(wheel.position!!)
        val wheelDir: Vector3dc = Vector3d(wheel.direction!!)
        val wheelAxisInWorld: Vector3dc = physShip.transform.shipToWorld.transformDirection(wheelDir, Vector3d()).normalize()

        val wheelSpeed = wheel.currentRPM / 60.0 * 2.0 * Math.PI
        val wheelPrevious = wheel.previousRPM / 60.0 * 2.0 * Math.PI

        val wheelMass = ClockworkConfig.SERVER.reactionWheelEffectiveness * 1_920_000.0.coerceAtMost(9600 * physShip.mass)
        // 1/2 * m * (r_2^2 + r_1^2)
        val wheelInertia = (0.5 * wheelMass) * (Math.pow(0.25, 2.0) + Math.pow(0.75, 2.0))
        // 1/12 * m * (3 * (r_2^2 + r_1^2) + h^2)
        val offAxisInertia = (1/12.0 * wheelMass) * (3*(Math.pow(0.25, 2.0) + Math.pow(0.75, 2.0)) + Math.pow(0.5, 2.0))

        val newW: Vector3dc = wheelAxisInWorld.mul(wheelSpeed, Vector3d())
        val deltaW: Vector3dc = newW.normalize(Vector3d()).cross(physShip.angularVelocity.div( 60.0, Vector3d()), Vector3d())

        val axialTorque = wheelAxisInWorld.mul((wheelSpeed - wheelPrevious) * wheelInertia / 20.0, Vector3d())

        val torqueInduced: Vector3dc = deltaW.mul(offAxisInertia, Vector3d())

        return torqueInduced to axialTorque
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
