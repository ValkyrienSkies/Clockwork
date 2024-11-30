package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.joml.Matrix3d
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierCreateData
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierData
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierUpdateData
import org.valkyrienskies.clockwork.content.physicalities.reactionwheel.data.ReactionWheelCreateData
import org.valkyrienskies.clockwork.content.physicalities.reactionwheel.data.ReactionWheelData
import org.valkyrienskies.clockwork.content.physicalities.reactionwheel.data.ReactionWheelUpdateData
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.absoluteValue
import kotlin.math.sign

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class ReactionWheelController(
    override val appliers: HashMap<Int, ReactionWheelData> = HashMap(),
    override val applierUpdateData: ConcurrentLinkedQueue<Pair<Int, ReactionWheelUpdateData>> = ConcurrentLinkedQueue(),
    override val createdAppliers: ConcurrentLinkedQueue<Pair<Int, ReactionWheelCreateData>> = ConcurrentLinkedQueue(),
    override val removedAppliers: ConcurrentLinkedQueue<Int> = ConcurrentLinkedQueue(),
    override var nextApplierID: Int = 0
) : MultiInstanceForceApplier<ReactionWheelUpdateData, ReactionWheelData, ReactionWheelCreateData> {

    private var previousTickShipOmega: Vector3dc = Vector3d()

    val pendingMomentumConsumptionQueue = HashMap<Int, ConcurrentLinkedQueue<Double>>()

    override fun applyForces(physShip: PhysShip, timeStep: Double) {
        super.applyForces(physShip)

        val (torque, deltaWheelOmegas) = conserveMomentum(physShip as PhysShipImpl)
        if (torque.isFinite && !torque.length().isNaN()) {
            physShip.applyInvariantTorque(torque)
        }

        for (wheelID in deltaWheelOmegas.keys) {
            if (appliers.containsKey(wheelID)) {
                pendingMomentumConsumptionQueue[wheelID]!!.add(deltaWheelOmegas[wheelID]!!)
            }
        }

        previousTickShipOmega = physShip.omega
    }

    fun conserveMomentum(physShip: PhysShipImpl): Pair<Vector3d, Map<Int, Double>> {

        val axesToCheck = listOf(Vector3d(1.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0), Vector3d(0.0, 0.0, 1.0))

        for (axis in axesToCheck) {
            val realAxis: Vector3dc = physShip.transform.worldToShip.transformDirection(axis, Vector3d()).normalize()
            var momentumConserved = false
            val wheelsOnAxis = appliers.values.filter { Vector3d(it.direction) == axis }
            if (wheelsOnAxis.isEmpty()) {
                continue
            }
            val deltaShipOmega = physShip.omega.sub(previousTickShipOmega, Vector3d())
            val shipDeltaL = realAxis.mul(deltaShipOmega.dot(realAxis), Vector3d()).mul(physShip.momentOfInertia, Vector3d())
            val totalDeltaWheelOmega = realAxis.mul(wheelsOnAxis.sumOf { (it.currentRPM * 2.0 * Math.PI / 60.0) - (it.previousRPM * 2.0 * Math.PI / 60.0) }, Vector3d())
            val totalDeltaWheelL = totalDeltaWheelOmega.mul(physShip.momentOfInertia.scale(ClockworkConfig.SERVER.reactionWheelEffectiveness * wheelsOnAxis.size.toDouble(), Matrix3d()), Vector3d())

            momentumConserved = totalDeltaWheelL.add(shipDeltaL, Vector3d()).length().absoluteValue <= 0.001

            while (!momentumConserved) {
                for (wheel in appliers.values.filter { Vector3d(it.direction) == axis }) {

                    val wheelRPM = wheel.currentRPM

                    val wheelOmega = wheelRPM * 2.0 * Math.PI / 60.0
                    val previousWheelOmega = wheel.previousRPM * 2.0 * Math.PI / 60.0

                    val requiredChangeInWheelOmega = (shipDeltaL.mul(physShip.momentOfInertia.scale(ClockworkConfig.SERVER.reactionWheelEffectiveness, Matrix3d()), Vector3d()))
                }
            }
        }
    }

    companion object {
        fun getOrCreate(ship: ServerShip): ReactionWheelController? {
            if (ship.getAttachment(ReactionWheelController::class.java) == null) {
                ship.saveAttachment(ReactionWheelController::class.java, ReactionWheelController())
            }
            return ship.getAttachment(ReactionWheelController::class.java)
        }
    }
}