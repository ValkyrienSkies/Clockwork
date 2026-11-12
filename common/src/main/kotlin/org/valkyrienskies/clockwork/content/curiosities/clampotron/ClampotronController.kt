package org.valkyrienskies.clockwork.content.curiosities.clampotron

import com.fasterxml.jackson.annotation.JsonIgnore
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronManipulation
import org.valkyrienskies.clockwork.content.forces.GravitronController
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.world.PhysLevel

class ClampotronController : ShipPhysicsListener {
    data class Grip(val owner: Long, val carrier: Long, val anchor: Vector3d, val target: Vector3d,
                    val rotation: Quaterniond, val maximumForce: Double, val refreshed: Long = System.nanoTime())

    @get:JsonIgnore @Volatile var grip: Grip? = null
    @get:JsonIgnore @Volatile var strain = 0f
    @get:JsonIgnore @Volatile var error = 0.0

    fun active() = grip?.let { System.nanoTime() - it.refreshed < 1_000_000_000L } == true

    override fun physTick(ship: PhysShip, level: PhysLevel) {
        val command = grip ?: return
        if (!active() || ship.isStatic || !ship.mass.isFinite() || ship.mass <= 0.0) return
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
        val velocity = GravitronManipulation.pointVelocity(ship.velocity, ship.angularVelocity, offset)
        if (carrier != null) velocity.sub(GravitronManipulation.pointVelocity(carrier.velocity,
            carrier.angularVelocity, Vector3d(target).sub(carrier.transform.positionInWorld)))
        val displacement = Vector3d(target).sub(anchor)
        error = displacement.length()
        val force = GravitronManipulation.acceleration(displacement, velocity, step, 160.0)
            .add(0.0, level.aerodynamicUtils.getAtmosphereForDimension(level.dimension).third, 0.0).mul(ship.mass)
        val difference = rotation.mul(ship.transform.shipToWorldRotation.invert(Quaterniond())).normalize()
        val sign = if (difference.w < 0) -2.0 else 2.0
        val angularError = Vector3d(difference.x, difference.y, difference.z).mul(sign)
        val angularVelocity = Vector3d(ship.angularVelocity)
        if (carrier != null) angularVelocity.sub(carrier.angularVelocity)
        val acceleration = GravitronManipulation.acceleration(angularError, angularVelocity, step, 40.0)
        val torque = ship.transform.shipToWorldRotation.transform(ship.momentOfInertia.transform(
            ship.transform.shipToWorldRotation.transformInverse(acceleration, Vector3d())))
        if (!force.isFinite || !torque.isFinite) return
        strain = maxOf(force.length() / command.maximumForce, torque.length() / (command.maximumForce * 2), error / 4.0).toFloat()
        limit(force, command.maximumForce)
        limit(torque, command.maximumForce * 2)
        ship.applyWorldForceToBodyPos(force)
        ship.applyWorldTorque(torque)
        if (carrier != null && !carrier.isStatic) {
            carrier.applyWorldForceToBodyPos(Vector3d(force).negate())
            val moment = Vector3d(ship.transform.positionInWorld).sub(carrier.transform.positionInWorld).cross(force)
            carrier.applyWorldTorque(moment.add(torque).negate())
        }
    }

    companion object {
        fun limit(vector: Vector3d, maximum: Double) {
            if (vector.lengthSquared() > maximum * maximum) vector.normalize(maximum)
        }
        fun getOrCreate(ship: LoadedServerShip): ClampotronController = ship.getAttachment<ClampotronController>()
            ?: ClampotronController().also { ship.setAttachment(it) }
    }
}
