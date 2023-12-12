package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.minecraft.world.phys.Vec3
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import kotlin.math.abs
import kotlin.math.log

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
class GyroShipControl : ShipForcesInducer, ServerTickListener {


    internal var targetVector: Vec3 = Vec3(0.0,1.0,0.0)
    private var physConsumption = 0f
    private var extraForceLinear = 0.0
    private var extraForceAngular = 0.0

    @JsonIgnore
    internal var ship: ServerShip? = null

    internal var speed: Float = 0f

    override fun applyForces(physShip: PhysShip) {
        if (gyros < 1) {
            return
        }

        physShip as PhysShipImpl

        val omega: Vector3dc = physShip.poseVel.omega

        ship ?: return

        val strength = calculateStrength(speed)
        gyroStabilizer(physShip, omega, physShip, strength, targetVector)
    }

    private fun calculateStrength(speed: Float): Double {
        val y = 64 * log((abs(speed) + 10) * 0.1, 20.0)
        return y.coerceIn(0.0, 128.0)
    }

    private fun deleteIfEmpty() {
        if (gyros <= 0) {
            ship?.saveAttachment<GyroShipControl>(null)
        }
    }

    override fun onServerTick() {
        extraForceLinear = powerLinear
        powerLinear = 0.0

        extraForceAngular = powerAngular
        powerAngular = 0.0;

        consumed = physConsumption * /* should be physics ticks based*/ 0.1f
        physConsumption = 0.0f
    }

    var powerLinear = 0.0
    var powerAngular = 0.0
    var gyros = 0 // Amount of helms
        set(v) {
            field = v; deleteIfEmpty()
        }
    var consumed = 0f
        private set

    companion object {
        fun getOrCreate(ship: ServerShip): GyroShipControl {
            return ship.getAttachment<GyroShipControl>()
                ?: GyroShipControl().also {
                    ship.saveAttachment(it)
                }
        }
    }

}