package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.valkyrienskies.clockwork.ClockworkAugmentations
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.utilities.PocketForcesQueueable
import org.valkyrienskies.clockwork.util.AerodynamicUtils.dimensionMap
import org.valkyrienskies.clockwork.util.ClockworkUtils.retrieveGasInfoFromPocket
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.connectivity.ConnectionStatus
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.util.pollUntilEmpty
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.DuctNetworkServer
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.mod.api.positionToWorld
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashMap
import kotlin.math.*

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class PocketForcesController: ShipPhysicsListener {

    var dimensionId: DimensionId = "minecraft:dimension:minecraft:overworld"

    @JsonIgnore
    private val pocketQueue: ConcurrentLinkedQueue<PocketForcesQueueable> = ConcurrentLinkedQueue()


    // Todo: Implement serialization

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        val physShipImpl = physShip

        val buoyancyForce = calculateBuoyancyForce(physShip, physLevel)

        //ClockworkMod.LOGGER.info(physShip.mass.toString())

        buoyancyForce.forEach {
            if (it.value > 1.0) println(it.value.toString() + "to" + it.key.toString())
            if (it.value.isFinite() && !it.value.isNaN()) { //just to be safe

                physShipImpl.applyWorldForceToModelPos(Vector3d(0.0, it.value, 0.0))
            }
        }
    }

    fun calculateBuoyancyForce(physShip: PhysShip, physLevel: PhysLevel): Map<Vector3dc, Double> {
        val physShipImpl = physShip

        var totalBuoyantForce = HashMap<Vector3dc, Double>()
        if (dimensionMap[dimensionId] == null || dimensionMap[dimensionId]!!.maxY <= 0.0) return totalBuoyantForce



        pocketQueue.pollUntilEmpty {
            val yHeight = physShip.transform.shipToWorld.transformPosition(it.pocketCenter, Vector3d()).y()
            val atmoDensity = physLevel.aerodynamicUtils.getAirDensityForY(yHeight, this.dimensionId)

            val atmoGravity = physLevel.aerodynamicUtils.getAtmosphereForDimension(this.dimensionId).third
            val buoyantForce = it.pocketVolume * (atmoDensity - it.hotDensity) * atmoGravity * ClockworkConfig.SERVER.balloonForceMult
            totalBuoyantForce[it.pocketCenter] = max(buoyantForce, 0.0)
        }
        pocketQueue.clear()

        return totalBuoyantForce
    }

    @OptIn(GameTickOnly::class, VsBeta::class)
    fun gameTick(level: ServerLevel, id: Long) {
        val ship = level.shipObjectWorld.loadedShips.getById(id) ?: return

        // Todo: Replace with better 'get all air pockets' once theres an api method for that
        val roots = level.shipObjectWorld.getFromEachAirComponent(ClockworkAugmentations.getComponentAugmentation("heatEnergy"), level.dimensionId, ship.chunkClaim)

        for (r: Triple<Int, Int, Int> in roots.keys) {



            // Just so we can have x,y,z instead of first,second,third
            val root = Vector3i(r.first,r.second,r.third)
            if (level.shipObjectWorld.isIsolatedAir(root.x, root.y, root.z, dimensionId) != ConnectionStatus.DISCONNECTED) continue

            val (gasMasses, heatEnergy) = retrieveGasInfoFromPocket(root, level)
            //val pressure = level.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("pressure"), root.x(), root.y(), root.z(), level.dimensionId)
            //val temperature = level.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("temperature"), root.x(), root.y(), root.z(), level.dimensionId)
            val volume = level.shipObjectWorld.getAirComponentSize(root.x(), root.y(), root.z(), dimensionId)

            var totalInternalDensity = gasMasses.values.sum() / volume

            pocketQueue.add(PocketForcesQueueable(getPocketCenter(level, root), volume.toDouble(), totalInternalDensity))
        }


        // region Ass code - to be re-worked

        for (r: Triple<Int, Int, Int> in roots.keys) {
            // Just so we can have x,y,z instead of first,second,third
            val root = Vector3i(r.first,r.second,r.third)
            val rootYInWorld = ship.transform.positionToWorld(Vector3d(root.x().toDouble(), root.y().toDouble(), root.z().toDouble())).y + 0.5
            val atmoDensity = level.shipObjectWorld.aerodynamicUtils.getAirDensityForY(rootYInWorld, level.dimensionId)
            val atmoPressure = level.shipObjectWorld.aerodynamicUtils.getAirPressureForY(rootYInWorld, dimensionId) //level.shipObjectWorld.aerodynamicUtils.getAirPressureForY(rootYInWorld, level.dimensionId)
            val atmoTemperature = level.shipObjectWorld.aerodynamicUtils.getAirTemperatureForY(rootYInWorld, level.dimensionId)

            val volume = level.shipObjectWorld.getAirComponentSize(root.x(), root.y(), root.z(), dimensionId).toDouble()
//            //if (level.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("airupdated"), root.x(), root.y(), root.z(), level.dimensionId) < 1.0 && roots[r]!! > 0) {
//
//                level.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("gas_air"), root.x(), root.y(), root.z(), level.dimensionId, requiredMassForDensity)
//                level.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("airupdated"), root.x(), root.y(), root.z(), level.dimensionId, 1.0)
//
//
//            //ClockworkMod.LOGGER.info("Removed pocket with root: ($root)")
//            }
            val isSealed = level.shipObjectWorld.collectAirAugmentation(ClockworkAugmentations.getAugmentation("sealed"), root.x(), root.y(), root.z(), level.dimensionId) > 0.0
            if (!isSealed) {
                var (gasMasses, currentHeatEnergy) = retrieveGasInfoFromPocket(root, level)
                //println("mass: ${gasMasses.values.sum()};  energy: $currentHeatEnergy;  temperature: ${currentHeatEnergy/(KelvinMod.getKelvin() as DuctNetworkServer).mixtureCapacity(gasMasses)}")

                if (currentHeatEnergy.isNaN()) {

                    val key = ClockworkAugmentations.getComponentAugmentation("gas/kelvin:air")
                    level.shipObjectWorld.setAirComponentAugmentation(key, root.x,root.y,root.z,dimensionId, volume*atmoDensity)

                    val air = GasTypeRegistry.getGasType("kelvin","air")!!


                    val capacity = 1000 * air.specificHeatCapacity / air.adiabaticIndex
                    level.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("heatEnergy"),
                        root.x,root.y,root.z,dimensionId, atmoTemperature * volume*atmoDensity * capacity)

                    continue
                }

                val totalMass = gasMasses.values.sum()
                val moles = gasMasses.entries.sumOf { it.key.massToMoles(it.value) }
                val capacity = (KelvinMod.getKelvin() as DuctNetworkServer).mixtureCapacity(gasMasses)
                var currentTemperature = currentHeatEnergy / capacity
                val currentPressure = moles * DuctNetwork.idealGasConstant * currentTemperature / volume
                val molarMass = gasMasses.entries.sumOf { it.key.density * 0.0224 * it.value } / totalMass
                val estimatedSurfaceArea = 4.84 * volume.pow(2.0/3.0)

                // Gas leak exiting
                val gasExitRate = max(0.0, currentPressure - atmoPressure) * estimatedSurfaceArea * ClockworkConfig.SERVER.permeabilityConstant / sqrt(currentTemperature * DuctNetwork.idealGasConstant / molarMass)
                val exitGas = gasExitRate * 0.05
                val exitGasMasses = HashMap<GasType, Double>()
                gasMasses.forEach {
                    gasMasses[it.key] = gasMasses[it.key]!! - exitGas * it.value / totalMass
                    exitGasMasses[it.key] = exitGas * it.value / totalMass}
                val exitHeat =  currentTemperature * ClockworkMod.getKelvin().mixtureCapacity(exitGasMasses)

                currentHeatEnergy -= exitHeat
                currentTemperature = currentHeatEnergy / capacity

                // Gas leak heat transfer
                val heatFlow = ClockworkConfig.SERVER.heatTransferCoefficient * estimatedSurfaceArea * (atmoTemperature - currentTemperature)
                var newHeatEnergy = currentHeatEnergy + heatFlow * 0.05
                val newCapacity = (KelvinMod.getKelvin() as DuctNetworkServer).mixtureCapacity(gasMasses)
                var newTemperature = newHeatEnergy / newCapacity

                // Gas leak entering
                val air = GasTypeRegistry.GAS_TYPES[ResourceLocation("kelvin","air")]!!
                val newMoles = gasMasses.entries.sumOf { it.key.massToMoles(it.value) }
                val newPressure = newMoles * DuctNetwork.idealGasConstant * newTemperature / volume
                val addMoles = max(0.0, atmoPressure - newPressure) * volume / (newTemperature * DuctNetwork.idealGasConstant)
                gasMasses[air] = (gasMasses[air] ?: 0.0) + air.molesToMass(addMoles)

                val addedHeat = air.molesToMass(addMoles) * atmoTemperature * (air.specificHeatCapacity * 1000 / air.adiabaticIndex)
                newHeatEnergy += addedHeat

                // Set Values
                gasMasses.forEach {
                    val key = ClockworkAugmentations.getComponentAugmentation("gas/" + it.key.resourceLocation.toString())
                    level.shipObjectWorld.setAirComponentAugmentation(key, root.x,root.y,root.z,dimensionId, it.value)
                }
                level.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("heatEnergy"),
                    root.x,root.y,root.z,dimensionId, newHeatEnergy)

            }
        }
    }


    private fun getPocketCenter(level: ServerLevel, pos: Vector3i): Vector3d {
        val pocketSize = level.shipObjectWorld.getAirComponentSize(pos.x(), pos.y(), pos.z(), level.dimensionId)
        val collectX = level.shipObjectWorld.collectAirAugmentation(level.shipObjectWorld.createDoubleSumAugmentation("core", "x"), pos.x(), pos.y(), pos.z(), level.dimensionId)
        val collectY = level.shipObjectWorld.collectAirAugmentation(level.shipObjectWorld.createDoubleSumAugmentation("core", "y"), pos.x(), pos.y(), pos.z(), level.dimensionId)
        val collectZ = level.shipObjectWorld.collectAirAugmentation(level.shipObjectWorld.createDoubleSumAugmentation("core", "z"), pos.x(), pos.y(), pos.z(), level.dimensionId)
        return Vector3d(collectX/pocketSize, collectY/pocketSize, collectZ/pocketSize)
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
