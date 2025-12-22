package org.valkyrienskies.clockwork.content.physicalities.gyro

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.attachment.removeAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerTickListener
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.world.PhysLevel
import kotlin.math.abs
import kotlin.math.exp

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
class GyroShipControl : ShipPhysicsListener, ServerTickListener {

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
    internal var ship: LoadedServerShip? = null

    internal var speed: Float = 0f

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        if (gyros < 1) {
            return
        }

        val rotDif = targetRotation
            .mul(physShip.transform.shipToWorldRotation.invert(Quaterniond()), Quaterniond())
            .normalize().invert()

        // Blackmagic ask triode
        val idealOmega = Vector3d(rotDif.x() * 2.0, rotDif.y() * 2.0, rotDif.z() * 2.0)
        if (rotDif.w() > 0) idealOmega.mul(-1.0)

        idealOmega.sub(physShip.omega)

        val idealTorque = physShip.transform.shipToWorldRotation.transform(
            physShip.momentOfInertia.transform(
                physShip.transform.shipToWorldRotation.transformInverse(idealOmega, Vector3d())))

        idealTorque.mul(abs(speedToForce(speed)))

        physShip.applyInvariantTorque(idealTorque)
    }

    private fun speedToForce(speed: Float): Double {
        val y = 128.0 / (1 + exp(6 - (speed * 0.05)))
        return y.coerceIn(0.0, 100.0)
    }

    private fun deleteIfEmpty() {
        if (gyros <= 0) {
            ship?.removeAttachment<GyroShipControl>()
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
        fun getOrCreate(ship: LoadedServerShip): GyroShipControl {
            return ship.getAttachment<GyroShipControl>()
                ?: GyroShipControl().also {
                    ship.setAttachment(it)
                }
        }
    }

}