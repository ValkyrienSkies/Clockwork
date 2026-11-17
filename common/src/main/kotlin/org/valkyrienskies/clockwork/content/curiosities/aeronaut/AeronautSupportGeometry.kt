package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.core.Direction
import net.minecraft.world.level.levelgen.structure.BoundingBox
import org.joml.Vector3d
import org.joml.Vector3dc

internal object AeronautSupportGeometry {
    private val neighbors = Direction.entries.associateWith { face -> Direction.entries.filter { it.axis != face.axis } }

    data class FaceLoad(val center: Vector3dc, val load: Double)

    fun connectedFaces(cells: List<AeronautContactTelemetry.Cell>, maxFaceChecks: Int,
                       canBridge: (AeronautContactTelemetry.Face) -> Boolean): List<FaceLoad> {
        val groups = ArrayList<FaceLoad>()
        var checks = maxFaceChecks
        val planes = cells.groupBy { it.face.direction to it.face.plane }
        for (plane in planes.values) {
            val remaining = plane.groupBy { it.face }.toMutableMap()
            val bounds = BoundingBox.encapsulatingPositions(remaining.keys.map { it.block }).orElseThrow()
            val checked = HashSet<AeronautContactTelemetry.Face>()
            while (remaining.isNotEmpty()) {
                val first = remaining.keys.first()
                val moment = Vector3d()
                var load = 0.0
                val queue = ArrayDeque<AeronautContactTelemetry.Face>()
                queue.add(first)
                checked.add(first)
                while (queue.isNotEmpty()) {
                    val face = queue.removeFirst()
                    remaining.remove(face)?.forEach { cell ->
                        moment.fma(cell.normalLoad, cell.center)
                        load += cell.normalLoad
                    }
                    if (remaining.isEmpty()) break
                    for (side in neighbors.getValue(face.direction)) {
                        val block = face.block.relative(side)
                        if (!bounds.isInside(block)) continue
                        val next = face.copy(block = block)
                        if (!checked.add(next)) continue
                        if (next in remaining) queue.add(next)
                        else if (checks > 0) {
                            checks--
                            if (canBridge(next)) queue.add(next)
                        }
                    }
                }
                groups.add(FaceLoad(moment.div(load), load))
            }
        }
        return groups
    }

    fun outline(contacts: List<Vector3dc>, height: Double): List<Vector3dc> {
        if (!height.isFinite()) return emptyList()
        val points = contacts.filter { it.isFinite }.map { Vector3d(it.x(), height, it.z()) }.distinct()
            .sortedWith(compareBy({ it.x }, { it.z }))
        if (points.size < 3) return points

        fun edge(sorted: List<Vector3d>): List<Vector3d> {
            val hull = ArrayList<Vector3d>()
            for (p in sorted) {
                while (hull.size >= 2 && cross(hull[hull.size - 2], hull.last(), p) <= 1e-10) hull.removeAt(hull.lastIndex)
                hull.add(p)
            }
            return hull.dropLast(1)
        }
        return edge(points) + edge(points.asReversed())
    }

    fun contains(outline: List<Vector3dc>, point: Vector3dc): Boolean = outline.size >= 3 &&
        outline.indices.all { i -> cross(outline[i], outline[(i + 1) % outline.size], point) >= -1e-8 }

    private fun cross(a: Vector3dc, b: Vector3dc, p: Vector3dc): Double =
        (b.x() - a.x()) * (p.z() - a.z()) - (b.z() - a.z()) * (p.x() - a.x())
}
