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
import org.valkyrienskies.clockwork.content.forces.BalloonDuctNode
import org.valkyrienskies.clockwork.content.forces.BalloonGeometry
import org.valkyrienskies.clockwork.content.forces.BalloonThermodynamics
import org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle.LeakParticleData
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinChunkPos
import org.valkyrienskies.kelvin.util.NodeThermodynamics
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
    var sealed: Boolean = false
    @JsonIgnore var kelvinNodePos: DuctNodePos? = null
        private set
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
    constructor() : this(arrayListOf(), hashMapOf(), 0.0, 0.0, false, false)

    constructor(
        regions: ArrayList<AABBic>,
        gasMasses: HashMap<String, Double>,
        currentEnergy: Double,
        currentVolume: Double,
        isLeaking: Boolean,
        sealed: Boolean = false
    ) {
        this.regions = regions
        this.gasMasses = gasMasses
        this.currentEnergy = currentEnergy
        this.currentVolume = currentVolume
        this.isLeaking = isLeaking
        this.sealed = sealed
    }

    @get:JsonIgnore
    internal val geometry: BalloonGeometry
        get() = geometryCache ?: BalloonGeometry(regions, sealed).also {
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

    /**
     * Upgrades an existing unsealed balloon to a sealed blimp envelope if every
     * 6-sided exterior neighbor (including [Direction.DOWN]) is a valid enclosure block.
     */
    fun upgradeToSealed(level: Level): Boolean {
        if (sealed) return true
        if (!geometry.isCompletelySealed(level)) return false
        sealed = true
        geometryCache = null
        validatedGeometry = false
        dirtyBoundary.clear()
        shouldValidate = true
        geometryVersion++
        return validate(level) == EnclosureStatus.VALID
    }

    internal fun computeKelvinNodePos(dimension: ResourceLocation): DuctNodePos? {
        val anchor = geometry.cells.minWithOrNull(compareBy<BlockPos> { it.x }.thenBy { it.y }.thenBy { it.z })
            ?: return null
        return DuctNodePos(anchor.x.toDouble(), anchor.y + 0.5, anchor.z.toDouble(), dimension)
    }

    /**
     * Ensures this sealed balloon has a live virtual [BalloonDuctNode] registered in [network].
     * Migrates any connected Blimp Port edges if the anchor position changed after a geometry update.
     */
    fun ensureKelvinNode(network: DuctNetwork<*>, dimension: ResourceLocation): DuctNodePos? {
        if (!sealed) return null
        val desiredPos = computeKelvinNodePos(dimension) ?: return null
        val previousPos = kelvinNodePos

        if (previousPos != null && previousPos != desiredPos) {
            pullFromKelvin(network)
            val existingNode = network.getNodeAt(previousPos)
            val migratedEdges = existingNode?.nodeEdges?.toList()?.mapNotNull { edge ->
                val other = when {
                    edge.nodeA == previousPos -> edge.nodeB
                    edge.nodeB == previousPos -> edge.nodeA
                    else -> null
                } ?: return@mapNotNull null
                Triple(other, edge.type, edge)
            } ?: emptyList()
            removeKelvinNode(network)
            val newNode = BalloonDuctNode(desiredPos, volume = max(1e-6, currentVolume))
            network.addNode(desiredPos, newNode)
            kelvinNodePos = desiredPos
            pushToKelvin(network)
            for ((otherPos, type, oldEdge) in migratedEdges) {
                if (network.getNodeAt(otherPos) != null) {
                    val newEdge = PipeDuctEdge(
                        type = type,
                        nodeA = otherPos,
                        nodeB = desiredPos,
                        radius = oldEdge.radius,
                        length = oldEdge.length,
                        thermalConductivityMultiplier = oldEdge.thermalConductivityMultiplier
                    ).also { it.wallThermalConductance = oldEdge.wallThermalConductance }
                    network.addEdge(otherPos, desiredPos, newEdge)
                }
            }
            return desiredPos
        }

        val existingNode = network.getNodeAt(desiredPos) as? BalloonDuctNode
        if (existingNode == null) {
            val newNode = BalloonDuctNode(desiredPos, volume = max(1e-6, currentVolume))
            network.addNode(desiredPos, newNode)
            kelvinNodePos = desiredPos
            pushToKelvin(network)
        } else {
            kelvinNodePos = desiredPos
            val vol = max(1e-6, currentVolume)
            existingNode.volume = vol
            network.nodeInfo[desiredPos]?.let { info ->
                if (info.totalVolume != vol) {
                    info.totalVolume = vol
                    NodeThermodynamics.update(existingNode, info)
                }
            }
        }
        return desiredPos
    }

    /** Copies the latest gas masses and thermal energy from the virtual Kelvin node into this balloon. */
    fun pullFromKelvin(network: DuctNetwork<*>) {
        if (!sealed) return
        val pos = kelvinNodePos ?: return
        val info = network.nodeInfo[pos] ?: return
        val newMasses = HashMap<String, Double>()
        var totalMass = 0.0
        for ((gas, mass) in info.currentGasMasses) {
            if (mass.isFinite() && mass > 0.0) {
                newMasses[gas.resourceLocation.toString()] = mass
                totalMass += mass
            }
        }
        if (totalMass < 1e-9) {
            gasMasses.clear()
            currentEnergy = 0.0
            return
        }
        val energy = info.currentEnergy
        if (energy.isFinite() && energy > 0.0) {
            gasMasses.clear()
            gasMasses.putAll(newMasses)
            currentEnergy = energy
        }
    }

    /** Pushes this balloon's authoritative gas masses, thermal energy, and volume into its virtual Kelvin node. */
    fun pushToKelvin(network: DuctNetwork<*>) {
        if (!sealed) return
        val pos = kelvinNodePos ?: return
        val node = network.getNodeAt(pos) as? BalloonDuctNode ?: return
        val info = network.nodeInfo[pos] ?: return
        val vol = max(1e-6, currentVolume)
        node.volume = vol
        info.totalVolume = vol
        info.currentGasMasses.clear()
        val resolved = resolvedMasses()
        var totalMass = 0.0
        for ((gas, mass) in resolved) {
            if (mass.isFinite() && mass > 0.0) {
                info.currentGasMasses[gas] = mass
                totalMass += mass
            }
        }
        info.currentEnergy = if (totalMass < 1e-9 || !currentEnergy.isFinite() || currentEnergy <= 0.0) {
            info.currentGasMasses.clear()
            0.0
        } else {
            currentEnergy
        }
        NodeThermodynamics.update(node, info)
    }

    /** Disconnects and removes this balloon's virtual Kelvin node from [network]. */
    fun removeKelvinNode(network: DuctNetwork<*>?) {
        val pos = kelvinNodePos ?: return
        kelvinNodePos = null
        if (network == null) return
        val node = network.getNodeAt(pos)
        if (node != null) {
            for (edge in node.nodeEdges.toList()) {
                network.removeEdge(edge.nodeA, edge.nodeB)
            }
        }
        network.nodesByChunk[KelvinChunkPos(pos.x.toInt() shr 4, pos.z.toInt() shr 4)]?.remove(pos)
        network.removeNode(pos)
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

    fun tick(level: ServerLevel, ship: LoadedServerShip, kelvin: DuctNetwork<*>? = null): Boolean {
        if (level.dimensionId != ship.chunkClaimDimension || enclosureStatus == EnclosureStatus.UNKNOWN) return false
        val y = ship.transform.positionToWorld(Vector3d(getCenter())).y
        val atmosphere = level.shipObjectWorld.aerodynamicUtils
        val density = atmosphere.getAirDensityForY(y, level.dimensionId)
        val pressure = atmosphere.getAirPressureForY(y, level.dimensionId)
        val temperature = atmosphere.getAirTemperatureForY(y, level.dimensionId)
        val config = ClockworkConfig.SERVER.balloons

        val emitLeakEffects: Boolean
        if (sealed) {
            if (kelvin != null) {
                val dimension = level.dimension()?.location()
                if (dimension != null) {
                    ensureKelvinNode(kelvin, dimension)
                }
                pullFromKelvin(kelvin)
            }
            val masses = resolvedMasses()
            if (gasMasses.values.any { !it.isFinite() || it < 0.0 } || !BalloonThermodynamics.isValidSealedState(masses, currentEnergy)) {
                gasMasses.clear()
                currentEnergy = 0.0
                if (kelvin != null) pushToKelvin(kelvin)
                return false
            }
            emitLeakEffects = isLeaking && leakPositions.isNotEmpty() && config.permeabilityConstant > 0.0 && run {
                val internalPressure = BalloonThermodynamics.pressure(masses, currentEnergy, currentVolume)
                internalPressure.isFinite() && internalPressure > 1.0
            }
            val energy = BalloonThermodynamics.stepSealed(
                masses, currentEnergy, currentVolume,
                pressure, temperature,
                config.blimpPermeabilityConstant, config.permeabilityConstant,
                config.heatTransferCoefficient, config.leakHeatTransferMultiplier,
                missingExternalPositions
            )
            if (!BalloonThermodynamics.isValidSealedState(masses, energy)) {
                gasMasses.clear()
                currentEnergy = 0.0
                if (kelvin != null) pushToKelvin(kelvin)
                return false
            }
            currentEnergy = energy
            gasMasses.clear()
            masses.forEach { (gas, mass) -> if (mass > 0.0) gasMasses[gas.resourceLocation.toString()] = mass }
            if (kelvin != null) pushToKelvin(kelvin)
        } else {
            val masses = resolvedMasses()
            val air = GasTypeRegistry.getGasType("kelvin", "air") ?: return false
            if (gasMasses.values.any { !it.isFinite() || it < 0.0 } || !BalloonThermodynamics.isValidState(masses, currentEnergy)) {
                resetGas(air, density, temperature)
                return false
            }
            // holes can stick around after the gas stops flowing out
            // check before stepping, since this tick might finish venting
            emitLeakEffects = isLeaking && leakPositions.isNotEmpty() && config.permeabilityConstant > 0.0 && run {
                val internalTemperature = currentEnergy / BalloonThermodynamics.capacity(masses)
                val moles = masses.entries.sumOf { (gas, mass) -> gas.massToMoles(mass) }
                val pressureDelta = moles * DuctNetwork.idealGasConstant * internalTemperature / currentVolume - pressure
                // ignore tiny pressure differences (0.1%, at least 1 Pa)
                pressureDelta.isFinite() && pressureDelta > max(1.0, pressure * 0.001)
            }
            val energy = BalloonThermodynamics.step(masses, currentEnergy, currentVolume, air,
                pressure, temperature, config.permeabilityConstant, config.heatTransferCoefficient,
                config.leakHeatTransferMultiplier, missingExternalPositions)
            if (!BalloonThermodynamics.isValidState(masses, energy)) {
                resetGas(air, density, temperature)
                return false
            }
            currentEnergy = energy
            gasMasses.clear()
            masses.forEach { (gas, mass) -> gasMasses[gas.resourceLocation.toString()] = mass }
        }

        if (emitLeakEffects) {
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
        return true
    }

    private fun resetGas(air: GasType, density: Double, temperature: Double) {
        val mass = max(0.0, density * currentVolume)
        val energy = temperature * mass * BalloonThermodynamics.specificCapacity(air)
        gasMasses.clear()
        currentEnergy = 0.0
        if (mass.isFinite() && energy.isFinite() && energy >= 0.0) {
            gasMasses[air.resourceLocation.toString()] = mass
            currentEnergy = energy
        }
    }

    fun makeForceData(level: ServerLevel, ship: LoadedServerShip): PhysBalloonData {
        val center = getCenter()
        val y = ship.transform.positionToWorld(Vector3d(center)).y
        val atmosphere = level.shipObjectWorld.aerodynamicUtils
        val density = atmosphere.getAirDensityForY(y, level.dimensionId)
        if (sealed) {
            val ambientPressure = atmosphere.getAirPressureForY(y, level.dimensionId)
            val masses = resolvedMasses()
            val internalPressure = BalloonThermodynamics.pressure(masses, currentEnergy, currentVolume)
            val effectiveVolume = BalloonThermodynamics.effectiveVolume(currentVolume, internalPressure, ambientPressure)
            val internalMass = masses.values.sum()
            return PhysBalloonData(Vector3d(center), max(0.0, density * effectiveVolume - internalMass), effectiveVolume)
        }
        return PhysBalloonData(Vector3d(center), max(0.0, density * currentVolume - gasMasses.values.sum()), currentVolume)
    }

    /** Hot gas can already be at ambient pressure; sealed blimps vent toward empty. */
    fun isNearlyAtmospheric(level: ServerLevel): Boolean {
        val center = getCenter()
        val ship = level.getLoadedShipManagingPos(BlockPos.containing(center.x(), center.y(), center.z())) ?: return false
        val y = ship.transform.positionToWorld(Vector3d(center)).y
        val atmosphere = level.shipObjectWorld.aerodynamicUtils
        val masses = resolvedMasses()
        val capacity = BalloonThermodynamics.capacity(masses)
        if (capacity <= 1e-9) return true
        if (sealed) {
            val ambientPressure = atmosphere.getAirPressureForY(y, level.dimensionId)
            val internalPressure = BalloonThermodynamics.pressure(masses, currentEnergy, currentVolume)
            return masses.values.sum() <= 1e-6 || internalPressure <= max(1.0, ambientPressure * 0.001)
        }
        val ambientMass = atmosphere.getAirDensityForY(y, level.dimensionId) * currentVolume
        val ambientTemperature = atmosphere.getAirTemperatureForY(y, level.dimensionId)
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
    fun mergeWith(other: BalloonData, level: Level, kelvin: DuctNetwork<*>? = null): EnclosureStatus {
        if (other === this) return enclosureStatus
        if (kelvin != null) {
            pullFromKelvin(kelvin)
            other.pullFromKelvin(kelvin)
            other.removeKelvinNode(kelvin)
        }
        val wasSealed = sealed
        sealed = sealed && other.sealed
        if (wasSealed && !sealed) {
            removeKelvinNode(kelvin)
        }
        val cells = HashSet(geometry.cells).apply { addAll(other.geometry.cells) }
        updateRegionsNoValidation(BalloonGeometry.regions(cells), level)
        for ((gas, mass) in other.gasMasses) gasMasses[gas] = gasMasses.getOrDefault(gas, 0.0) + mass
        currentEnergy += other.currentEnergy
        if (sealed && kelvin != null) {
            level.dimension()?.location()?.let { ensureKelvinNode(kelvin, it) }
            pushToKelvin(kelvin)
        }
        return EnclosureStatus.UNKNOWN
    }

    fun trySplit(level: Level, kelvin: DuctNetwork<*>? = null): Pair<Boolean, ArrayList<BalloonData>> {
        val components = geometry.airComponents(level) ?: return false to arrayListOf()
        if (components.isEmpty()) {
            shouldRemove = true
            removeKelvinNode(kelvin)
            return true to arrayListOf()
        }
        if (components.size == 1 && components[0].size == geometry.cells.size) return false to arrayListOf()
        if (kelvin != null) pullFromKelvin(kelvin)
        val masses = HashMap(gasMasses)
        val energy = currentEnergy
        val remainingVolume = components.sumOf { it.size }.toDouble()
        val additional = arrayListOf<BalloonData>()
        for ((index, component) in components.withIndex()) {
            val fraction = component.size / remainingVolume
            val target = if (index == 0) this else BalloonData(arrayListOf(), hashMapOf(), 0.0, 0.0, false, sealed)
            target.updateRegionsNoValidation(BalloonGeometry.regions(component), level)
            target.gasMasses.clear()
            masses.forEach { (gas, mass) -> target.gasMasses[gas] = mass * fraction }
            target.currentEnergy = energy * fraction
            target.shouldRemove = target.validate(level) == EnclosureStatus.INVALID
            if (index == 0) {
                if (target.shouldRemove) {
                    target.removeKelvinNode(kelvin)
                } else if (target.sealed && kelvin != null) {
                    level.dimension()?.location()?.let { target.ensureKelvinNode(kelvin, it) }
                    target.pushToKelvin(kelvin)
                }
            } else if (!target.shouldRemove) {
                additional.add(target)
            }
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
