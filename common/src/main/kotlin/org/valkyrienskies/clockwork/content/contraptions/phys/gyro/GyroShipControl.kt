package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.joml.Quaterniond
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
class GyroShipControl : ShipForcesInducer, ServerTickListener {

    private var targetRotation = Quaterniond()
    private var targetStrength = 1.0f
    private var physConsumption = 0f
    private var extraForceLinear = 0.0
    private var extraForceAngular = 0.0
    var powerLinear = 0.0
    var powerAngular = 0.0
    var gyros = 0
        set(v) {
            field = v; deleteIfEmpty()
        }
    var consumed = 0f
        private set

    @JsonIgnore
    internal var ship: ServerShip? = null

    internal var speed: Float = 0f

    override fun applyForces(physShip: PhysShip) {
        if (gyros < 1) {
            return
        }

        physShip as PhysShipImpl

        val rotDif = targetRotation
            .mul(physShip.transform.shipToWorldRotation.invert(Quaterniond()), Quaterniond())
            .normalize().invert()

        // Blackmagic ask triode
        val idealOmega = Vector3d(rotDif.x() * 2.0, rotDif.y() * 2.0, rotDif.z() * 2.0)
        if (rotDif.w() > 0) idealOmega.mul(-1.0)

        idealOmega.sub(physShip.poseVel.omega)

        val idealTorque = physShip.poseVel.rot.transform(
            physShip.inertia.momentOfInertiaTensor.transform(
                physShip.poseVel.rot.transformInverse(idealOmega, Vector3d())))

        idealTorque.mul(100.0)

        physShip.applyInvariantTorque(idealTorque)
    }

    private fun deleteIfEmpty() {
        if (gyros <= 0) {
            ship?.saveAttachment<GyroShipControl>(null)
        }
    }

    fun pointTowards(targetRotation: Quaterniond, power: Float) {
        //val axis = seatDir.normal.toJOMLD().cross(targetDirection, Vector3d())
        this.targetRotation = targetRotation//Quaterniond(AxisAngle4d(seatDir.normal.toJOMLD().angle(targetDirection), axis)).normalize()
        this.targetStrength = power
    }

    override fun onServerTick() {
        extraForceLinear = powerLinear
        powerLinear = 0.0

        extraForceAngular = powerAngular
        powerAngular = 0.0;

        consumed = physConsumption * /* should be physics ticks based*/ 0.1f
        physConsumption = 0.0f
    }

    companion object {
        fun getOrCreate(ship: ServerShip): GyroShipControl {
            return ship.getAttachment<GyroShipControl>()
                ?: GyroShipControl().also {
                    ship.saveAttachment(it)
                }
        }
    }

}