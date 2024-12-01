package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.util.Mth
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
import kotlin.math.min
import kotlin.math.sign

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

    override fun applyForces(physShip: PhysShip) {
        super.applyForces(physShip)

        for (wheelID in appliers.keys) {
            val (torque, deltaWheelOmega) = conserveMomentum(physShip as PhysShipImpl, appliers[wheelID]!!)
            if (torque.isFinite && !torque.length().isNaN()) {
                physShip.applyInvariantTorque(torque)
                pendingMomentumConsumptionQueue[wheelID]?.add(deltaWheelOmega) ?: run {
                    pendingMomentumConsumptionQueue[wheelID] = ConcurrentLinkedQueue()
                    pendingMomentumConsumptionQueue[wheelID]?.add(deltaWheelOmega)
                }
            }
        }

        previousTickShipOmega = physShip.omega
    }

    fun conserveMomentum(physShip: PhysShipImpl, wheel: ReactionWheelData): Pair<Vector3d, Double> {
        val wheelPos: Vector3dc = Vector3d(wheel.position!!)
        val wheelDir: Vector3dc = Vector3d(wheel.direction!!)
        val realAxis: Vector3dc = physShip.transform.shipToWorld.transformDirection(wheelDir, Vector3d()).normalize()

        val wheelSpeed = wheel.currentRPM / 60.0 * 2.0 * Math.PI
        val wheelPrevious = wheel.previousRPM / 60.0 * 2.0 * Math.PI

        val wheelMass = ClockworkConfig.SERVER.reactionWheelEffectiveness * 32000.0
        val wheelInertia = (0.5 * wheelMass) * (Math.pow(0.25, 2.0) + Math.pow(0.75, 2.0))

        val wheelOmega = realAxis.mul(wheelSpeed, Vector3d())
        val wheelPreviousOmega = realAxis.mul(wheelPrevious, Vector3d())

        val deltaWheelOmega = wheelOmega.sub(wheelPreviousOmega, Vector3d())

        val wheelL: Vector3dc = wheelOmega.mul(wheelInertia)
        val wheelPreviousL: Vector3dc = wheelPreviousOmega.mul(wheelInertia)

        val shipL: Vector3dc = realAxis.mul(physShip.omega.dot(realAxis), Vector3d()).mul(physShip.momentOfInertia)
        val previousShipL: Vector3dc = realAxis.mul(previousTickShipOmega.dot(realAxis), Vector3d()).mul(physShip.momentOfInertia)

        val deltaShipL = shipL.sub(previousShipL, Vector3d())

        val requiredChangeInWheel = deltaShipL.div(wheelInertia, Vector3d()).mul(-1.0, Vector3d()).length()

        val actualChangeInWheel = Mth.clamp(Mth.clamp(requiredChangeInWheel, -(wheelL.length().absoluteValue / wheelInertia), (wheelL.length().absoluteValue / wheelInertia)), -512 + wheelSpeed, 512 - wheelSpeed)

        val leftoverShipL: Vector3dc = shipL.add(realAxis.mul((actualChangeInWheel + deltaWheelOmega.length()) * wheelInertia, Vector3d()), Vector3d())

        val torque = leftoverShipL.mul(-1.0, Vector3d())

        return torque to actualChangeInWheel
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