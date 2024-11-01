package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.ClockworkAugmentations
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.utilities.PocketForcesQueueable
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.AerodynamicUtils
import org.valkyrienskies.clockwork.util.AerodynamicUtils.GRAVITATIONAL_ACCELERATION
import org.valkyrienskies.clockwork.util.AerodynamicUtils.UNIVERSAL_GAS_CONSTANT
import org.valkyrienskies.clockwork.util.AerodynamicUtils.getDensityFromTemperature
import org.valkyrienskies.clockwork.util.ClockworkUtils.getAirComponentsInChunkClaim
import org.valkyrienskies.clockwork.util.ClockworkUtils.retrieveGasInfoFromPocket
import org.valkyrienskies.clockwork.util.MathFunctions.chunkPos
import org.valkyrienskies.clockwork.util.MathFunctions.toTriple
import org.valkyrienskies.clockwork.util.MathFunctions.toVector3i
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashMap

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class PocketForcesController: ShipForcesInducer {
    @JsonIgnore
    private var max_height: Double = 563.0
    @JsonIgnore
    val gametickKnownPocketRoots: HashSet<Vector3ic> = HashSet()
    @JsonIgnore
    val pocketRoots: HashMap<Vector3ic, Long> = HashMap()
    @JsonIgnore
    val pocketCenters: HashMap<Vector3ic, Vector3dc> = HashMap()
    @JsonIgnore
    val gasMasses: HashMap<Vector3ic, EnumMap<GasType, Double>> = HashMap()
    @JsonIgnore
    val temperatures: HashMap<Vector3ic, Double> = HashMap()
    @JsonIgnore
    private val pocketQueue: ConcurrentLinkedQueue<PocketForcesQueueable> = ConcurrentLinkedQueue()
    @JsonIgnore
    private val pocketRemoveQueue: ConcurrentLinkedQueue<Vector3ic> = ConcurrentLinkedQueue()

    @JsonIgnore
    private val pocketsOnRemoval = HashMap<Vector3ic, Int>()

    // Todo: Implement serialization

    override fun applyForces(physShip: PhysShip) {
        val physShipImpl = physShip

        while (pocketQueue.isNotEmpty()) {
            processPocketQueue()
        }

        while (pocketRemoveQueue.isNotEmpty()) {
            val root = pocketRemoveQueue.poll()
            pocketRoots.remove(root)
            pocketCenters.remove(root)
            gasMasses.remove(root)
            temperatures.remove(root)
        }

        val buoyancyForce = calculateBuoyancyForce(physShip)

        //ClockworkMod.LOGGER.info(physShip.mass.toString())

        buoyancyForce.forEach {
            physShipImpl.applyInvariantForceToPos(Vector3d(0.0, it.value, 0.0), Vector3d(pocketCenters[it.key]!!).sub(physShip.transform.positionInShip))
        }
    }

    private fun processPocketQueue() {
        val pocket = pocketQueue.poll()
        pocketRoots[pocket.rootPos] = pocket.pocketSize
        pocketCenters[pocket.rootPos] = pocket.pocketCenter
        gasMasses[pocket.rootPos] = pocket.gasMasses
        temperatures[pocket.rootPos] = pocket.temperature
    }

    fun calculateBuoyancyForce(physShip: PhysShip): Map<Vector3ic, Double> {
        val physShipImpl = physShip

        var totalBuoyantForce = HashMap<Vector3ic, Double>()

        pocketRoots.keys.forEach {
            if (pocketRoots[it]!! > 0) {
                if (gasMasses.containsKey(it) && temperatures.containsKey(it)) {
                    val gasMassesLocal = gasMasses[it]!!
                    val temperature = temperatures[it]!!
                    var totalInternalDensity = 0.0
                    for (gas in GasType.entries) {
                        if (gasMassesLocal.containsKey(gas)) {
                            val gasMass = gasMassesLocal[gas] ?: 0.0
                            if (gasMass == 0.0) continue

                            val density = getDensityFromTemperature(pocketRoots[it]!!.toDouble(), gasMass, temperature, gas)
                            totalInternalDensity += density
                        }
                    }
                    if (totalInternalDensity != 0.0) {
                        //ClockworkMod.LOGGER.info("Density at Y: " + AerodynamicUtils.getAirDensityForY(physShip.transform.positionInWorld.y(), max_height).toString())
                        //ClockworkMod.LOGGER.info("Internal Density: $totalInternalDensity")
                        val buoyantForce = (pocketRoots[it]!!.toDouble() * (AerodynamicUtils.getAirDensityForY(physShip.transform.positionInWorld.y(), max_height) - totalInternalDensity) * GRAVITATIONAL_ACCELERATION) * ClockworkConfig.SERVER.balloonForceMult
                        totalBuoyantForce[it] = buoyantForce
                    }
                }
            }
        }

        return totalBuoyantForce
    }

    fun gameTick(level: ServerLevel, ship: ServerShip) {
        val loadedShip = level.shipObjectWorld.loadedShips.getById(ship.id) ?: return

        val roots = getAirComponentsInChunkClaim(loadedShip.chunkClaim, level, ClockworkAugmentations.getComponentAugmentation("temperature"))

        for (root: Vector3i in roots.keys) {
            val gasMap = retrieveGasInfoFromPocket(root, level)

            val componentSize = roots[root] ?: continue

            val collectX = level.shipObjectWorld.collectAirAugmentation(level.shipObjectWorld.createDoubleSumAugmentation("core", "x"), root.x(), root.y(), root.z(), level.dimensionId)
            val collectY = level.shipObjectWorld.collectAirAugmentation(level.shipObjectWorld.createDoubleSumAugmentation("core", "y"), root.x(), root.y(), root.z(), level.dimensionId)
            val collectZ = level.shipObjectWorld.collectAirAugmentation(level.shipObjectWorld.createDoubleSumAugmentation("core", "z"), root.x(), root.y(), root.z(), level.dimensionId)

            val center = Vector3d(collectX / componentSize.toDouble(), collectY / componentSize.toDouble(), collectZ / componentSize.toDouble())

            center.add(0.5, 0.5, 0.5)

            pocketQueue.add(PocketForcesQueueable(root, center, componentSize, gasMap.first, gasMap.second))
            gametickKnownPocketRoots.add(root)
            //ClockworkMod.LOGGER.info("Added pocket with root: ($root)")
        }

        val toRemove = ArrayList(gametickKnownPocketRoots.filterNot { roots[it] != null })
        while (toRemove.isNotEmpty()) {
            val polled = toRemove.removeFirst()
            if (pocketsOnRemoval.containsKey(polled)) {
                pocketsOnRemoval[polled] = pocketsOnRemoval[polled]!! + 1
            } else {
                pocketsOnRemoval[polled] = 1
            }
            if (pocketsOnRemoval[polled]!! > 4) {
                pocketRemoveQueue.add(polled)
                pocketsOnRemoval.remove(polled)
                gametickKnownPocketRoots.remove(polled)
                //ClockworkMod.LOGGER.info("Removed pocket with root: ($polled)")
            }
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