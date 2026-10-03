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
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.forces.BalloonController.Companion.isValidBalloonEnclosure
import org.valkyrienskies.clockwork.content.forces.BalloonGeometry
import org.valkyrienskies.clockwork.content.forces.BalloonThermodynamics
import org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle.LeakParticleData
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.mod.api.positionToWorld
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.abs
import kotlin.math.max

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class BalloonData {
    val regions: ArrayList<AABBic>
    val gasMasses: HashMap<String, Double>
    var currentEnergy: Double
    var currentVolume: Double
    var isLeaking: Boolean
    @JsonIgnore var missingExternalPositions = 0
    @JsonIgnore var leakPositions: HashSet<Vector3ic> = hashSetOf()
    @JsonIgnore var lastSentLeaks: HashSet<Vector3ic> = hashSetOf()
    @JsonIgnore var currentMaxLeaks = 1
    @JsonIgnore var shouldRemove = false
    @JsonIgnore var shouldReScan = false
    // Revalidate after loading.
    @JsonIgnore var shouldValidate = true
    @JsonIgnore var timeSinceLeakSound = 0
    @JsonIgnore var validationRetryTicks = 0
    @JsonIgnore var geometryVersion = 0
        private set
    @JsonIgnore var enclosureStatus = EnclosureStatus.UNKNOWN
        private set
    @JsonIgnore private var geometryCache: BalloonGeometry? = null
    @JsonIgnore private var validatedGeometry = false
    @JsonIgnore private val dirtyBoundary = HashSet<BlockPos>()
    @JsonIgnore private var waitingForChunk: BlockPos? = null

    @Deprecated("For Jackson")
    constructor() : this(arrayListOf(), hashMapOf(), 0.0, 0.0, false)

    constructor(regions: ArrayList<AABBic>, gasMasses: HashMap<String, Double>, currentEnergy: Double, currentVolume: Double, isLeaking: Boolean) {
        this.regions = regions
        this.gasMasses = gasMasses
        this.currentEnergy = currentEnergy
        this.currentVolume = currentVolume
        this.isLeaking = isLeaking
    }

    @get:JsonIgnore
    internal val geometry: BalloonGeometry
        get() = geometryCache ?: BalloonGeometry(regions).also {
            geometryCache = it
            currentVolume = it.cells.size.toDouble()
        }

    fun recalculateVolume(): Double {
        geometryCache = null
        validatedGeometry = false
        waitingForChunk = null
        shouldValidate = true
        validationRetryTicks = 0
        geometryVersion++
        return geometry.cells.size.toDouble()
    }

    fun getCenter(): Vector3dc = geometry.center
    fun containsPosition(pos: BlockPos) = pos in geometry.cells
    fun getExternalPositions(): Set<BlockPos> = geometry.enclosure
    fun touchesPosition(pos: BlockPos) = pos in geometry.cells || pos in geometry.boundary

    fun markBoundaryDirty(pos: BlockPos) {
        if (pos in geometry.enclosure) {
            dirtyBoundary.add(pos.immutable())
            shouldValidate = true
            validationRetryTicks = 0
        }
    }

    internal fun waitForChunk(pos: BlockPos) {
        waitingForChunk = pos.immutable()
        shouldValidate = true
        enclosureStatus = EnclosureStatus.UNKNOWN
        validationRetryTicks = 20
    }

    internal fun chunksReady(level: Level): Boolean {
        val pos = waitingForChunk ?: return true
        if (!level.isLoaded(pos)) {
            validationRetryTicks = 20
            return false
        }
        waitingForChunk = null
        return true
    }

    fun getFirstValidExternalPosition(level: Level): BlockPos? = geometry.enclosure.firstOrNull {
        level.isLoaded(it) && level.getBlockState(it).isValidBalloonEnclosure(level, it)
    }

    fun interiorSeed(level: Level): BlockPos? = geometry.cells.asSequence().filter {
        level.isLoaded(it) && !level.getBlockState(it).isValidBalloonEnclosure(level, it)
    }.maxByOrNull { it.y }

    fun validate(level: Level): EnclosureStatus {
        if (!chunksReady(level)) return EnclosureStatus.UNKNOWN
        val exterior = geometry.enclosure
        if (exterior.isEmpty()) {
            enclosureStatus = EnclosureStatus.INVALID
            shouldValidate = false
            return enclosureStatus
        }
        val incremental = validatedGeometry && dirtyBoundary.isNotEmpty()
        val targets = if (incremental) dirtyBoundary else exterior
        val unloaded = targets.firstOrNull { !level.isLoaded(it) }
        if (unloaded != null) {
            waitForChunk(unloaded)
            return enclosureStatus
        }
        val leaks = if (incremental) HashSet(leakPositions) else hashSetOf()
        for (pos in targets) {
            val key = Vector3i(pos.x, pos.y, pos.z)
            if (level.getBlockState(pos).isValidBalloonEnclosure(level, pos)) leaks.remove(key) else leaks.add(key)
        }
        val oldLeaks = leakPositions
        lastSentLeaks = HashSet(oldLeaks)
        leakPositions = leaks
        missingExternalPositions = leaks.size
        currentMaxLeaks = max(1, exterior.size / 4)
        isLeaking = leaks.isNotEmpty()
        enclosureStatus = when {
            !isLeaking -> EnclosureStatus.VALID
            leaks.size < currentMaxLeaks -> EnclosureStatus.LEAKING
            else -> EnclosureStatus.INVALID
        }
        if (validatedGeometry && level is ServerLevel) {
            for (pos in leaks - oldLeaks) {
                val blockPos = BlockPos(pos.x(), pos.y(), pos.z())
                announceLeak(level, blockPos)
            }
        }
        validatedGeometry = true
        dirtyBoundary.clear()
        shouldValidate = false
        validationRetryTicks = 0
        return enclosureStatus
    }

    internal fun announceLeak(level: ServerLevel, pos: BlockPos) {
        level.playSound(null, pos, ClockworkSounds.BALLOON_RUPTURE.mainEvent!!, SoundSource.BLOCKS, 1f, 0.9f + level.random.nextFloat() * 0.2f)
        sendInitialLeakParticleBurst(level, pos, Direction.UP)
    }

    internal fun resolvedMasses(): HashMap<GasType, Double> {
        val result = HashMap<GasType, Double>()
        for ((key, mass) in gasMasses) {
            val location = ResourceLocation.tryParse(key) ?: continue
            val gas = GasTypeRegistry.getGasType(location) ?: continue
            if (mass.isFinite() && mass >= 0.0) result[gas] = mass
        }
        return result
    }

    fun tick(level: ServerLevel, ship: LoadedServerShip): Boolean {
        if (level.dimensionId != ship.chunkClaimDimension || enclosureStatus == EnclosureStatus.UNKNOWN) return false
        if (isLeaking && leakPositions.isNotEmpty()) {
            val average = Vector3d()
            for (pos in leakPositions) {
                val blockPos = BlockPos(pos.x(), pos.y(), pos.z())
                if (level.isLoaded(blockPos)) sendLeakParticles(level, blockPos, Direction.UP)
                average.add(pos.x().toDouble(), pos.y().toDouble(), pos.z().toDouble())
            }
            average.div(leakPositions.size.toDouble())
            if (timeSinceLeakSound <= 0) {
                level.playSound(null, average.x, average.y, average.z,
                    if (missingExternalPositions * 2 >= currentMaxLeaks) ClockworkSounds.BALLOON_LEAKING_HEAVY.mainEvent!! else ClockworkSounds.BALLOON_LEAKING_LIGHT.mainEvent!!,
                    SoundSource.BLOCKS, 0.5f, 1f)
                timeSinceLeakSound = 120
            }
        }
        if (timeSinceLeakSound > 0) timeSinceLeakSound--
        val y = ship.transform.positionToWorld(Vector3d(getCenter())).y
        val atmosphere = level.shipObjectWorld.aerodynamicUtils
        val density = atmosphere.getAirDensityForY(y, level.dimensionId)
        val pressure = atmosphere.getAirPressureForY(y, level.dimensionId)
        val temperature = atmosphere.getAirTemperatureForY(y, level.dimensionId)
        val masses = resolvedMasses()
        val air = GasTypeRegistry.getGasType("kelvin", "air") ?: return false
        if (!currentEnergy.isFinite() || currentEnergy <= 0.0 || gasMasses.values.any { !it.isFinite() || it < 0.0 } || masses.values.sum() < 1e-9) {
            gasMasses.clear()
            gasMasses[air.resourceLocation.toString()] = max(0.0, density * currentVolume)
            currentEnergy = temperature * gasMasses.values.sum() * BalloonThermodynamics.specificCapacity(air)
            return false
        }
        val config = ClockworkConfig.SERVER
        currentEnergy = BalloonThermodynamics.step(masses, currentEnergy, currentVolume, air,
            pressure, temperature, config.permeabilityConstant, config.heatTransferCoefficient,
            config.leakHeatTransferMultiplier, missingExternalPositions)
        gasMasses.clear()
        masses.forEach { (gas, mass) -> gasMasses[gas.resourceLocation.toString()] = mass }
        return currentEnergy.isFinite() && masses.values.all { it.isFinite() && it >= 0.0 }
    }

    fun makeForceData(level: ServerLevel, ship: LoadedServerShip): PhysBalloonData {
        val center = getCenter()
        val y = ship.transform.positionToWorld(Vector3d(center)).y
        val density = level.shipObjectWorld.aerodynamicUtils.getAirDensityForY(y, level.dimensionId)
        return PhysBalloonData(Vector3d(center), max(0.0, density * currentVolume - gasMasses.values.sum()), currentVolume)
    }

    /** Hot gas can already be at ambient pressure. */
    fun isNearlyAtmospheric(level: ServerLevel): Boolean {
        val center = getCenter()
        val ship = level.getLoadedShipManagingPos(BlockPos.containing(center.x(), center.y(), center.z())) ?: return false
        val y = ship.transform.positionToWorld(Vector3d(center)).y
        val atmosphere = level.shipObjectWorld.aerodynamicUtils
        val ambientMass = atmosphere.getAirDensityForY(y, level.dimensionId) * currentVolume
        val ambientTemperature = atmosphere.getAirTemperatureForY(y, level.dimensionId)
        val masses = resolvedMasses()
        val capacity = BalloonThermodynamics.capacity(masses)
        if (capacity <= 1e-9) return true
        return abs(masses.values.sum() - ambientMass) <= max(1e-6, ambientMass * 0.001) &&
            abs(currentEnergy / capacity - ambientTemperature) <= max(0.1, ambientTemperature * 0.001)
    }

    private fun leakDirection(position: BlockPos, fallback: Direction): Vector3f {
        // Keep shipyard precision until after subtraction.
        val offset = Vector3d(position.x + 0.5, position.y + 0.5, position.z + 0.5).sub(getCenter())
        if (offset.lengthSquared() < 1e-12) return fallback.step()
        offset.normalize()
        return Vector3f(offset.x.toFloat(), offset.y.toFloat(), offset.z.toFloat())
    }

    fun sendInitialLeakParticleBurst(level: ServerLevel, position: BlockPos, dir: Direction) {
        val ship = level.getLoadedShipManagingPos(position) ?: return
        val direction = ship.transform.shipToWorldRotation.transform(leakDirection(position, dir))
        level.sendParticles(LeakParticleData(direction, 0.1f + level.random.nextFloat() * 0.2f),
            position.x + 0.5, position.y + 0.5, position.z + 0.5, level.random.nextInt(50, 100), 0.5, 0.5, 0.5, 1.0)
    }

    fun sendLeakParticles(level: ServerLevel, position: BlockPos, dir: Direction) {
        val ship = level.getLoadedShipManagingPos(position) ?: return
        val direction = ship.transform.shipToWorldRotation.transform(leakDirection(position, dir))
        for (i in 1..missingExternalPositions.coerceAtMost(4)) {
            level.sendParticles(LeakParticleData(direction, 0.05f + level.random.nextFloat() * 0.5f),
                position.x + 0.5, position.y + 0.5, position.z + 0.5,
                if (missingExternalPositions * 2 >= currentMaxLeaks) 2 else 1, 0.5, 0.5, 0.5, 1.0)
        }
    }

    fun updateRegions(newRegions: List<AABBic>, level: Level): Boolean {
        updateRegionsNoValidation(newRegions, level)
        shouldRemove = validate(level) == EnclosureStatus.INVALID
        return !shouldRemove
    }

    fun updateRegionsNoValidation(newRegions: List<AABBic>, level: Level) {
        val copy = newRegions.toList()
        regions.clear()
        regions.addAll(copy)
        recalculateVolume()
    }

    /** Caller removes the source balloon. */
    fun mergeWith(other: BalloonData, level: Level): EnclosureStatus {
        if (other === this) return enclosureStatus
        val cells = HashSet(geometry.cells).apply { addAll(other.geometry.cells) }
        updateRegionsNoValidation(BalloonGeometry.regions(cells), level)
        for ((gas, mass) in other.gasMasses) gasMasses[gas] = gasMasses.getOrDefault(gas, 0.0) + mass
        currentEnergy += other.currentEnergy
        return EnclosureStatus.UNKNOWN
    }

    fun trySplit(level: Level): Pair<Boolean, ArrayList<BalloonData>> {
        val components = geometry.airComponents(level) ?: return false to arrayListOf()
        if (components.isEmpty()) {
            shouldRemove = true
            return true to arrayListOf()
        }
        if (components.size == 1 && components[0].size == geometry.cells.size) return false to arrayListOf()
        val masses = HashMap(gasMasses)
        val energy = currentEnergy
        val remainingVolume = components.sumOf { it.size }.toDouble()
        val additional = arrayListOf<BalloonData>()
        for ((index, component) in components.withIndex()) {
            val fraction = component.size / remainingVolume
            val target = if (index == 0) this else BalloonData(arrayListOf(), hashMapOf(), 0.0, 0.0, false)
            target.updateRegionsNoValidation(BalloonGeometry.regions(component), level)
            target.gasMasses.clear()
            masses.forEach { (gas, mass) -> target.gasMasses[gas] = mass * fraction }
            target.currentEnergy = energy * fraction
            target.shouldRemove = target.validate(level) == EnclosureStatus.INVALID
            if (index > 0 && !target.shouldRemove) additional.add(target)
        }
        return true to additional
    }

    enum class EnclosureStatus {
        VALID, LEAKING, UNKNOWN, INVALID;
        fun isAtLeast(status: EnclosureStatus) = ordinal <= status.ordinal
        fun weakestOf(other: EnclosureStatus) = if (ordinal >= other.ordinal) this else other
    }

    data class PhysBalloonData(val center: Vector3dc, val hotAir: Double, val volume: Double)
}
