package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.joml.primitives.AABBi
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.util.AerodynamicUtils.DRAG_COEFFICIENT
import org.valkyrienskies.clockwork.util.AerodynamicUtils.getAirDensityForY
import org.valkyrienskies.clockwork.util.SideProfileTracker
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.util.expand
import org.valkyrienskies.mod.common.util.settings
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.util.logger
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashMap

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class DragController(var dimensionId: DimensionId) : ShipForcesInducer {

    @JsonIgnore
    private val blockUpdateQueue = ConcurrentLinkedQueue<Pair<Vector3ic, Boolean>>()
    @JsonIgnore
    private val aabbUpdateQueue = ConcurrentLinkedQueue<AABBic>()

    private val allBlocks = HashSet<Vector3ic>()
    private val exposedFaces : EnumMap<Direction, HashSet<Vector3ic>> = EnumMap(Direction::class.java)
    private val surfaceAreaByDirection = EnumMap<Direction, Double>(Direction::class.java)

    @JsonIgnore
    private val sideTracker: SideProfileTracker = SideProfileTracker()

    private var bounds: AABBic? = null

    @JsonIgnore
    private var shouldUpdate: Boolean = true
    @JsonIgnore
    private var firstTimeUpdate: Boolean = true
    @JsonIgnore
    private var max_height: Double = 563.0

    @JsonIgnore
    private var lodDetail: Int = 5

    override fun applyForces(physShip: PhysShip) {
        val impl = physShip
        if (blockUpdateQueue.isNotEmpty()) {
            val posair = blockUpdateQueue.poll()
            if (!posair.second) {
                allBlocks.add(posair.first)
                //sideTracker.add(posair.first.x(), posair.first.y(), posair.first.z())
            } else {
                allBlocks.remove(posair.first)
                //sideTracker.remove(posair.first.x(), posair.first.y(), posair.first.z())
            }
            shouldUpdate = true
        }

        if (aabbUpdateQueue.isNotEmpty()) {
            bounds = aabbUpdateQueue.poll()
        }

        if (shouldUpdate && allBlocks.isNotEmpty() && bounds != null) {
            updateExposedFaces()
        }

        //if (sideTracker.xArea != 0 && sideTracker.yArea != 0 && sideTracker.zArea != 0) {
        if (allBlocks.isNotEmpty() && exposedFaces.isNotEmpty() && bounds != null) {
            val drag = calculateDrag(impl)
            val dragPos = calculateDragPosition(impl)

            val rotDrag = calculateRotationalDrag(impl)

            if (drag.isFinite && dragPos.isFinite) {
                physShip.applyInvariantForceToPos(drag, dragPos)
            }
            for (rot in rotDrag) {
                if (rot.value.isFinite) {
                    physShip.applyInvariantForceToPos(rot.value, rot.key)
                }
            }
        }
    }

    fun gameTick(ship: ServerShip, slevel: ServerLevel) {
        if (ship.shipAABB != null && ship.shipAABB != bounds) {
            aabbUpdateQueue.add(ship.shipAABB)
        }

        if (firstTimeUpdate) {
            if (ship.shipAABB != null) {
                val vecSet: HashSet<Pair<Vector3ic, Boolean>> = HashSet()
                for (x in ship.shipAABB!!.minX() .. ship.shipAABB!!.maxX()) {
                    for (y in ship.shipAABB!!.minY() .. ship.shipAABB!!.maxY()) {
                        for (z in ship.shipAABB!!.minZ() .. ship.shipAABB!!.maxZ()) {
                            if (slevel.getBlockState(Vector3i(x, y, z).toBlockPos()).isAir) continue
                            vecSet.add(Vector3i(x, y, z) to false)
                        }
                    }
                }
                if (vecSet.isNotEmpty()) {
                    blockUpdateQueue.addAll(vecSet)
                    firstTimeUpdate = false
                }
            }
        }
    }

    fun pushUpdate(pos: Vector3ic, air: Boolean) {
        blockUpdateQueue.add(pos to air)
    }

    private fun updateExposedFaces() {
        exposedFaces.clear()
        bounds != null || return
        val foundEdges = HashMap<Direction, HashSet<Vector3ic>>()

        //NOTE TO SELF: REMEMBER TO USE OPPOSITE DIR
        for (dir in Direction.values()) {
            val actualBounds = bounds!!.expand(1, AABBi())
            val targetXMin = actualBounds.minX()
            val targetYMin = actualBounds.minY()
            val targetZMin = actualBounds.minZ()
            val targetXMax = actualBounds.maxX()
            val targetYMax = actualBounds.maxY()
            val targetZMax = actualBounds.maxZ()

            val foundEdgesForDir = HashSet<Vector3ic>()

            when (dir) {
                Direction.NORTH -> {
                    for (x in targetXMin .. targetXMax) {
                        for (y in targetYMin .. targetYMax) {
                            for (z in targetZMin .. targetZMax) {
                                val pos = Vector3i(x, y, z)
                                if (allBlocks.contains(pos)) {
                                    foundEdgesForDir.add(pos)
                                    break
                                }
                            }
                        }
                    }
                }
                Direction.SOUTH -> {
                    for (x in targetXMin .. targetXMax) {
                        for (y in targetYMin .. targetYMax) {
                            for (z in targetZMax downTo targetZMin) {
                                val pos = Vector3i(x, y, z)
                                if (allBlocks.contains(pos)) {
                                    foundEdgesForDir.add(pos)
                                    break
                                }
                            }
                        }
                    }
                }
                Direction.EAST -> {
                    for (z in targetZMin .. targetZMax ) {
                        for (y in targetYMin .. targetYMax) {
                            for (x in targetXMax downTo targetXMin) {
                                val pos = Vector3i(x, y, z)
                                if (allBlocks.contains(pos)) {
                                    foundEdgesForDir.add(pos)
                                    break
                                }
                            }
                        }
                    }
                }
                Direction.WEST -> {
                    for (z in targetZMin .. targetZMax ) {
                        for (y in targetYMin .. targetYMax) {
                            for (x in targetXMin .. targetXMax) {
                                val pos = Vector3i(x, y, z)
                                if (allBlocks.contains(pos)) {
                                    foundEdgesForDir.add(pos)
                                    break
                                }
                            }
                        }
                    }
                }
                Direction.UP -> {
                    for (x in targetXMin .. targetXMax) {
                        for (z in targetZMin .. targetZMax) {
                            for (y in targetYMax downTo targetYMin) {
                                val pos = Vector3i(x, y, z)
                                if (allBlocks.contains(pos)) {
                                    foundEdgesForDir.add(pos)
                                    break
                                }
                            }
                        }
                    }
                }
                Direction.DOWN -> {
                    for (x in targetXMin .. targetXMax) {
                        for (z in targetZMin .. targetZMax) {
                            for (y in targetYMin .. targetYMax) {
                                val pos = Vector3i(x, y, z)
                                if (allBlocks.contains(pos)) {
                                    foundEdgesForDir.add(pos)
                                    break
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
            foundEdges[dir] = foundEdgesForDir
        }

        exposedFaces.putAll(foundEdges)

        surfaceAreaByDirection[Direction.NORTH] = foundEdges[Direction.NORTH]?.size?.toDouble()?: 0.0
        surfaceAreaByDirection[Direction.SOUTH] = foundEdges[Direction.SOUTH]?.size?.toDouble()?: 0.0
        surfaceAreaByDirection[Direction.EAST] = foundEdges[Direction.EAST]?.size?.toDouble()?: 0.0
        surfaceAreaByDirection[Direction.WEST] = foundEdges[Direction.WEST]?.size?.toDouble()?: 0.0
        surfaceAreaByDirection[Direction.UP] = foundEdges[Direction.UP]?.size?.toDouble()?: 0.0
        surfaceAreaByDirection[Direction.DOWN] = foundEdges[Direction.DOWN]?.size?.toDouble()?: 0.0

        shouldUpdate = false
    }

    private fun calculateDrag(ship: PhysShip): Vector3dc {
        val motionVector: Vector3dc = ship.velocity
        val motionNormal: Vector3dc = motionVector.normalize(Vector3d()).mul(-1.0)

        val density = getAirDensityForY(ship.transform.positionInWorld.y(), dimensionId)

        var exposedArea = 0.0

        for (dir in Direction.values()) {
            val surfaceArea = surfaceAreaByDirection[dir]?: continue
//            val surfaceArea = when (dir.axis) {
//                Direction.Axis.X -> sideTracker.xArea
//                Direction.Axis.Y -> sideTracker.yArea
//                Direction.Axis.Z -> sideTracker.zArea
//                else -> continue
//            }
            val dot = motionNormal.dot(dir.normal.toJOMLD())
            if (dot > 0) {
                exposedArea += surfaceArea * dot
            }
        }

        val dragForce = DRAG_COEFFICIENT * density * (motionVector.lengthSquared()/2.0) * exposedArea


        return motionNormal.mul(dragForce, Vector3d())
    }

    private fun calculateRotationalDrag(ship: PhysShip): Map<Vector3dc, Vector3dc> {
        val rotationVector: Vector3dc = ship.omega

        val density = getAirDensityForY(ship.transform.positionInWorld.y(), dimensionId)

        val totalDragForce: HashMap<Vector3dc, Vector3dc> = HashMap()
        val centersOfPressure: EnumMap<Direction, Vector3d> = EnumMap(net.minecraft.core.Direction::class.java)

        for ((dir, faces) in exposedFaces.entries) {
            if (faces.isEmpty()) continue
            val centerOfPressure: Vector3d = Vector3d()
            faces.forEach {
                centerOfPressure.add(it.x().toDouble(), it.y().toDouble(), it.z().toDouble()).add(0.5, 0.5, 0.5)
                    .add(dir.normal.toJOMLD().mul(0.5, Vector3d()))
            }
            centerOfPressure.div(faces.size.toDouble())
            centersOfPressure[dir] = centerOfPressure
        }

        for (dir in Direction.values()) {
            val centerOfPressure = centersOfPressure[dir]?: continue
            val distanceToCenter = centerOfPressure.sub(ship.transform.positionInShip)
            val velocityVector : Vector3dc = rotationVector.cross(distanceToCenter, Vector3d())
            val dot = velocityVector.normalize(Vector3d()).dot(dir.normal.toJOMLD())
            if (dot < 0) continue
            val dragForce = DRAG_COEFFICIENT * density * (velocityVector.lengthSquared()/2.0) * (surfaceAreaByDirection[dir]!! * dot)
            totalDragForce[centerOfPressure] = velocityVector.normalize(Vector3d()).mul(-1.0).mul(dragForce, Vector3d())
        }

        return totalDragForce
    }

    private fun calculateDragPosition(ship: PhysShip): Vector3dc {

        val motionVector: Vector3dc = ship.velocity
        val motionNormal: Vector3dc = motionVector.normalize(Vector3d()).mul(-1.0)

        val avgCenterOfPressure = Vector3d()
        var sumOfWeights = 0.0
        for ((dir, faces) in exposedFaces.entries) {
            if (faces.isEmpty()) continue
            val centerOfPressure = Vector3d()
            faces.forEach {
                centerOfPressure.add(it.x().toDouble(), it.y().toDouble(), it.z().toDouble()).add(0.5, 0.5, 0.5).add(dir.normal.toJOMLD().mul(0.5, Vector3d()))
            }
            centerOfPressure.div(faces.size.toDouble())
            val dot = motionNormal.dot(dir.normal.toJOMLD())
            if (dot > 0.0) {
                centerOfPressure.mul(dot)
                avgCenterOfPressure.add(centerOfPressure)
                sumOfWeights += dot
            }
        }

        val realAvg: Vector3dc = if (sumOfWeights != 0.0) avgCenterOfPressure.div(sumOfWeights) else avgCenterOfPressure

        //return ship.transform.shipToWorld.transformPosition(realAvg, Vector3d())

        return realAvg.sub(ship.transform.positionInShip, Vector3d())
    }

    companion object {
        fun getOrCreate(ship: ServerShip): DragController? {
            if (ship.getAttachment(DragController::class.java) == null) {
                ship.saveAttachment(DragController::class.java, DragController(ship.chunkClaimDimension))
            }
            return ship.getAttachment(DragController::class.java)
        }

        private val dragLogger by logger("Drag Controller")
    }
}