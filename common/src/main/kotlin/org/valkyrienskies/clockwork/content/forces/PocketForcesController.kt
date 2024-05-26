package org.valkyrienskies.clockwork.content.forces

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.AerodynamicUtils
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.core.api.ships.datastructures.AirPocket
import org.valkyrienskies.core.api.ships.datastructures.ShipConnDataAttachment
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashMap

class PocketForcesController: ShipForcesInducer {

    val pockets: HashMap<Int, AirPocket> = HashMap()

    val queuedChanges: ConcurrentLinkedQueue<HashMap<Int, AirPocket>> = ConcurrentLinkedQueue()

    private var max_height: Double = 563.0

    override fun applyForces(physShip: PhysShip) {
        val physShipImpl = physShip as PhysShipImpl

        if (queuedChanges.isNotEmpty()) {
            val newPockets = queuedChanges.poll()
            if (newPockets != null) {
                pockets.clear()
                pockets.putAll(newPockets)
                for (pocketId in newPockets.keys){
                    if (!newPockets[pocketId]!!.extraData.containsKey("kelvin/gas_masses")) {
                        val newMap = HashMap<GasType, Double>()
                        for (gas in GasType.values()) {
                            if (gas == GasType.AIR){
                                newMap[gas] = (newPockets[pocketId]!!.pocket.size.toDouble() * gas.density / 4.0)
                            } else {
                                newMap[gas] = 0.0
                            }

                        }
                        newPockets[pocketId]!!.extraData["kelvin/gas_masses"] = newMap
                    }
                }
            }
        }

        val buoyancyForces = calculateBuoyancyForce(physShip)


        for (stuff in buoyancyForces) {
            physShip.applyInvariantForceToPos(Vector3d(0.0, stuff.second, 0.0), stuff.first)
        }

    }

    fun calculateBuoyancyForce(physShip: PhysShip): HashSet<Pair<Vector3dc, Double>> {
        val physShipImpl = physShip as PhysShipImpl

        var totalBuoyantForce = 0.0



        var totalForces = HashSet<Pair<Vector3dc, Double>>()

        pockets.values.forEach {
            if (it.pocket.size > 0) {
                if (it.extraData.containsKey("kelvin/gas_masses") && it.extraData.containsKey("kelvin/temperature_dbl_mrg_avg")) {
                    val centerOfBuoyancy = Vector3d(0.0, 0.0, 0.0)
                    val gasMasses = (it.extraData["kelvin/gas_masses"] ?: HashMap<GasType, Double>()) as HashMap<GasType, Double>
                    val temperature = (it.extraData["kelvin/temperature_dbl_mrg_avg"] ?: 0.0) as Double
                    var totalInternalDensity = 0.0
                    for (gas in GasType.values()) {
                        if (gasMasses.containsKey(gas)) {
                            val gasMass = gasMasses[gas] ?: 0.0

                            val density = getDensityFromTemperature(it.pocket.size.toDouble(), gasMass, temperature, gas)
                            totalInternalDensity += density
                        }
                    }
                    val buoyantForce = it.pocket.size.toDouble() * (AerodynamicUtils.getAirDensityForY(physShip.poseVel.pos.y(), max_height) - totalInternalDensity) * 10.0
                    totalBuoyantForce += buoyantForce

                    it.pocket.keys.forEach { pocketPos ->
                        centerOfBuoyancy.add(Vector3d(pocketPos.x().toDouble() + 0.5, pocketPos.y().toDouble() + 0.5, pocketPos.z().toDouble() + 0.5))
                    }
                    centerOfBuoyancy.div(it.pocket.size.toDouble()).sub(physShip.transform.positionInShip)
                    totalForces.add(Pair(centerOfBuoyancy, buoyantForce))
                }
            }
        }

        return totalForces
    }

    private fun getDensityFromTemperature(volume: Double, mass: Double, temperature: Double, gasType: GasType): Double {
        if (volume == 0.0) return 0.0

        var density = mass / volume

        if (temperature != 0.0) {
            val molarMass = gasType.density * 22.4
            val pressure = calcPressure(mass, volume, temperature, gasType)
            density = (molarMass * pressure) / (8.31446261815324 * temperature)
        }
        return density
    }

    /**
     * Calculates pressure using the ideal gas law.
     */
    private fun calcPressure(mass: Double, volume: Double, temp: Double, gasType: GasType): Double {
        var pressure = 0.0
        val molarMass = gasType.density * 22.4
        val moles = mass / molarMass
        pressure = ((moles) * 8.31446261815324 * temp) / volume
        return pressure
    }

    fun gameTick(level: ServerLevel, ship: ServerShip) {
        val loadedShip = level.shipObjectWorld.loadedShips.getById(ship.id) ?: return
        val pocketsCopy = ship.getAttachment(ShipConnDataAttachment::class.java)?.airPockets ?: return
        if (pocketsCopy != pockets) {
            queuedChanges.add(pocketsCopy)
        }
    }

    companion object {
        fun getOrCreate(ship: ServerShip): PocketForcesController? {
            if (ship.getAttachment(PocketForcesController::class.java) == null) {
                ship.saveAttachment(PocketForcesController::class.java, PocketForcesController())
            }
            return ship.getAttachment(PocketForcesController::class.java)
        }
    }
}