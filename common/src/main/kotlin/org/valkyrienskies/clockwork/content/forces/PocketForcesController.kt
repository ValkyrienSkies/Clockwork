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
import org.valkyrienskies.clockwork.content.logistics.gas.utilities.PocketForcesQueueable
import org.valkyrienskies.clockwork.util.AerodynamicUtils.GRAVITATIONAL_ACCELERATION
import org.valkyrienskies.clockwork.util.AerodynamicUtils.dimensionMap
import org.valkyrienskies.clockwork.util.ClockworkUtils.retrieveGasInfoFromPocket
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.impl.DuctNetworkServer
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.Locale
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
            if (it.value.isFinite() && !it.value.isNaN()) { //just to be safe
                physShipImpl.applyWorldForceToModelPos(Vector3d(0.0, it.value, 0.0), Vector3d(it.key))
            }
        }
    }

    fun calculateBuoyancyForce(physShip: PhysShip, physLevel: PhysLevel): Map<Vector3dc, Double> {
        val physShipImpl = physShip

        var totalBuoyantForce = HashMap<Vector3dc, Double>()
        if (dimensionMap[dimensionId] != null && dimensionMap[dimensionId]!!.maxY <= 0.0) return totalBuoyantForce



        pocketQueue.forEach {
            val yHeight = physShip.transform.shipToWorld.transformPosition(it.pocketCenter, Vector3d()).y()

            val buoyantForce = it.pocketVolume * (physLevel.aerodynamicUtils.getAirDensityForY(yHeight, this.dimensionId) - it.hotDensity) * 10.0 * ClockworkConfig.SERVER.balloonForceMult
            totalBuoyantForce[it.pocketCenter] = max(buoyantForce, 0.0)
        }

        return totalBuoyantForce
    }

    @OptIn(GameTickOnly::class)
    fun gameTick(level: ServerLevel, id: Long) {
        val ship = level.shipObjectWorld.loadedShips.getById(id) ?: return

        // Todo: Replace with better 'get all air pockets' once theres an api method for that
        val roots = level.shipObjectWorld.getFromEachAirComponent(ClockworkAugmentations.getComponentAugmentation("heatEnergy"), level.dimensionId, ship.chunkClaim)

        for (r: Triple<Int, Int, Int> in roots.keys) {
            // Just so we can have x,y,z instead of first,second,third
            val root = Vector3i(r.first,r.second,r.third)

            val gasMap = retrieveGasInfoFromPocket(root, level)
            //val pressure = level.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("pressure"), root.x(), root.y(), root.z(), level.dimensionId)
            //val temperature = level.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("temperature"), root.x(), root.y(), root.z(), level.dimensionId)
            val volume = level.shipObjectWorld.getAirComponentSize(root.x(), root.y(), root.z(), dimensionId)

            var totalInternalDensity = gasMap.first.values.sum() / volume

            pocketQueue.add(PocketForcesQueueable(getPocketCenter(level, root), volume.toDouble(), totalInternalDensity))
        }


        // region Ass code - to be re-worked

        for (r: Triple<Int, Int, Int> in roots.keys) {
            // Just so we can have x,y,z instead of first,second,third
            val root = Vector3i(r.first,r.second,r.third)
            val rootYInWorld = ship.transform.shipToWorld.transformPosition(Vector3d(root.x().toDouble(), root.y().toDouble(), root.z().toDouble())).y + 0.5
            val atmosphericDensityAtRoot = level.shipObjectWorld.aerodynamicUtils.getAirDensityForY(rootYInWorld, level.dimensionId)
            val atmosphericPressureAtRoot = level.shipObjectWorld.aerodynamicUtils.getAirPressureForY(rootYInWorld, level.dimensionId)
            val atmosphericTemperatureAtRoot = level.shipObjectWorld.aerodynamicUtils.getAirTemperatureForY(rootYInWorld, level.dimensionId)
            val requiredMassForDensity = atmosphericDensityAtRoot * roots[r]!!.toDouble()
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
            if (!isSealed && roots[r]!! > 0) {
                val (gasMass, currentHeatEnergy) = retrieveGasInfoFromPocket(root, level)
                val totalMass = gasMass.values.sum()
                val moles = gasMass.entries.sumOf { it.key.massToMoles(it.value) }
                val heatCapacity = (KelvinMod.getKelvin() as DuctNetworkServer).mixtureCapacity(gasMass)
                val currentTemperature = currentHeatEnergy / (heatCapacity * totalMass)
                val currentPressure = moles * DuctNetwork.idealGasConstant * currentTemperature / volume
                val molarMass = gasMass.entries.sumOf { it.key.density * 0.0224 * it.value } / totalMass
                val estimatedSurfaceArea = 4.84 * volume.pow(2.0/3.0)

                // Gas leak exiting
                val gasExitRate = max(0.0, currentPressure - atmosphericPressureAtRoot) * estimatedSurfaceArea * ClockworkConfig.SERVER.permeabilityConstant / sqrt(currentTemperature * DuctNetwork.idealGasConstant / molarMass)
                val exitGas = gasExitRate * 0.05
                gasMass.forEach { gasMass[it.key] = gasMass[it.key]!! - exitGas * it.value / totalMass }

                // Gas leak heat transfer
                val heatFlow = ClockworkConfig.SERVER.heatTransferCoefficient * estimatedSurfaceArea * max(0.0,currentTemperature - atmosphericTemperatureAtRoot)
                val newHeatEnergy = heatFlow * 0.05
                val newHeatCapacity = (KelvinMod.getKelvin() as DuctNetworkServer).mixtureCapacity(gasMass)
                val newTemperature = newHeatEnergy / (newHeatCapacity * totalMass)

                // Gas leak entering
                val air = GasTypeRegistry.GAS_TYPES[ResourceLocation("kelvin","air")]!!
                val newMoles = gasMass.entries.sumOf { it.key.massToMoles(it.value) }
                val newPressure = newMoles * DuctNetwork.idealGasConstant * newTemperature / volume
                val addMoles = max(0.0, atmosphericPressureAtRoot - newPressure) * volume / (newTemperature * DuctNetwork.idealGasConstant)
                gasMass[air] = (gasMass[air] ?: 0.0) + air.molesToMass(addMoles)

                // Set Values
                gasMass.forEach {
                    val key = ClockworkAugmentations.getComponentAugmentation("gas_" + it.key.name.lowercase(Locale.getDefault()))
                    level.shipObjectWorld.setAirComponentAugmentation(key, root.x,root.y,root.z,dimensionId, it.value)
                }
                level.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("heatEnergy"),
                    root.x,root.y,root.z,dimensionId,
                    newHeatEnergy)

            }
        }
    }

    private fun getPocketCenter(level: ServerLevel, pos: Vector3i): Vector3d {
        val collectX = level.shipObjectWorld.collectAirAugmentation(level.shipObjectWorld.createDoubleSumAugmentation("core", "x"), pos.x(), pos.y(), pos.z(), level.dimensionId)
        val collectY = level.shipObjectWorld.collectAirAugmentation(level.shipObjectWorld.createDoubleSumAugmentation("core", "y"), pos.x(), pos.y(), pos.z(), level.dimensionId)
        val collectZ = level.shipObjectWorld.collectAirAugmentation(level.shipObjectWorld.createDoubleSumAugmentation("core", "z"), pos.x(), pos.y(), pos.z(), level.dimensionId)
        return Vector3d(collectX, collectY, collectZ)
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
