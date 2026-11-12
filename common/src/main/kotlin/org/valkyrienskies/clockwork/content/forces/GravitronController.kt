package org.valkyrienskies.clockwork.content.forces

import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronForceInducerData
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronManipulation
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import org.valkyrienskies.clockwork.util.ConnectedShipMass
import java.util.concurrent.ConcurrentHashMap

class GravitronController : ShipPhysicsListener {
    @Volatile
    var data: GravitronForceInducerData? = null

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        val dataCopy = data ?: return
        val connected = if (dataCopy.connectedMass && physLevel is VsiPhysLevel)
            ConnectedShipMass(physLevel.getAllJoints().values, elastic = { true }) { id ->
                physLevel.getShipById(id)?.let { ConnectedShipMass.Load(it.mass, it.isStatic) }
                    ?: ConnectedShipMass.Load(0.0, true)
            } else null
        val gain = GravitronManipulation.supportedGain(physShip.mass, connected?.total(physShip.id)?.mass ?: physShip.mass,
            connected?.rigidMass(physShip.id) ?: physShip.mass,
            timeSteps[physLevel.dimension] ?: (1.0 / 60.0))

        if (physShip.isStatic || !physShip.mass.isFinite() || physShip.mass <= 0.0) return
        val step = timeSteps[physLevel.dimension] ?: (1.0 / 60.0)
        val grabWorld = physShip.transform.shipToWorld.transformPosition(dataCopy.grabbedPos, Vector3d())
        val grabOffset = Vector3d(grabWorld).sub(physShip.transform.positionInWorld)
        val velocity = GravitronManipulation.pointVelocity(physShip.velocity, physShip.angularVelocity, grabOffset)
        val error = dataCopy.idealPos.sub(grabWorld, Vector3d())
        val acceleration = GravitronManipulation.acceleration(error, velocity, step, 120.0)
        val force = acceleration.mul(physShip.mass * gain)
        if (force.isFinite) physShip.applyWorldForceToBodyPos(force)

        val rotationError = dataCopy.idealRot.mul(physShip.transform.shipToWorldRotation.invert(Quaterniond()), Quaterniond())
        if (!rotationError.isFinite || rotationError.lengthSquared() < 1e-12) return
        rotationError.normalize()
        val sign = if (rotationError.w < 0.0) -2.0 else 2.0
        val angularError = Vector3d(rotationError.x, rotationError.y, rotationError.z).mul(sign)
        val angularAcceleration = GravitronManipulation.acceleration(angularError, physShip.angularVelocity, step, 30.0)
        val localAcceleration = physShip.transform.shipToWorldRotation.transformInverse(angularAcceleration, Vector3d())
        val torque = physShip.transform.shipToWorldRotation.transform(physShip.momentOfInertia.transform(localAcceleration))
            .mul(gain)
        if (torque.isFinite) physShip.applyWorldTorque(torque)
    }

    companion object {
        private val timeSteps = ConcurrentHashMap<String, Double>()
        fun timeStep(level: PhysLevel, delta: Double) {
            if (delta.isFinite() && delta > 0.0) timeSteps[level.dimension] = delta
        }
        fun getOrCreate(ship: LoadedServerShip): GravitronController {
            return ship.getAttachment<GravitronController>()
                ?: GravitronController().also { ship.setAttachment(it) }
        }
    }
}
