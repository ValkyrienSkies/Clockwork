package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.propulsion.singleton.fan.EncasedFanCreateData
import org.valkyrienskies.clockwork.content.propulsion.singleton.fan.EncasedFanData
import org.valkyrienskies.clockwork.content.propulsion.singleton.fan.EncasedFanUpdateData
import org.valkyrienskies.clockwork.util.AerodynamicUtils
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.setAttachment
import org.valkyrienskies.core.api.world.properties.DimensionId
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.*

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class EncasedFanController(
    override val appliers: HashMap<Int, EncasedFanData> = HashMap(),
    override val applierUpdateData: ConcurrentLinkedQueue<kotlin.Pair<Int, EncasedFanUpdateData>> = ConcurrentLinkedQueue(),
    override val createdAppliers: ConcurrentLinkedQueue<kotlin.Pair<Int, EncasedFanCreateData>> = ConcurrentLinkedQueue(),
    override val removedAppliers: ConcurrentLinkedQueue<Int> = ConcurrentLinkedQueue(),
    override var nextApplierID: Int = 0
) : MultiInstanceForceApplier<EncasedFanUpdateData, EncasedFanData, EncasedFanCreateData> {

    @JsonIgnore
    var dimensionId: DimensionId = "minecraft:overworld"

    override fun applyForces(physShip: PhysShip) {
        super.applyForces(physShip)

        for (physData in appliers.values) {
            val force = computeForce(physData, physShip)
            val fanVector: Vector3dc =
                Vector3d(physData.position).add(0.5, 0.5, 0.5, Vector3d()).sub(physShip.transform.positionInShip)
            physShip.applyRotDependentForceToPos(force, fanVector)
        }
    }

    private fun computeForce(
        physData: EncasedFanData,
        physShip: PhysShip
    ): Vector3dc {
        val speed: Double = physData.fanSpeed * 2.0 * PI / 60.0
        val bladePitch = 15.0
        val fanPosRelCenterMass: Vector3dc = physShip.transform.shipToWorld.transformPosition(
            Vector3d(physData.position).add(0.5, 0.5, 0.5, Vector3d()),
            Vector3d()
        ).sub(physShip.transform.positionInWorld, Vector3d())
        val worldVelAtFan: Vector3dc = physShip.omega.cross(fanPosRelCenterMass, Vector3d())
            .add(physShip.velocity, Vector3d())

        val airDensityAtY = AerodynamicUtils.getAirDensityForY(physShip.transform.positionInWorld.y(), dimensionId)

        val velocityTowardsPropellerDir = worldVelAtFan.dot(physShip.transform.shipToWorld.transformDirection(physData.fanDir!!, Vector3d()).normalize())

        val netForce = Vector3d(physData.fanDir).normalize()

        val effectiveVelocity = sqrt(velocityTowardsPropellerDir.pow(2.0) + (physData.fanSpeed * 0.5).pow(2.0))

        val inflowAngle = atan(velocityTowardsPropellerDir / (speed))

        val angleOfAttack = Math.toRadians(bladePitch) - inflowAngle

        val liftCoefficient = 2.0 * Math.PI * angleOfAttack
        val dragCoefficient = 0.01 * liftCoefficient

        val dLift = 0.5 * airDensityAtY * effectiveVelocity.pow(2.0) * 0.15 * 0.5 * liftCoefficient
        val dDrag = 0.5 * airDensityAtY * effectiveVelocity.pow(2.0) * 0.15 * 0.5 * dragCoefficient

        val dThrust = dLift * Math.cos(inflowAngle) - dDrag * Math.sin(inflowAngle)

        netForce.mul(dThrust * 40.0)

        return netForce
    }

    private fun setDimension(dimensionId: DimensionId) {
        this.dimensionId = dimensionId
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip): EncasedFanController? {
            if (ship.getAttachment(EncasedFanController::class.java) == null) {
                val controller = EncasedFanController()
                controller.setDimension(ship.chunkClaimDimension)
                ship.setAttachment(controller)
            }
            val attachment = ship.getAttachment(EncasedFanController::class.java)
            attachment!!.dimensionId = ship.chunkClaimDimension
            return attachment
        }
    }
}