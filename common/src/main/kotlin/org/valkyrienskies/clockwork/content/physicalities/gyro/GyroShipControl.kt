package org.valkyrienskies.clockwork.content.physicalities.gyro

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.joml.Vector3d
import org.joml.Vector3dc
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

    private var shipUp: Vector3dc = Vector3d(0.0,1.0,0.0)
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

        val shipWorldUp: Vector3dc = physShip.transform.shipToWorldRotation.transform(shipUp, Vector3d())
        val offAxisOmega = physShip.angularVelocity.sub(
            shipWorldUp.normalize(physShip.angularVelocity.dot(shipWorldUp), Vector3d()), Vector3d())
        val idealOmega = shipWorldUp.cross(Vector3d(0.0,1.0,0.0), Vector3d()).sub(offAxisOmega)

        val idealTorque = physShip.transform.shipToWorldRotation.transform(
            physShip.momentOfInertia.transform(
                physShip.transform.shipToWorldRotation.transformInverse(idealOmega, Vector3d())))

        idealTorque.mul(abs(speedToForce(speed)))

        physShip.applyWorldTorque(idealTorque)
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

    fun pointTowards(shipUp: Vector3dc, power: Float) {
        this.shipUp = shipUp//Quaterniond(AxisAngle4d(seatDir.normal.toJOMLD().angle(targetDirection), axis)).normalize()
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