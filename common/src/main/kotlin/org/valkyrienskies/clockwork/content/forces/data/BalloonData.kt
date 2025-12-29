package org.valkyrienskies.clockwork.content.forces.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandItem
import org.valkyrienskies.clockwork.content.forces.BalloonController.Companion.isValidBalloonEnclosure
import org.valkyrienskies.clockwork.util.AABBHelper.mergeAdjacentFast
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.kelvin.api.GasType

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
    var shouldRemove = false

    @JsonIgnore
    var shouldReScan = false

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
        center.x = sumX / count.toDouble() + 0.5
        center.y = sumY / count.toDouble() + 0.5
        center.z = sumZ / count.toDouble() + 0.5
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
        return gasMasses.values.sum() > 1e-9 && missingExternalPositions < externalSize / 4
    }

    fun canLeakGassesOnly(): Boolean {
        return gasMasses.values.sum() > 1e-9
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
        missingExternalPositions = 0
        for (pos in externals) {
            if (!level.isLoaded(pos)) {
                return EnclosureStatus.UNKNOWN
            }
            val blockState = level.getBlockState(pos)
            if (!blockState.isValidBalloonEnclosure(level, pos)) {
                missingExternalPositions ++
                currentStatus = if (canLeak(externals.size)) {
                    EnclosureStatus.LEAKING
                } else {
                    EnclosureStatus.INVALID
                }
            }
        }
        return currentStatus
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
