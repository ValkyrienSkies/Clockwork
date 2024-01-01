package org.valkyrienskies.clockwork.content.curiosities.tools.designator

import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3ic
import org.joml.primitives.AABBd
import org.joml.primitives.AABBic
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import kotlin.math.roundToInt

/**
 * A storage class for things (mostly the auric designator) that need to keep track of clustered selections.
 */
public class SelectedAreaToolkit {
    /**
     * A set of all selected areas (represented by [AABB]s), irrelevant of cluster.
     */
    var selectedAreas: HashSet<AABB> = HashSet()

    /**
     * A set of all individual clusters, which are represented by a set of [AABB]s.
     */
    public var selectionClusters: HashSet<Set<AABB>> = HashSet()

    var blockClusters: HashSet<Set<BlockPos>> = HashSet()

    @JsonIgnore
    private val toBeRemoved = ArrayList<Set<AABB>>()

    @JsonIgnore
    var toStopRendering = ArrayList<Set<AABB>>()

    // AREA : ADD

    fun clusterNewArea(initial: AABB) {
        val newCluster: MutableSet<AABB> = HashSet()
        newCluster.add(initial)
        val makeNewCluster = true

        if (makeNewCluster) {
            this.selectionClusters.add(newCluster)
            this.mergeClusters(initial)
        }
    }

    // AREA : CLUSTER

    private fun massClusterAreas(areas: Set<AABB>) {
        for (box in areas) {
            this.mergeClusters(box)
        }
    }

    private fun mergeClusters(starter: AABB) {
        val newCluster: MutableSet<AABB> = HashSet()

        // get direct neighbors
        for (area in this.selectedAreas) {
            if (starter.intersects(area)) {
                newCluster.add(area)
            }
        }
        // spiral out of control
        val toCheck = ArrayList(newCluster)
        while (!toCheck.isEmpty()) {
            val check = toCheck[0]
            for (area in this.selectedAreas) {
                if (check!!.intersects(area) && !newCluster.contains(area) && !toCheck.contains(area)) {
                    newCluster.add(area)
                    toCheck.add(area)
                }
            }
            toCheck.removeAt(0)
        }
        // finish off the insanity by adding back the initial
        newCluster.add(starter)

        // now check to dump all clusters that were merged
        for (check in newCluster) {
            val oldCluster = getClusterContainingAABB(check)
            oldCluster?.let { dumpClusterDirty(it) }
        }
        this.selectionClusters.add(newCluster)
    }

    // AREA : REMOVE

    fun dumpCluster(cluster: Set<AABB>) {
        this.selectionClusters.remove(cluster)
        this.toBeRemoved.add(cluster)
        this.toStopRendering.add(cluster)
    }

    fun dumpClusterDirty(cluster: Set<AABB>) {
        this.selectionClusters.remove(cluster)
        this.toStopRendering.add(cluster)
    }

    // AREA : UTIL

    fun containsAABB(aabb: AABB): Boolean {
        return this.selectedAreas.contains(aabb)
    }

    fun containsCluster(cluster: Set<AABB>): Boolean {
        return this.selectionClusters.contains(cluster)
    }

    fun getClosestCluster(pos: Vector3ic): Set<AABB?> {
        val posInVec: Vec3i = pos.toBlockPos()
        val posAsVec3: Vec3 = Vec3.atCenterOf(posInVec)
        var returnCluster: Set<AABB?> = HashSet()
        var closestDistance = Double.MAX_VALUE
        for (cluster in this.selectionClusters) {
            for (area in cluster) {
                if (area!!.contains(posAsVec3)) {
                    return cluster
                } else {
                    val center: Vector3dc = area.center.toJOML()
                    val distance = center.distance(pos.x().toDouble(), pos.y().toDouble(), pos.z().toDouble())
                    if (distance < closestDistance) {
                        closestDistance = distance
                        returnCluster = cluster
                    }
                }
            }
        }
        return returnCluster
    }

    fun getClusterContaining(pos: Vector3ic?): Set<AABB>? {
        for (cluster in this.selectionClusters) {
            for (area in cluster) {
                if (area!!.contains(Vec3.atCenterOf(pos!!.toBlockPos()))) {
                    return cluster
                }
            }
        }
        return null
    }

    fun getClusterContainingAABB(box: AABB?): Set<AABB>? {
        for (cluster in this.selectionClusters) {
            if (cluster.contains(box)) {
                return cluster
            }
        }
        return null
    }

    fun getAABBFromPos(pos: Vector3ic): AABB? {
        for (area in this.selectedAreas) {
            if (area.contains(Vec3.atCenterOf(pos.toBlockPos()))) {
                return area
            }
        }
        return null
    }

    fun overwriteFrom(sat: SelectedAreaToolkit) {
        this.selectedAreas = sat.selectedAreas
        this.selectionClusters = sat.selectionClusters
    }

