package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
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
import org.valkyrienskies.clockwork.util.AerodynamicUtils.calcPressure
import org.valkyrienskies.clockwork.util.AerodynamicUtils.densityAverage
import org.valkyrienskies.clockwork.util.AerodynamicUtils.densityFromPressureAverage
import org.valkyrienskies.clockwork.util.AerodynamicUtils.extraHeatInfoAverage
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
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.sqrt

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
    val pressures: HashMap<Vector3ic, Double> = HashMap()
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
            if (!(it.value.isInfinite() || it.value.isNaN())) {
                physShipImpl.applyInvariantForceToPos(Vector3d(0.0, it.value, 0.0), Vector3d(pocketCenters[it.key]!!).sub(physShip.transform.positionInShip))
            }
        }
    }

    private fun processPocketQueue() {
        val pocket = pocketQueue.poll()
        pocketRoots[pocket.rootPos] = pocket.pocketSize
        pocketCenters[pocket.rootPos] = pocket.pocketCenter
        gasMasses[pocket.rootPos] = pocket.gasMasses
        temperatures[pocket.rootPos] = pocket.temperature
        pressures[pocket.rootPos] = pocket.pressure
    }

    fun calculateBuoyancyForce(physShip: PhysShip): Map<Vector3ic, Double> {
        val physShipImpl = physShip

        var totalBuoyantForce = HashMap<Vector3ic, Double>()

        pocketRoots.keys.forEach {
            if (pocketRoots[it]!! > 0) {
                if (gasMasses.containsKey(it) && temperatures.containsKey(it)) {
                    val gasMassesLocal = gasMasses[it]!!
                    val temperature = temperatures[it]!!
                    val pressure = pressures[it]!!
                    var totalInternalDensity = densityFromPressureAverage(gasMassesLocal, temperature, pressure)
//                    for (gas in GasType.entries) {
//                        if (gasMassesLocal.containsKey(gas)) {
//                            val gasMass = gasMassesLocal[gas] ?: 0.0
//                            if (gasMass == 0.0) continue
//
//                            val density = getDensityFromTemperature(pocketRoots[it]!!.toDouble(), gasMass, temperature, gas)
//                            totalInternalDensity += density
//                        }
//                    }
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
            val pressure = level.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("pressure"), root.x(), root.y(), root.z(), level.dimensionId)

            val componentSize = roots[root] ?: continue

            val collectX = level.shipObjectWorld.collectAirAugmentation(level.shipObjectWorld.createDoubleSumAugmentation("core", "x"), root.x(), root.y(), root.z(), level.dimensionId)
            val collectY = level.shipObjectWorld.collectAirAugmentation(level.shipObjectWorld.createDoubleSumAugmentation("core", "y"), root.x(), root.y(), root.z(), level.dimensionId)
            val collectZ = level.shipObjectWorld.collectAirAugmentation(level.shipObjectWorld.createDoubleSumAugmentation("core", "z"), root.x(), root.y(), root.z(), level.dimensionId)

            val center = Vector3d(collectX / componentSize.toDouble(), collectY / componentSize.toDouble(), collectZ / componentSize.toDouble())

            center.add(0.5, 0.5, 0.5)

            pocketQueue.add(PocketForcesQueueable(root, center, componentSize, gasMap.first, gasMap.second, pressure))
            gametickKnownPocketRoots.add(root)
            //ClockworkMod.LOGGER.info("Added pocket with root: ($root)")
        }

        for (root in gametickKnownPocketRoots) {
            if (roots[root] != null) {
                val rootYInWorld = ship.transform.shipToWorld.transformPosition(Vector3d(root.x().toDouble(), root.y().toDouble(), root.z().toDouble())).y
                val atmosphericDensityAtRoot = AerodynamicUtils.getAirDensityForY(rootYInWorld, max_height)
                val atmosphericPressureAtRoot = AerodynamicUtils.getAirPressureForY(rootYInWorld, max_height)
                if (level.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("airupdated"), root.x(), root.y(), root.z(), level.dimensionId) < 1.0 && roots[root]!! > 0) {
                    val requiredMassForDensity = atmosphericDensityAtRoot * roots[root]!!.toDouble()
                    level.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("gas_air"), root.x(), root.y(), root.z(), level.dimensionId, requiredMassForDensity)
                    level.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("airupdated"), root.x(), root.y(), root.z(), level.dimensionId, 1.0)
                    //ClockworkMod.LOGGER.info("Removed pocket with root: ($root)")
                }
                val isSealed = level.shipObjectWorld.collectAirAugmentation(ClockworkAugmentations.getAugmentation("sealed"), root.x(), root.y(), root.z(), level.dimensionId) > 0.0
                if (!isSealed && roots[root]!! > 0) {

                    //leaking
                    val (gasMassesInternal, temperatureInternal) = retrieveGasInfoFromPocket(root, level)
                    val internalPressure = level.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("pressure"), root.x(), root.y(), root.z(), level.dimensionId)
                    val internalDensity = densityFromPressureAverage(gasMassesInternal, temperatureInternal, internalPressure)
                    val leakRate = ClockworkConfig.SERVER.pocketLeakageRate
                    var leakOrificeSize = ((roots[root]!!.toDouble() * 2.0) / 3.0) * 0.1

                    var static = false

                    if (leakOrificeSize < 0.001 && leakOrificeSize != 0.0) leakOrificeSize = 0.001

                    if (leakOrificeSize != 0.0) {
                        val newGasMasses = EnumMap<GasType, Double>(GasType::class.java)

                        val pressureDifference = internalPressure - atmosphericPressureAtRoot
                        val chokedFlow = (internalPressure/atmosphericPressureAtRoot > 1.89)
                        val (averageSpecificGasConstant, averageSutherlandConstant, averageAdiabaticIndex)  = extraHeatInfoAverage(gasMassesInternal)
                        if (pressureDifference >= 0.001) {
                            //leaking out
                            val massFlowRate = if (!chokedFlow) {
                                leakRate * leakOrificeSize * sqrt(2.0 * internalDensity * pressureDifference)
                            } else {
                                leakRate * leakOrificeSize * internalPressure * sqrt(averageAdiabaticIndex / (averageSpecificGasConstant * temperatureInternal)) * Math.pow((averageAdiabaticIndex + 1.0) / 2.0, -((averageAdiabaticIndex + 1.0) / (2.0 * (averageAdiabaticIndex - 1.0))))
                            }

                            forEachGas@ for (gas in GasType.entries) {
                                if (massFlowRate < 0.001) break@forEachGas
                                if (gasMassesInternal.containsKey(gas)) {
                                    val limit: Double = gasMassesInternal[gas]!! / 2.0

                                    val deltaMass = Mth.clamp(massFlowRate, 0.0, limit)

                                    newGasMasses[gas] = gasMassesInternal[gas]!! - deltaMass
                                }
                            }

                        } else if (pressureDifference <= -0.001) {
                            // leaking in
                            val massFlowRate = if (!chokedFlow) {
                                (leakRate * leakOrificeSize * sqrt(2.0 * internalDensity * pressureDifference.absoluteValue))
                            } else {
                                (leakRate * leakOrificeSize * atmosphericPressureAtRoot * sqrt(1.4 / (287.0 * temperatureInternal)) * Math.pow((1.4 + 1.0) / 2.0, -((1.4 + 1.0) / (2.0 * (1.4 - 1.0)))))
                            }

                            forEachGas@ for (gas in GasType.entries) {
                                if (massFlowRate > -0.001) break@forEachGas
                                if (gasMassesInternal.containsKey(gas)) {
                                    val deltaMass = if (gas == GasType.AIR) massFlowRate else 0.0
                                    newGasMasses[gas] = gasMassesInternal[gas]!! + deltaMass
                                }
                            }
                        } else {
                            static = true
                        }

                        if (!static) {
                            var newPocketPressure = calcPressure(newGasMasses.values.sum(), roots[root]!!.toDouble(), temperatureInternal, densityAverage(newGasMasses))
                            var newPocketTemperature = ((newPocketPressure * roots[root]!!.toDouble()) / ((if (newGasMasses.values.sum() != 0.0) newGasMasses.values.sum() else 0.0001) * averageSpecificGasConstant))

                            if (newPocketPressure.isNaN() || newPocketPressure.isInfinite()) newPocketPressure = 0.001
                            if (newPocketTemperature.isNaN() || newPocketTemperature.isInfinite()) newPocketTemperature = 0.001

                            for (gas in GasType.entries) {
                                if (newGasMasses[gas] != null)
                                level.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("gas_" + gas.name.lowercase()), root.x(), root.y(), root.z(), level.dimensionId, newGasMasses[gas]!!)
                            }
                            level.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("temperature"), root.x(), root.y(), root.z(), level.dimensionId, newPocketTemperature)
                            level.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("pressure"), root.x(), root.y(), root.z(), level.dimensionId, newPocketPressure)
                        }
                    }
                }
            }
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