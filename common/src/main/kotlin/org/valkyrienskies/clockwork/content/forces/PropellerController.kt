package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import net.minecraft.util.Mth
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropCreateData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropUpdateData
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.internal.ships.VsiPhysShip
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashMap
import kotlin.math.*

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class PropellerController(
    override val appliers: HashMap<Int, PropData> = HashMap(),
    override val applierUpdateData: ConcurrentLinkedQueue<Pair<Int, PropUpdateData>> = ConcurrentLinkedQueue(),
    override val createdAppliers: ConcurrentLinkedQueue<Pair<Int, PropCreateData>> = ConcurrentLinkedQueue(),
    override val removedAppliers: ConcurrentLinkedQueue<Int> = ConcurrentLinkedQueue(),
    override var nextApplierID: Int = 0
) : MultiInstanceForceApplier<PropUpdateData, PropData, PropCreateData> {

    var dimensionId: DimensionId = "minecraft:dimension:minecraft:overworld"

    var ticksSinceLastUpdate = 0

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        if (applierUpdateData.isNotEmpty()) ticksSinceLastUpdate = 0
        super.physTick(physShip, physLevel)

        // Propeller Thrust
        for (physData in appliers.values) {
            if(physData.active) {
                val (force, torque) = if (physData.brass) computeForce(physShip.transform, physData, physShip.velocity, physShip.angularVelocity, physShip, physLevel)
                else computeBladeForce(physShip, physData, physLevel)

                if (force.isFinite && torque.isFinite) {
                    // Both models return the complete world-space force and moment about the COM.
                    physShip.applyWorldForceToBodyPos(force)
                    physShip.applyWorldTorque(torque)

                }
            }
        }

        // Propeller Pushing
        ticksSinceLastUpdate++
    }

    internal fun computeForce(
        physTransform: ShipTransform,
        physProp: PropData,
        vel: Vector3dc,
        omega: Vector3dc,
        physShip: PhysShip,
        physLevel: PhysLevel
    ): Pair<Vector3dc, Vector3dc> = computeAerodynamics(physTransform, physProp, vel, omega, physShip, physLevel)

    internal fun computeBladeForce(
        physShip: PhysShip, physProp: PropData, physLevel: PhysLevel
    ): Pair<Vector3dc, Vector3dc> = computeAerodynamics(
        physShip.transform, physProp, physShip.velocity, physShip.angularVelocity, physShip, physLevel
    )

    private fun computeAerodynamics(
        transform: ShipTransform, prop: PropData, velocity: Vector3dc, angularVelocity: Vector3dc,
        ship: PhysShip, level: PhysLevel
    ): Pair<Vector3dc, Vector3dc> {
        val flow = PropellerAerodynamics.Flow(
            transform.shipToWorld, ship.centerOfMass, velocity, angularVelocity,
            (ship as VsiPhysShip).dragController?.getWindVector() ?: Vector3d()
        ) { y -> level.aerodynamicUtils.getAirDensityForY(y, dimensionId) }
        val estimatedAngle = (prop.bearingAngle + prop.bearingSpeed / 3.0 * ticksSinceLastUpdate) % 360.0
        return PropellerAerodynamics.compute(
            prop, flow, estimatedAngle, ClockworkConfig.SERVER.forceMulPerSailInPropeller,
            ClockworkConfig.SERVER.propellerMaxForce, ClockworkConfig.SERVER.propellerMaxTorque
        )
    }

    // Create's bearing speed is degrees per 20 Hz game tick; physics velocities are SI units.
    internal fun bearingSpeedToRadiansPerSecond(degreesPerTick: Double): Double =
        PropellerAerodynamics.bearingSpeedToRadiansPerSecond(degreesPerTick)

    private fun setDimension(dimID: DimensionId) {
        dimensionId = dimID
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip): PropellerController? {
            if (ship.getAttachment(PropellerController::class.java) == null) {
                val controller = PropellerController()
                controller.setDimension(ship.chunkClaimDimension)
                ship.setAttachment(controller)
            }
            return ship.getAttachment(PropellerController::class.java)
        }

        fun calculateBladePower(velocityTowardsPropellerDir: Double,
                                bladeRotationalSpeed: Double, bladeLength: Double, bladeAngle: Double, bladeWidth: Double): Double {
            // Magic balancing constants
            val b = 1.0
            val c = 4.0

            // Transform blade angle to usable range for powerCoefficient
            val a = ln(Mth.clamp(abs(bladeAngle), 0.0, 90.0) + 1)/2.3
            if (a == 0.0) return 0.0

            // TODO: Calculate speed of sound
            val airspeed = Mth.clamp(velocityTowardsPropellerDir, 0.0, 331.0)
            val advanceRatio = airspeed / (bladeRotationalSpeed * bladeLength)
            val powerCoefficient =  b * (a - 1 - advanceRatio/(a.pow(3.0) * c.pow(2.0)))
            val machPowerMultiplier = 1 - 1.0/(1+ exp((331 - airspeed) / 30.0))
            val power = powerCoefficient *
                    bladeRotationalSpeed.pow(3) *
                    (2 * bladeLength).pow(5) *
                    bladeWidth * 4 *
                    machPowerMultiplier

            return max(power, 0.0)
        }
    }
}