    fun overwriteFrom(ssat: SerializableSelectedAreaToolkit) {
        overwriteFrom(toDeserialize(ssat))
    }

    companion object {
        fun denseBlocksFromCluster(cluster: Set<AABB>): DenseBlockPosSet {
            val set = DenseBlockPosSet()
            for (area in cluster) {
                val minX = area.minX.toInt()
                val maxX = area.maxX.roundToInt()
                val minY = area.minY.toInt()
                val maxY = area.maxY.roundToInt()
                val minZ = area.minZ.toInt()
                val maxZ = area.maxZ.roundToInt()

                for (x in minX..maxX) {
                    for (y in minY..maxY) {
                        for (z in minZ..maxZ) {
                            set.add(x, y, z)
                        }
                    }
                }
            }
            return set
        }

        fun denseBlocksFromCluster(cluster: Set<AABB>, level: Level): DenseBlockPosSet {
            val set = DenseBlockPosSet()
            for (area in cluster) {
                val minX = area.minX.toInt()
                val maxX = area.maxX.roundToInt()
                val minY = area.minY.toInt()
                val maxY = area.maxY.roundToInt()
                val minZ = area.minZ.toInt()
                val maxZ = area.maxZ.roundToInt()

                for (x in minX..maxX) {
                    for (y in minY..maxY) {
                        for (z in minZ..maxZ) {
                            if (!level.getBlockState(BlockPos(x, y, z)).isAir) {
                                set.add(x, y, z)
                            }
                        }
                    }
                }
            }
            return set
        }

        fun blocksFromCluster(cluster: Set<AABB>): Set<BlockPos> {
            val set: MutableSet<BlockPos> = HashSet()
            for (area in cluster) {
                val minX = area.minX.toInt()
                val maxX = area.maxX.roundToInt()
                val minY = area.minY.toInt()
                val maxY = area.maxY.roundToInt()
                val minZ = area.minZ.toInt()
                val maxZ = area.maxZ.roundToInt()

                for (x in minX..maxX) {
                    for (y in minY..maxY) {
                        for (z in minZ..maxZ) {
                            set.add(BlockPos(x, y, z))
                        }
                    }
                }
            }
            return set
        }

        fun blocksFromCluster(cluster: Set<AABB>, level: Level): Set<BlockPos> {
            val set: MutableSet<BlockPos> = HashSet()
            for (area in cluster) {
                val minX = area.minX.toInt()
                val maxX = area.maxX.roundToInt()
                val minY = area.minY.toInt()
                val maxY = area.maxY.roundToInt()
                val minZ = area.minZ.toInt()
                val maxZ = area.maxZ.roundToInt()

                for (x in minX..maxX) {
                    for (y in minY..maxY) {
                        for (z in minZ..maxZ) {
                            if (!level.getBlockState(BlockPos(x, y, z)).isAir) {
                                set.add(BlockPos(x, y, z))
                            }
                        }
                    }
                }
            }
            return set
        }

        fun entitiesFromCluster(cluster: Set<AABB>, level: ServerLevel): Set<Entity> {
            val set: MutableSet<Entity> = HashSet()
            for (area in cluster) {
                set.addAll(level.getEntities(null, area))
            }
            return set
        }

        fun toSerialize(tk: SelectedAreaToolkit): SerializableSelectedAreaToolkit {
            val serializedAreas: HashSet<AABBd> = HashSet()
            for (area in tk.selectedAreas) {
                serializedAreas.add(AABBd(area.toJOML()))
            }
            val serializedClusters: HashSet<Set<AABBd>> = HashSet()
            for (cluster in tk.selectionClusters) {
                val serializedCluster: MutableSet<AABBd> = HashSet()
                for (area in cluster) {
                    serializedCluster.add(AABBd(area.toJOML()))
                }
                serializedClusters.add(serializedCluster)
            }
            return SerializableSelectedAreaToolkit(serializedAreas, serializedClusters)
        }
        fun toDeserialize(stk: SerializableSelectedAreaToolkit): SelectedAreaToolkit {
            val deserializedAreas: HashSet<AABB> = HashSet()
            for (area in stk.selectedAreas) {
                deserializedAreas.add(area.toMinecraft())
            }
            val deserializedClusters: HashSet<Set<AABB>> = HashSet()
            for (cluster in stk.selectionClusters) {
                val deserializedCluster: MutableSet<AABB> = HashSet()
                for (area in cluster) {
                    deserializedCluster.add(area.toMinecraft())
                }
                deserializedClusters.add(deserializedCluster)
            }
            val sat = SelectedAreaToolkit()
            sat.selectedAreas = deserializedAreas
            sat.selectionClusters = deserializedClusters
            return sat
        }
    }
}