package org.valkyrienskies.clockwork.content.forces

import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
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

        run {
            val pConst = 160.0
            val dConst = 20.0

            val localGrabPos: Vector3dc =
                physShip.transform.shipToWorld.transformPosition(dataCopy.grabbedPos, Vector3d())
            val idealPosDif: Vector3dc = dataCopy.idealPos.sub(localGrabPos, Vector3d())

            val posDif: Vector3d = idealPosDif.mul(pConst, Vector3d())
            val mass = physShip.mass

            // Integrate
            posDif.sub(physShip.velocity.mul(dConst, Vector3d()))

            val force = posDif.mul(mass * gain, Vector3d())
            physShip.applyWorldForceToBodyPos(force)
        }

        run {
            val pConst = 160.0
            val dConst = 20.0
            val rotDif =
                dataCopy.idealRot.mul(physShip.transform.shipToWorldRotation.invert(Quaterniond()), Quaterniond())
                    .normalize().invert()
            val rotDifVector = Vector3d(rotDif.x() * 2.0, rotDif.y() * 2.0, rotDif.z() * 2.0).mul(pConst)
            if (rotDif.w() < 0) {
                rotDifVector.mul(-1.0)
            }
            rotDifVector.mul(-1.0)

            // Integrate
            rotDifVector.sub(physShip.angularVelocity.mul(dConst, Vector3d()))

            val torque = physShip.transform.shipToWorldRotation.transform(
                physShip.momentOfInertia.transform(
                    physShip.transform.shipToWorldRotation.transformInverse(
                        rotDifVector,
                        Vector3d()
                    )
                )
            )
            physShip.applyWorldTorque(torque.mul(gain))
        }
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
