package org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3ic
import org.joml.primitives.AABBic
import org.valkyrienskies.core.impl.datastructures.DenseBlockPosSet
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

/**
 * A storage class for things (mostly the auric designator) that need to keep track of clustered selections.
 */
class SelectedAreaToolkit {
    /**
     * A set of all selected areas (represented by [AABBic]s), irrelevant of cluster.
     */
    var selectedAreas: HashSet<AABBic> = HashSet()

    /**
     * A set of all individual clusters, which are represented by a set of [AABBic]s.
     */
    public var selectionClusters: HashSet<Set<AABBic>> = HashSet()

    private val toBeRemoved = ArrayList<Set<AABBic>>()
    var toStopRendering = ArrayList<Set<AABBic>>()

    // AREA : ADD

    fun clusterNewArea(initial: AABBic) {
        val newCluster: MutableSet<AABBic> = HashSet()
        newCluster.add(initial)
        val makeNewCluster = true

        if (makeNewCluster) {
            this.selectionClusters.add(newCluster)
            this.mergeClusters(initial)
        }
    }

    // AREA : CLUSTER

    private fun massClusterAreas(areas: Set<AABBic>) {
        for (box in areas) {
            this.mergeClusters(box)
        }
    }

    private fun mergeClusters(starter: AABBic) {
        val newCluster: MutableSet<AABBic> = HashSet()

        //get direct neighbors
        for (area in this.selectedAreas) {
            if (starter.intersectsAABB(area)) {
                newCluster.add(area)
            }
        }
        //spiral out of control
        val toCheck = ArrayList(newCluster)
        while (!toCheck.isEmpty()) {
            val check = toCheck[0]
            for (area in this.selectedAreas) {
                if (check!!.intersectsAABB(area) && !newCluster.contains(area) && !toCheck.contains(area)) {
                    newCluster.add(area)
                    toCheck.add(area)
                }
            }
            toCheck.removeAt(0)
        }
        //finish off the insanity by adding back the initial
        newCluster.add(starter)

        //now check to dump all clusters that were merged
        for (check in newCluster) {
            val oldCluster = getClusterContainingAABB(check)
            oldCluster?.let { dumpClusterDirty(it) }
        }
        this.selectionClusters.add(newCluster)
    }


    // AREA : REMOVE

    fun dumpCluster(cluster: Set<AABBic>) {
        this.selectionClusters.remove(cluster)
        this.toBeRemoved.add(cluster)
        this.toStopRendering.add(cluster)
    }

    fun dumpClusterDirty(cluster: Set<AABBic>) {
        this.selectionClusters.remove(cluster)
        this.toStopRendering.add(cluster)
    }

    // AREA : UTIL

    fun containsAABB(aabb: AABBic): Boolean {
        return this.selectedAreas.contains(aabb)
    }

    fun containsCluster(cluster: Set<AABBic>): Boolean {
        return this.selectionClusters.contains(cluster)
    }

    fun getClosestCluster(pos: Vector3ic): Set<AABBic?> {
        var returnCluster: Set<AABBic?> = HashSet()
        var closestDistance = Double.MAX_VALUE
        for (cluster in this.selectionClusters) {
            for (area in cluster) {
                if (area!!.containsPoint(pos)) {
                    return cluster
                } else {
                    val center: Vector3dc = area.center(Vector3d())
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

    fun getClusterContaining(pos: Vector3ic?): Set<AABBic>? {
        for (cluster in this.selectionClusters) {
            for (area in cluster) {
                if (area!!.containsPoint(pos)) {
                    return cluster
                }
            }
        }
        return null
    }

    fun getClusterContainingAABB(box: AABBic?): Set<AABBic>? {
        for (cluster in this.selectionClusters) {
            if (cluster.contains(box)) {
                return cluster
            }
        }
        return null
    }

    fun getAABBFromPos(pos: Vector3ic): AABBic? {
        for (area in this.selectedAreas) {
            if (area.containsPoint(pos)) {
                return area
            }
        }
        return null
    }

    fun overwriteFrom(sat: SelectedAreaToolkit) {
        this.selectedAreas = sat.selectedAreas
        this.selectionClusters = sat.selectionClusters
    }

    companion object {
        fun denseBlocksFromCluster(cluster: Set<AABBic>): DenseBlockPosSet {
            val set = DenseBlockPosSet()
            for (area in cluster) {
                for (x in area.minX()..area.maxX()) {
                    for (y in area.minY()..area.maxY()) {
                        for (z in area.minZ()..area.maxZ()) {
                            set.add(x, y, z)
                        }
                    }
                }
            }
            return set
        }

        fun blocksFromCluster(cluster: Set<AABBic>): Set<BlockPos> {
            val set: MutableSet<BlockPos> = HashSet()
            for (area in cluster) {
                for (x in area.minX()..area.maxX()) {
                    for (y in area.minY()..area.maxY()) {
                        for (z in area.minZ()..area.maxZ()) {
                            set.add(BlockPos(x, y, z))
                        }
                    }
                }
            }
            return set
        }

        fun entitiesFromCluster(cluster: Set<AABBic>, level: ServerLevel): Set<Entity> {
            val set: MutableSet<Entity> = HashSet()
            for (area in cluster) {
                val box = AABB(
                    area.maxX().toDouble(),
                    area.maxY().toDouble(),
                    area.maxZ().toDouble(),
                    area.minX().toDouble(),
                    area.minY().toDouble(),
                    area.minZ().toDouble()
                )
                set.addAll(level.getEntities(null, box))
            }
            return set
        }
    }
}