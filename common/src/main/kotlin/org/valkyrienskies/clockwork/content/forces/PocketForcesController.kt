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
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.AerodynamicUtils
import org.valkyrienskies.clockwork.util.AerodynamicUtils.GRAVITATIONAL_ACCELERATION
import org.valkyrienskies.clockwork.util.AerodynamicUtils.UNIVERSAL_GAS_CONSTANT
import org.valkyrienskies.clockwork.util.AerodynamicUtils.calcPressure
import org.valkyrienskies.clockwork.util.AerodynamicUtils.densityAverage
import org.valkyrienskies.clockwork.util.AerodynamicUtils.densityFromPressureAverage
import org.valkyrienskies.clockwork.util.AerodynamicUtils.dimensionMap
import org.valkyrienskies.clockwork.util.AerodynamicUtils.extraHeatInfoAverage
import org.valkyrienskies.clockwork.util.AerodynamicUtils.specificHeatAverage
import org.valkyrienskies.clockwork.util.ClockworkUtils.retrieveGasInfoFromPocket
import org.valkyrienskies.clockwork.util.PIDstance
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.util.x
import org.valkyrienskies.core.util.y
import org.valkyrienskies.core.util.z
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashMap
import kotlin.math.*

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class PocketForcesController: ShipPhysicsListener {

    var dimensionId: DimensionId = "minecraft:dimension:minecraft:overworld"

    @JsonIgnore
    val gametickKnownPocketRoots: HashSet<Vector3ic> = HashSet()
    @JsonIgnore
    val pocketRoots: HashMap<Vector3ic, Long> = HashMap()
    @JsonIgnore
    val pocketCenters: HashMap<Vector3ic, Vector3dc> = HashMap()
    @JsonIgnore
    val gasMasses: HashMap<Vector3ic, HashMap<GasType, Double>> = HashMap()
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

    @JsonIgnore
    private val pidstances = HashMap<Vector3ic, PIDstance>()

    // Todo: Implement serialization

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
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
            if (it.value.isFinite() && !it.value.isNaN()) { //just to be safe
                physShipImpl.applyWorldForceToModelPos(Vector3d(0.0, it.value, 0.0), Vector3d(pocketCenters[it.key]!!))
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
        if (dimensionMap[dimensionId] != null && dimensionMap[dimensionId]!!.maxY <= 0.0) {
            return totalBuoyantForce
        }

        pocketRoots.keys.forEach {
            if (pocketRoots[it]!! > 0) {
                if (gasMasses.containsKey(it) && temperatures.containsKey(it)) {
                    val gasMassesLocal = gasMasses[it]!!
                    val temperature = max(temperatures[it]!!, 0.0001)
                    val pressure = pressures[it]!!
                    var totalInternalDensity = gasMassesLocal.values.sum() / pocketRoots[it]!!.toDouble() //gasMassesLocal.values.sum() / pocketRoots[it]!!.toDouble()
//                    for (gas in GasType.entries) {
//                        if (gasMassesLocal.containsKey(gas)) {
//                            val gasMass = gasMassesLocal[gas] ?: 0.0
//                            if (gasMass == 0.0) continue
//
//                            val density = getDensityFromTemperature(pocketRoots[it]!!.toDouble(), gasMass, temperature, gas)
//                            totalInternalDensity += density
//                        }
//                    }
                    if (totalInternalDensity != 0.0 && AerodynamicUtils.getAirDensityForY(physShip.transform.shipToWorld.transformPosition(pocketCenters[it], Vector3d()).y(), this.dimensionId) != 0.0) {
                        //ClockworkMod.LOGGER.info("Density at Y: " + AerodynamicUtils.getAirDensityForY(physShip.transform.positionInWorld.y(), max_height).toString())
                        //ClockworkMod.LOGGER.info("Internal Density: $totalInternalDensity")
                        val buoyantForce = (pocketRoots[it]!!.toDouble() * (AerodynamicUtils.getAirDensityForY(physShip.transform.shipToWorld.transformPosition(pocketCenters[it], Vector3d()).y(), this.dimensionId) - totalInternalDensity) * GRAVITATIONAL_ACCELERATION) * ClockworkConfig.SERVER.balloonForceMult
                        totalBuoyantForce[it] = max(buoyantForce, 0.0)
                    }
                }
            }
        }

        return totalBuoyantForce
    }

    fun gameTick(level: ServerLevel, ship: ServerShip) {
        val loadedShip = level.shipObjectWorld.loadedShips.getById(ship.id) ?: return

        val roots = level.shipObjectWorld.getFromEachAirComponent(ClockworkAugmentations.getComponentAugmentation("temperature"), level.dimensionId, loadedShip.chunkClaim)
        for (r: Triple<Int, Int, Int> in roots.keys) {
            val root = Vector3i(r.first,r.second,r.third)
            val gasMap = retrieveGasInfoFromPocket(root, level)
            val pressure = level.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("pressure"), root.x(), root.y(), root.z(), level.dimensionId)

            val componentSize = roots[r] ?: continue

            val collectX = level.shipObjectWorld.collectAirAugmentation(level.shipObjectWorld.createDoubleSumAugmentation("core", "x"), root.x(), root.y(), root.z(), level.dimensionId)
            val collectY = level.shipObjectWorld.collectAirAugmentation(level.shipObjectWorld.createDoubleSumAugmentation("core", "y"), root.x(), root.y(), root.z(), level.dimensionId)
            val collectZ = level.shipObjectWorld.collectAirAugmentation(level.shipObjectWorld.createDoubleSumAugmentation("core", "z"), root.x(), root.y(), root.z(), level.dimensionId)

            val center = Vector3d(collectX / componentSize, collectY / componentSize, collectZ / componentSize)

            center.add(0.5, 0.5, 0.5)

            pocketQueue.add(PocketForcesQueueable(root, center, componentSize.toLong(), gasMap.first, gasMap.second, pressure))
            gametickKnownPocketRoots.add(root)
            //ClockworkMod.LOGGER.info("Added pocket with root: ($root)")
        }

        for (root in gametickKnownPocketRoots) {
            val r = Triple(root.x,root.y,root.z)
            if (roots[r] != null) {
                val rootYInWorld = ship.transform.shipToWorld.transformPosition(Vector3d(root.x().toDouble(), root.y().toDouble(), root.z().toDouble())).y + 0.5
                val atmosphericDensityAtRoot = AerodynamicUtils.getAirDensityForY(rootYInWorld, level.dimensionId)
                val atmosphericPressureAtRoot = AerodynamicUtils.getAirPressureForY(rootYInWorld, level.dimensionId)
                val atmosphericTemperatureAtRoot = AerodynamicUtils.getAirTemperatureForY(rootYInWorld, level.dimensionId)
                val requiredMassForDensity = atmosphericDensityAtRoot * roots[r]!!.toDouble()
                if (level.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("airupdated"), root.x(), root.y(), root.z(), level.dimensionId) < 1.0 && roots[r]!! > 0) {

                    level.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("gas_air"), root.x(), root.y(), root.z(), level.dimensionId, requiredMassForDensity)
                    level.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("airupdated"), root.x(), root.y(), root.z(), level.dimensionId, 1.0)
                    //ClockworkMod.LOGGER.info("Removed pocket with root: ($root)")
                }
                val isSealed = level.shipObjectWorld.collectAirAugmentation(ClockworkAugmentations.getAugmentation("sealed"), root.x(), root.y(), root.z(), level.dimensionId) > 0.0
                if (!isSealed && roots[r]!! > 0) {

                    if (!pidstances.containsKey(root)) {
                        pidstances[root] = PIDstance(p = 0.5)
                    }

                    var static = false // this causes problems,,,, very annoying!!!!1!

                    //leaking
                    var (gasMassesInternal, temperatureInternal) = retrieveGasInfoFromPocket(root, level)
                    temperatureInternal = max(temperatureInternal, 0.0001)
                    val internalPressure = calcPressure(gasMassesInternal.values.sum(), roots[r]!!.toDouble(), temperatureInternal,
                        densityAverage(gasMassesInternal))
                    var internalDensity = densityFromPressureAverage(gasMassesInternal, temperatureInternal, internalPressure)

                    //if (internalDensity.absoluteValue < 0.001) static = true

                    val leakRate = ClockworkConfig.SERVER.pocketLeakageRate
                    val leakOrificeSize = ((roots[r]!!.toDouble() * 2.0) / 3.0) * 0.1

                    val massInFlowRate: Double
                    val massOutFlowRate: Double
                    var combinedMassFlowRate: Double = 0.0
                    var totalDeltaMass: Double = 0.0

                    if (leakOrificeSize < 0.001 && leakOrificeSize != 0.0) static = true

                    if (leakOrificeSize != 0.0) {
                        val newGasMasses = HashMap(gasMassesInternal)

                        val pressureDifference = internalPressure - atmosphericPressureAtRoot
                        val chokedFlow = if (atmosphericPressureAtRoot != 0.0) {
                            (internalPressure/atmosphericPressureAtRoot > 1.89)
                        } else {
                            false
                        }
                        val adjustment = pidstances[root]!!.control(requiredMassForDensity, gasMassesInternal.values.sum(), 0.75)
                        val (averageSpecificGasConstant, averageSutherlandConstant, averageAdiabaticIndex)  = extraHeatInfoAverage(gasMassesInternal)
                        if (pressureDifference >= 0.001 || pressureDifference <= -0.001 && !static) {
                            //leaking out
                            if (pressureDifference >= 0.0001) {
                                massOutFlowRate = if (!chokedFlow) {
                                    leakRate * leakOrificeSize * sqrt(2.0 * internalDensity * pressureDifference.absoluteValue)
                                } else {
                                    leakRate * leakOrificeSize * internalPressure * sqrt(averageAdiabaticIndex / (averageSpecificGasConstant * temperatureInternal)) * Math.pow(
                                        (averageAdiabaticIndex + 1.0) / 2.0,
                                        -((averageAdiabaticIndex + 1.0) / (2.0 * (averageAdiabaticIndex - 1.0)))
                                    )
                                } * -1.0 - adjustment


                                if (massOutFlowRate.absoluteValue <= 0.001 || massOutFlowRate.isNaN() || massOutFlowRate.isInfinite()) {
                                    static = true
                                }
                            } else {
                                massOutFlowRate = 0.0
                            }
                            // leaking in
                            if (pressureDifference <= -0.0001 && atmosphericDensityAtRoot != 0.0) {
                                massInFlowRate = if (!chokedFlow) {
                                    (leakRate * leakOrificeSize * sqrt(2.0 * atmosphericDensityAtRoot * pressureDifference.absoluteValue))
                                } else {
                                    (leakRate * leakOrificeSize * atmosphericPressureAtRoot * sqrt(1.4 / (287.0 * atmosphericTemperatureAtRoot)) * Math.pow(
                                        (1.4 + 1.0) / 2.0,
                                        -((1.4 + 1.0) / (2.0 * (1.4 - 1.0)))
                                    ))
                                } + adjustment


                                if (massInFlowRate.absoluteValue <= 0.001 || massInFlowRate.isNaN() || massInFlowRate.isInfinite()) {
                                    static = true
                                }
                            } else {
                                massInFlowRate = 0.0
                            }
                        } else {
                            massInFlowRate = 0.0
                            massOutFlowRate = 0.0
                            totalDeltaMass = 0.0
                            static = true
                        }

                        combinedMassFlowRate = (massInFlowRate + massOutFlowRate) / 20.0

                        forEachGas@ for (gas in GasTypeRegistry.GAS_TYPES.values) {
                            if (gasMassesInternal.containsKey(gas)) {
                                if (gasMassesInternal[gas]!! <= 0.0) {
                                    newGasMasses[gas] = 0.0
                                    continue
                                }
                                val limit: Double = gasMassesInternal[gas]!! / 2.0

                                val deltaMass = Mth.clamp(combinedMassFlowRate, -limit, if (gas.name == "Air") requiredMassForDensity/2.0 else 0.0) * 0.1

                                newGasMasses[gas] = gasMassesInternal[gas]!! + deltaMass
                                newGasMasses[gas] = max(newGasMasses[gas]!!, 0.0)
                                totalDeltaMass += deltaMass
                            }
                        }

                        if (newGasMasses.values.sum() <= 0.0 || (totalDeltaMass < 0.0001 && totalDeltaMass > -0.0001)) {
                            static = true
                        }
                        ////outflow
                        //                            temperatureInternal * ((newGasMasses.values.sum() / gasMassesInternal.values.sum())).pow(averageAdiabaticIndex - 1.0)
                        // else if (pressureDifference <= -0.001) {
                        //                            //inflow
                        //                            (gasMassesInternal.values.sum() * temperatureInternal + totalDeltaMass * atmosphericTemperatureAtRoot) / (gasMassesInternal.values.sum() + totalDeltaMass)
                        //                        }

                        val pretendSurfaceArea = 4 * Math.PI * (roots[r]!!.toDouble().pow(2.0 / 3.0))

                        val pretendConductionCoefficient = 0.025

                        val specificHeatAverage = min(specificHeatAverage(newGasMasses), 0.0001)

                        val conduction = Mth.clamp((pretendConductionCoefficient * pretendSurfaceArea * (atmosphericTemperatureAtRoot - temperatureInternal) / 20.0) / (newGasMasses.values.sum() * specificHeatAverage), -5.0, 5.0)

//                        val deltaPocketTemperature = Mth.clamp(if (pressureDifference >= 0.001 || pressureDifference <= -0.001) {
//                            (gasMassesInternal.values.sum() * temperatureInternal + massInFlowRate * atmosphericTemperatureAtRoot / 20.0 - massOutFlowRate * temperatureInternal / 20.0) / if(newGasMasses.values.sum() >= 0.0001) newGasMasses.values.sum() else 0.0001
//                        } else {
//                            temperatureInternal
//                        } + conduction, 0.0001, 5772.0) - temperatureInternal

                        val newPocketTemperature = max(min(temperatureInternal + conduction, 5772.0), 0.0001)

                        val newPocketPressure = calcPressure(newGasMasses.values.sum(), roots[r]!!.toDouble(), newPocketTemperature, densityAverage(newGasMasses))
                        if (!newPocketPressure.isNaN() && newPocketPressure.isFinite()) level.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("pressure"), root.x(), root.y(), root.z(), level.dimensionId, newPocketPressure)
                        if (!newPocketTemperature.isNaN() && newPocketTemperature.isFinite()) level.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("temperature"), root.x(), root.y(), root.z(), level.dimensionId, newPocketTemperature)


                        //ClockworkMod.LOGGER.info("delta mass: $totalDeltaMass // pressure diff: $pressureDifference // temperature: $temperatureInternal // conduction: $conduction, // new temperature: $newPocketTemperature")
                        if (!static) {
                            for (gas in GasTypeRegistry.GAS_TYPES.values) {
                                if (newGasMasses[gas] != null && newGasMasses[gas]!!.isFinite() && !newGasMasses[gas]!!.isNaN()) level.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("gas_" + gas.name.lowercase()), root.x(), root.y(), root.z(), level.dimensionId, newGasMasses[gas]!!)
                            }
                        } else {
                            //ClockworkMod.LOGGER.info("Static pocket at: $root")
                        }

                        //incredible fix. insanity. truly.
                    }
                }
            }
        }

        val toRemove = ArrayList(gametickKnownPocketRoots.filterNot { roots[Triple(it.x,it.y,it.z)] != null })
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
                pidstances.remove(polled)
                //ClockworkMod.LOGGER.info("Removed pocket with root: ($polled)")
            }
        }
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip): PocketForcesController? {
            if (ship.getAttachment(PocketForcesController::class.java) == null) {
                val controller = PocketForcesController()
                controller.dimensionId = ship.chunkClaimDimension
                ship.setAttachment(controller)
            }
            val controller = ship.getAttachment(PocketForcesController::class.java)
            controller!!.dimensionId = ship.chunkClaimDimension
            return controller
        }
    }
}
