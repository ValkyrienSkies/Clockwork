package org.valkyrienskies.clockwork.content.forces.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.Level
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector3ic
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkAugmentations
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandItem
import org.valkyrienskies.clockwork.content.forces.BalloonController.Companion.isValidBalloonEnclosure
import org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle.LeakParticleData
import org.valkyrienskies.clockwork.util.AABBHelper.mergeAdjacentFast
import org.valkyrienskies.clockwork.util.ClockworkUtils.retrieveGasInfoFromPocket
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.DuctNetworkServer
import org.valkyrienskies.kelvin.impl.client.particle.DefaultGasParticle
import org.valkyrienskies.kelvin.impl.registry.GasParticlePickerRegistry
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.mod.api.positionToWorld
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.collections.set
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class BalloonData {
    val regions: ArrayList<AABBic>
    val gasMasses: HashMap<String, Double>
    var currentEnergy: Double
    var currentVolume: Double
    var isLeaking: Boolean
    @JsonIgnore
    var missingExternalPositions: Int
    @JsonIgnore
    var leakPositions: HashSet<Vector3ic> = hashSetOf()
    @JsonIgnore
    var lastSentLeaks: HashSet<Vector3ic> = hashSetOf()

    @JsonIgnore
    var currentMaxLeaks = 1

    @JsonIgnore
    var shouldRemove = false

    @JsonIgnore
    var shouldReScan = false

    @JsonIgnore
    var shouldValidate = false

    @JsonIgnore
    var timeSinceLeakSound = 0

    // Default constructor for Jackson, should never be invoked manually
    @Deprecated("")
    constructor() {
        this.regions = ArrayList()
        this.gasMasses = HashMap()
        this.currentEnergy = 0.0
        this.currentVolume = 0.0
        this.isLeaking = false
        this.missingExternalPositions = 0
    }

    constructor(regions: ArrayList<AABBic>, gasMasses: HashMap<String, Double>, currentEnergy: Double, currentVolume: Double, isLeaking: Boolean) {
        this.regions = regions
        this.gasMasses = gasMasses
        this.currentEnergy = currentEnergy
        this.currentVolume = currentVolume
        this.isLeaking = isLeaking
        this.missingExternalPositions = 0
    }

    fun tick(level: ServerLevel, ship: LoadedServerShip): Boolean {

        if (this.isLeaking && missingExternalPositions > 0) {
            var averageLeakPos = Vector3i(0,0,0)
            for (pos in leakPositions) {
                val blockPos = BlockPos(pos.x(), pos.y(), pos.z())
                if (level.isLoaded(blockPos)) {
                    sendLeakParticles(level, blockPos, Direction.UP)
                }
                averageLeakPos.add(pos)
            }
            averageLeakPos.div(leakPositions.size)
            if (timeSinceLeakSound <= 0) {

                //HEAVY leak if over half max leaks, light otherwise
                level.playSound(
                    null,
                    averageLeakPos.x.toDouble(),
                    averageLeakPos.y.toDouble(),
                    averageLeakPos.z.toDouble(),
                    if (missingExternalPositions >= currentMaxLeaks / 2) ClockworkSounds.BALLOON_LEAKING_HEAVY.mainEvent!! else ClockworkSounds.BALLOON_LEAKING_LIGHT.mainEvent!!,
                    SoundSource.BLOCKS,
                    0.5f,
                    1f
                )
                timeSinceLeakSound = 120
            }
        }

        if (timeSinceLeakSound > 0) {
            timeSinceLeakSound --
        }

        //copy pasted from pocket forces
        // Just so we can have x,y,z instead of first,second,third
        val root = this.getCenter()
        val rootYInWorld = ship.transform.positionToWorld(Vector3d(root.x().toDouble(), root.y().toDouble(), root.z().toDouble())).y
        val atmoDensity = level.shipObjectWorld.aerodynamicUtils.getAirDensityForY(rootYInWorld, level.dimensionId)
        val atmoPressure = level.shipObjectWorld.aerodynamicUtils.getAirPressureForY(rootYInWorld, level.dimensionId) //level.shipObjectWorld.aerodynamicUtils.getAirPressureForY(rootYInWorld, level.dimensionId)
        val atmoTemperature = level.shipObjectWorld.aerodynamicUtils.getAirTemperatureForY(rootYInWorld, level.dimensionId)

        val volume = this.currentVolume

        var currentHeatEnergy = this.currentEnergy
        //map, with gasses as GasType instead of the string of their resource location
        var currentGasMasses: HashMap<GasType, Double> = HashMap()
        for ((key, value) in gasMasses) {
            val gasType = GasTypeRegistry.getGasType(ResourceLocation(key)) ?: continue
            currentGasMasses[gasType] = value
        }
        //println("mass: ${gasMasses.values.sum()};  energy: $currentHeatEnergy;  temperature: ${currentHeatEnergy/(KelvinMod.getKelvin() as DuctNetworkServer).mixtureCapacity(gasMasses)}")

        val totalMass = currentGasMasses.values.sum()
        if (currentHeatEnergy.isNaN() || currentHeatEnergy <= 1e-9 || totalMass.isNaN() || totalMass < 1e-9) {

            val air = GasTypeRegistry.getGasType("kelvin","air")!!
            gasMasses[air.resourceLocation.toString()] = atmoDensity * volume

            val capacity = 1000 * air.specificHeatCapacity / air.adiabaticIndex
            currentHeatEnergy = atmoTemperature * volume*atmoDensity * capacity
            currentEnergy = currentHeatEnergy
            return false
        }


        val moles = currentGasMasses.entries.sumOf { it.key.massToMoles(it.value) }
        val capacity = (KelvinMod.getKelvin() as DuctNetworkServer).mixtureCapacity(currentGasMasses)
        var currentTemperature = currentHeatEnergy / capacity
        val currentPressure = moles * DuctNetwork.idealGasConstant * currentTemperature / volume
        val molarMass = currentGasMasses.entries.sumOf { it.key.density * 0.0224 * it.value } / totalMass
        val estimatedSurfaceArea = 4.84 * volume.pow(2.0/3.0)

        // Gas leak exiting
        val gasExitRate = max(0.0, (currentPressure - atmoPressure)) * estimatedSurfaceArea * ClockworkConfig.SERVER.permeabilityConstant / sqrt(currentTemperature * DuctNetwork.idealGasConstant / molarMass) * max(1.0, missingExternalPositions.toDouble() + 1.0)
        val exitGas = gasExitRate * 0.01
        val exitGasMasses = HashMap<GasType, Double>()
        currentGasMasses.forEach {
            currentGasMasses[it.key] = currentGasMasses[it.key]!! - exitGas * it.value / totalMass
            exitGasMasses[it.key] = exitGas * it.value / totalMass}
        val exitHeat =  currentTemperature * ClockworkMod.getKelvin().mixtureCapacity(exitGasMasses)

        currentHeatEnergy -= exitHeat
        currentTemperature = currentHeatEnergy / capacity

        // Gas leak heat transfer
        val heatFlow = ClockworkConfig.SERVER.heatTransferCoefficient * estimatedSurfaceArea * (atmoTemperature - currentTemperature) * max(1.0, missingExternalPositions.toDouble() * 2.0 + 1.0)
        var newHeatEnergy = currentHeatEnergy + heatFlow * 0.05
        val newCapacity = (KelvinMod.getKelvin() as DuctNetworkServer).mixtureCapacity(currentGasMasses)
        var newTemperature = newHeatEnergy / newCapacity

        // Gas leak entering
        val air = GasTypeRegistry.GAS_TYPES[ResourceLocation("kelvin","air")]!!
        val newMoles = currentGasMasses.entries.sumOf { it.key.massToMoles(it.value) }
        val newPressure = newMoles * DuctNetwork.idealGasConstant * newTemperature / volume
        val addMoles = max(0.0, atmoPressure - newPressure) * volume / (newTemperature * DuctNetwork.idealGasConstant)
        currentGasMasses[air] = (currentGasMasses[air] ?: 0.0) + air.molesToMass(addMoles)

        val addedHeat = air.molesToMass(addMoles) * atmoTemperature * (air.specificHeatCapacity * 1000 / air.adiabaticIndex)
        newHeatEnergy += addedHeat

        // Set Values
        this.gasMasses.clear()
        currentGasMasses.forEach { this.gasMasses[it.key.resourceLocation.toString()] = it.value }
        this.currentEnergy = newHeatEnergy

    return true
    }

    fun makeForceData(): PhysBalloonData {
        val internalDensity = if (currentVolume > 1e-9) {
            gasMasses.values.sum() / currentVolume
        } else {
            0.0
        }
        val center = getCenter()
        return PhysBalloonData(
            center = center,
            internalDensity = internalDensity,
            volume = currentVolume
        )
    }

    fun recalculateVolume(): Double {
        var totalVolume = 0.0
        for (region in regions) {
            totalVolume += region.volume()
        }
        this.currentVolume = totalVolume
        return totalVolume
    }

    fun getCenter(): Vector3dc {
        val center = Vector3d()
        if (regions.isEmpty()) {
            return center
        }
        var sumX = 0.0
        var sumY = 0.0
        var sumZ = 0.0
        for (region in regions) {
            sumX += (region.minX() + region.maxX()) / 2.0
            sumY += (region.minY() + region.maxY()) / 2.0
            sumZ += (region.minZ() + region.maxZ()) / 2.0
        }
        val count = regions.size
        center.x = sumX / count.toDouble()
        center.y = sumY / count.toDouble()
        center.z = sumZ / count.toDouble()
        return center
    }

    fun containsPosition(pos: BlockPos): Boolean {
        for (region in regions) {
            if (pos.x >= region.minX() && pos.x < region.maxX() &&
                pos.y >= region.minY() && pos.y < region.maxY() &&
                pos.z >= region.minZ() && pos.z < region.maxZ()) {
                return true
            }
        }
        return false
    }

    fun getFirstValidExternalPosition(level: Level): BlockPos? {
        return getExternalPositions().firstOrNull { pos ->
            level.isLoaded(pos) && level.getBlockState(pos).isValidBalloonEnclosure(level, pos)
        }
    }

    fun getExternalPositions(): Set<BlockPos> {
        val externalPositions = mutableSetOf<BlockPos>()
        val directions = arrayOf(
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(-1.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 0.0),
            //Vector3d(0.0, -1.0, 0.0),
            Vector3d(0.0, 0.0, 1.0),
            Vector3d(0.0, 0.0, -1.0)
        )

        for (region in regions) {
            for (x in region.minX() until region.maxX()) {
                for (y in region.minY() until region.maxY()) {
                    for (z in region.minZ() until region.maxZ()) {
                        val currentPos = BlockPos(x, y, z)
                        for (dir in directions) {
                            val neighborPos = currentPos.offset(dir.x.toInt(), dir.y.toInt(), dir.z.toInt())
                            if (!containsPosition(neighborPos)) {
                                externalPositions.add(neighborPos)
                            }
                        }
                    }
                }
            }
        }
        return externalPositions
    }

    fun canLeak(externalSize: Int): Boolean {
        currentMaxLeaks = max(1, externalSize / 4)
        return gasMasses.values.sum() > 1e-9 && missingExternalPositions < currentMaxLeaks
    }

    fun isNearlyAtmospheric(level: ServerLevel): Boolean {
        val root = this.getCenter()
        val ship = level.getLoadedShipManagingPos(BlockPos(root.x().toInt(), root.y().toInt(), root.z().toInt())) ?: return false

        val rootYInWorld = ship.transform.positionToWorld(Vector3d(root.x().toDouble(), root.y().toDouble(), root.z().toDouble())).y
        val atmoPressure = level.shipObjectWorld.aerodynamicUtils.getAirPressureForY(rootYInWorld, level.dimensionId)
        val internalPressure = run {
            val moles = gasMasses.entries.sumOf { GasTypeRegistry.getGasType(ResourceLocation(it.key))!!.massToMoles(it.value) }
            val capacity = (KelvinMod.getKelvin() as DuctNetworkServer).mixtureCapacity(gasMasses.mapKeys { GasTypeRegistry.getGasType(ResourceLocation(it.key))!! })
            val temperature = currentEnergy / capacity
            moles * DuctNetwork.idealGasConstant * temperature / currentVolume
        }

        return (internalPressure - atmoPressure).absoluteValue <= 1e-4

    }

    fun validate(level: Level): EnclosureStatus {
        if (regions.isEmpty()) {
            return EnclosureStatus.INVALID
        }
        val externals = getExternalPositions()
        if (externals.isEmpty()) {
            return EnclosureStatus.INVALID
        }
        var currentStatus = EnclosureStatus.VALID
        //in hindsight, this is not really efficient anyways, so i wont bother
//        if (leakPositions.isNotEmpty()) {
//            val sealed = hashSetOf<Vector3ic>()
//            for (pos in leakPositions) {
//                val blockPos = BlockPos(pos.x(), pos.y(), pos.z())
//                if (level.isLoaded(blockPos)) {
//                    val blockState = level.getBlockState(blockPos)
//                    if (blockState.isValidBalloonEnclosure(level, blockPos)) {
//                        sealed.add(pos)
//                    }
//                }
//            }
//            leakPositions.removeAll(sealed)
//            if (leakPositions.isEmpty()) {
//                this.isLeaking = false
//                missingExternalPositions = 0
//                currentStatus = EnclosureStatus.VALID
//            } else {
//                currentStatus = EnclosureStatus.LEAKING
//            }
//        }
        lastSentLeaks.clear()
        lastSentLeaks = HashSet(leakPositions)
        missingExternalPositions = 0
        leakPositions.clear()
        for (pos in externals) {
            if (!level.isLoaded(pos)) {
                return EnclosureStatus.UNKNOWN
            }
            val blockState = level.getBlockState(pos)
            if (!blockState.isValidBalloonEnclosure(level, pos)) {
                missingExternalPositions ++
                leakPositions.add(Vector3i(pos.x, pos.y, pos.z))
                currentStatus = if (canLeak(externals.size) && !isNearlyAtmospheric(level as ServerLevel)) {
                    EnclosureStatus.LEAKING
                } else {
                    EnclosureStatus.INVALID
                }
                if (level is ServerLevel) {
                    val isNew = !lastSentLeaks.contains(Vector3i(pos.x, pos.y, pos.z))
                    if (isNew) {
                        level.playSound(
                            null,
                            pos.x.toDouble(),
                            pos.y.toDouble(),
                            pos.z.toDouble(),
                            ClockworkSounds.BALLOON_RUPTURE.mainEvent!!,
                            SoundSource.BLOCKS,
                            1f,
                            0.9f + level.random.nextFloat() * 0.2f
                        )
                        sendInitialLeakParticleBurst(level, pos, Direction.UP)
                    }
                }
            }
        }
        this.isLeaking = currentStatus == EnclosureStatus.LEAKING
        return currentStatus
    }

    fun findDirectionToOutside(level: ServerLevel, pos: BlockPos, externals: Set<BlockPos>): Direction {
        for (dir in Direction.values()) {
            val check = pos.relative(dir)
            if (!this.containsPosition(check) && !externals.contains(check) && !level.getBlockState(check).isValidBalloonEnclosure(level, check)) {
                //println(dir)
                return dir
            }
        }
        return Direction.UP
    }

    fun sendInitialLeakParticleBurst(level: ServerLevel, position: BlockPos, dir: Direction) {
        val thisShip = level.getLoadedShipManagingPos(position) ?: return
        val center = getCenter().sub(0.5, 0.5, 0.5, Vector3d())
        val realDir = Vector3f(position.x.toFloat(), position.y.toFloat(), position.z.toFloat()).sub(Vector3f(center.x().toFloat(), center.y().toFloat(), center.z().toFloat())).normalize()
        val worldDirection = thisShip.transform.shipToWorldRotation.transform(Vector3f(realDir.x.toFloat(), realDir.y.toFloat(), realDir.z.toFloat()))
        val leakParticle = LeakParticleData(worldDirection, 0.1f + level.random.nextFloat() * 0.2f)
        level.sendParticles(
            leakParticle,
            position.x.toDouble(),
            position.y.toDouble(),
            position.z.toDouble(),
            level.random.nextInt(50, 100),
            0.5,
            0.5,
            0.5,
            1.0
        )
    }

    fun sendLeakParticles(level: ServerLevel, position: BlockPos, dir: Direction) {
        val thisShip = level.getLoadedShipManagingPos(position) ?: return
        val center = getCenter().sub(0.5, 0.5, 0.5, Vector3d())
        val realDir = Vector3f(position.x.toFloat(), position.y.toFloat(), position.z.toFloat()).sub(Vector3f(center.x().toFloat(), center.y().toFloat(), center.z().toFloat())).normalize()
        val worldDirection = thisShip.transform.shipToWorldRotation.transform(Vector3f(realDir.x.toFloat(), realDir.y.toFloat(), realDir.z.toFloat()))
        for (i in 1..missingExternalPositions.coerceAtMost(4)) {
            val leakParticle = LeakParticleData(worldDirection, 0.05f + level.random.nextFloat() * 0.5f)
            level.sendParticles(
                leakParticle,
                position.x.toDouble() + 0.5,
                position.y.toDouble() + 0.5,
                position.z.toDouble() + 0.5,
                if (missingExternalPositions >= currentMaxLeaks / 2) 2 else 1,
                0.5,
                0.5,
                0.5,
                1.0
            )
        }
    }

    /**
     * Updates the regions of the balloon and validates its enclosure status.
     *
     * Returns true if the balloon is valid or leaking, false if it should be removed.
     */
    fun updateRegions(newRegions: List<AABBic>, level: Level): Boolean {
        regions.clear()
        regions.addAll(newRegions)
        recalculateVolume()
        val result = validate(level)
        isLeaking = result == EnclosureStatus.LEAKING
        shouldRemove = result == EnclosureStatus.INVALID
        return result.isAtLeast(EnclosureStatus.UNKNOWN)
    }

    fun updateRegionsNoValidation(newRegions: List<AABBic>, level: Level) {
        regions.clear()
        regions.addAll(newRegions)
        recalculateVolume()
    }

    fun mergeWith(other: BalloonData, level: Level): EnclosureStatus {
        this.regions.addAll(other.regions)
        val merged = mergeAdjacentFast(this.regions)
        this.regions.clear()
        this.regions.addAll(merged) // this is scuffed af
        for ((gasType, mass) in other.gasMasses) {
            this.gasMasses[gasType] = this.gasMasses.getOrDefault(gasType, 0.0) + mass
        }
        this.currentEnergy += other.currentEnergy
        recalculateVolume()
        return validate(level)
    }

    fun trySplit(level: Level): Pair<Boolean, ArrayList<BalloonData>> {
        val result = WanderwandItem.findIsolatedAABBComponents(this.regions, level)
        if (result.size == 1) {
            return Pair(false, arrayListOf())
        } else if (result.size == 0) {
            // this bloon is cooked...
            this.shouldRemove = true
            return Pair(true, arrayListOf())
        }
        val newBalloons = ArrayList<BalloonData>()
        val totalVolume = this.currentVolume
        result.sortByDescending( { it.volume() })

        val thisBecomes = result[0]
        var newThis: BalloonData? = null

        for (component in result) {
            val newBalloon = BalloonData(
                regions = component as ArrayList<AABBic>, //sussy cast but oh well
                gasMasses = HashMap(),
                currentEnergy = 0.0,
                currentVolume = 0.0,
                isLeaking = false
            )
            newBalloon.recalculateVolume()
            val volumeFraction = newBalloon.currentVolume / totalVolume
            for ((gasType, mass) in this.gasMasses) {
                newBalloon.gasMasses[gasType] = mass * volumeFraction
            }
            newBalloon.currentEnergy = this.currentEnergy * volumeFraction
            val status = newBalloon.validate(level)
            if (status.isAtLeast(EnclosureStatus.LEAKING)) {
                newBalloons.add(newBalloon)
                if (component == thisBecomes) {
                    newThis = newBalloon
                }
            }
        }
        this.clear()
        if (newThis != null) {
            this.regions.addAll(newThis.regions)
            this.gasMasses.putAll(newThis.gasMasses)
            this.currentEnergy = newThis.currentEnergy
            this.isLeaking = newThis.isLeaking
            result.remove(thisBecomes)
        }
        this.recalculateVolume()
        return Pair(true, newBalloons)
    }

    private fun clear() {
        regions.clear()
        gasMasses.clear()
        currentEnergy = 0.0
        currentVolume = 0.0
        isLeaking = false
        missingExternalPositions = 0
    }

    private fun AABBic.volume(): Long {
        val dx = (maxX() - minX()).toLong()
        val dy = (maxY() - minY()).toLong()
        val dz = (maxZ() - minZ()).toLong()
        return dx * dy * dz
    }

    private fun List<AABBic>.volume(): Long {
        var total = 0L
        for (aabb in this) {
            total += aabb.volume()
        }
        return total
    }

    enum class EnclosureStatus {
        VALID,
        LEAKING,
        UNKNOWN,
        INVALID;

        fun isAtLeast(status: EnclosureStatus): Boolean {
            return this.ordinal >= status.ordinal
        }

        fun weakestOf(other: EnclosureStatus): EnclosureStatus {
            return if (this.ordinal < other.ordinal) this else other
        }
    }

    data class PhysBalloonData(
        val center : Vector3dc,
        val internalDensity : Double,
        val volume : Double
    )
}
