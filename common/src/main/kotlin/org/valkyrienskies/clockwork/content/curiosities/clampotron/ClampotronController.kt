package org.valkyrienskies.clockwork.content.curiosities.clampotron

import com.fasterxml.jackson.annotation.JsonIgnore
import org.joml.Matrix3d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronManipulation
import org.valkyrienskies.clockwork.content.forces.GravitronController
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import org.valkyrienskies.clockwork.util.ConnectedShipMass

class ClampotronController : ShipPhysicsListener {
    data class Grip(val owner: Long, val carrier: Long, val anchor: Vector3d, val target: Vector3d,
                    val rotation: Quaterniond, val maximumForce: Double, val refreshed: Long = System.nanoTime())

    @get:JsonIgnore @Volatile var grip: Grip? = null
    @get:JsonIgnore @Volatile var strain = 0f
    @get:JsonIgnore @Volatile var error = 0.0

    fun active() = grip?.let { System.nanoTime() - it.refreshed < 1_000_000_000L } == true

    override fun physTick(ship: PhysShip, level: PhysLevel) {
        val command = grip ?: return
        if (!active() || ship.isStatic || !ship.mass.isFinite() || ship.mass <= 0.0 ||
            !command.maximumForce.isFinite() || command.maximumForce <= 0.0) return
        val carrier = if (command.carrier >= 0) level.getShipById(command.carrier) ?: return else null
        val step = GravitronController.timeStep(level)
        val target = Vector3d(command.target)
        val rotation = Quaterniond(command.rotation)
        if (carrier != null) {
            carrier.transform.shipToWorld.transformPosition(target)
            carrier.transform.shipToWorldRotation.mul(rotation, rotation)
        }
        val anchor = ship.transform.shipToWorld.transformPosition(command.anchor, Vector3d())
        val offset = Vector3d(anchor).sub(ship.transform.positionInWorld)
        val carrierOffset = carrier?.let { Vector3d(anchor).sub(it.transform.positionInWorld) } ?: Vector3d()
        val velocity = GravitronManipulation.pointVelocity(ship.velocity, ship.angularVelocity, offset)
        if (carrier != null) velocity.sub(GravitronManipulation.pointVelocity(carrier.velocity,
            carrier.angularVelocity, carrierOffset))
        val displacement = Vector3d(target).sub(anchor)
        error = displacement.length()
        val difference = rotation.mul(ship.transform.shipToWorldRotation.invert(Quaterniond())).normalize()
        val sign = if (difference.w < 0) -2.0 else 2.0
        val angularError = Vector3d(difference.x, difference.y, difference.z).mul(sign)
        val angularVelocity = Vector3d(ship.angularVelocity)
        if (carrier != null) angularVelocity.sub(carrier.angularVelocity)
        val graph = (level as? VsiPhysLevel)?.let { physics ->
            ConnectedShipMass(physics.getAllJoints().values, elastic = { true }) { id ->
                level.getShipById(id)?.let { ConnectedShipMass.Load(it.mass, it.isStatic) }
            }
        }
        val loads = graph?.endpoints(ship.id, carrier?.id)
        val loadMass = (loads?.first?.mass ?: ship.mass).coerceAtLeast(ship.mass)
        val carrierMass = if (carrier == null || carrier.isStatic) 0.0 else
            (loads?.second?.mass ?: carrier.mass).coerceAtLeast(carrier.mass)
        val effectiveMass = if (carrierMass > 0.0) 1.0 / (1.0 / loadMass + 1.0 / carrierMass) else loadMass
        val gravity = if (carrier == null || carrier.isStatic)
            level.aerodynamicUtils.getAtmosphereForDimension(level.dimension).third else 0.0
        val wrench = ClampotronPhysics.solve(body(ship, offset), body(carrier, carrierOffset), displacement, velocity,
            angularError, angularVelocity, effectiveMass, gravity, step)
        val force = wrench.force
        val torque = wrench.torque
        strain = maxOf(force.length() / command.maximumForce, torque.length() / (command.maximumForce * 2), error / 4.0).toFloat()
        val scale = minOf(1.0, command.maximumForce / force.length().coerceAtLeast(1e-12),
            command.maximumForce * 2 / torque.length().coerceAtLeast(1e-12))
        force.mul(scale)
        torque.mul(scale)
        ship.applyWorldForceToBodyPos(force)
        ship.applyWorldTorque(Vector3d(offset).cross(force).add(torque))
        if (carrier != null && !carrier.isStatic) {
            carrier.applyWorldForceToBodyPos(Vector3d(force).negate())
            carrier.applyWorldTorque(Vector3d(carrierOffset).cross(force).add(torque).negate())
        }
    }

    private fun body(ship: PhysShip?, offset: Vector3d): ClampotronPhysics.Body {
        if (ship == null || ship.isStatic) return ClampotronPhysics.Body(0.0, Matrix3d().zero(), offset)
        val rotation = Matrix3d().rotation(ship.transform.shipToWorldRotation)
        val inverse = Matrix3d(rotation).mul(Matrix3d(ship.momentOfInertia).invert()).mul(rotation.transpose())
        return ClampotronPhysics.Body(1.0 / ship.mass, inverse, offset)
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip): ClampotronController = ship.getAttachment<ClampotronController>()
            ?: ClampotronController().also { ship.setAttachment(it) }
    }
}
