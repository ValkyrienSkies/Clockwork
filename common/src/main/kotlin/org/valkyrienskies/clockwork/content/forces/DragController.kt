package org.valkyrienskies.clockwork.content.forces

import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import org.joml.Vector2ic
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.joml.primitives.AABBi
import org.joml.primitives.AABBic
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.core.util.expand
import org.valkyrienskies.core.util.y
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.util.logger
import java.util.EnumMap
import java.util.concurrent.ConcurrentLinkedQueue

class DragController : ShipForcesInducer {

    private val blockUpdateQueue = ConcurrentLinkedQueue<Pair<Vector3ic, Boolean>>()
    private val aabbUpdateQueue = ConcurrentLinkedQueue<AABBic>()

    private val allBlocks = HashSet<Vector3ic>()
    private val exposedFaces : EnumMap<Direction, HashSet<Vector3ic>> = EnumMap(Direction::class.java)

    private val surfaceAreaByDirection: EnumMap<Direction, Double> = EnumMap(Direction::class.java)

    private var bounds: AABBic? = null

    private var shouldUpdate: Boolean = true

    private var firstTimeUpdate: Boolean = true

    private var max_height: Double = 563.0

    override fun applyForces(physShip: PhysShip) {
        val impl = physShip as PhysShipImpl
        if (blockUpdateQueue.isNotEmpty()) {
            val posair = blockUpdateQueue.poll()
            if (!posair.second) {
                allBlocks.add(posair.first)
            } else {
                allBlocks.remove(posair.first)
            }
            shouldUpdate = true
        }

        if (aabbUpdateQueue.isNotEmpty()) {
            bounds = aabbUpdateQueue.poll()
        }

        if (shouldUpdate && allBlocks.isNotEmpty() && bounds != null) {
            updateExposedFaces()
        }

        if (exposedFaces.isNotEmpty() && surfaceAreaByDirection.isNotEmpty()) {
            val drag = calculateDrag(impl)
            val dragPos = calculateDragPosition(impl)

            if (drag.isFinite && dragPos.isFinite) {
                physShip.applyInvariantForceToPos(drag, dragPos)
            }
            physShip.applyInvariantTorque(calculateRotationalDrag()) //does nothing rn lol
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
        for (dir in Direction.values()) {
            val surfaceArea = exposedFaces[dir]!!.size.toDouble()
            surfaceAreaByDirection[dir] = surfaceArea
        }

        shouldUpdate = false
    }

    private fun calculateDrag(ship: PhysShipImpl): Vector3dc {
        val motionVector: Vector3dc = ship.poseVel.vel
        val motionNormal: Vector3dc = motionVector.normalize(Vector3d()).mul(-1.0)

        val density = getAirDensityForY(ship.poseVel.pos.y())

        var exposedArea = 0.0

        for (dir in Direction.values()) {
            val surfaceArea = surfaceAreaByDirection[dir]?: continue
            val dot = motionNormal.dot(dir.normal.toJOMLD())
            if (dot > 0) {
                exposedArea += surfaceArea * dot
            }
        }

        val dragForce = DRAG_COEFFICIENT * density * (motionVector.lengthSquared()/2.0) * exposedArea

        return motionNormal.mul(dragForce, Vector3d())
    }

    private fun calculateRotationalDrag(): Vector3dc {
        //todo: implement
        //response: no, later
        return Vector3d(0.0,0.0,0.0)
    }

    private fun calculateDragPosition(ship: PhysShipImpl): Vector3dc {
        val motionVector: Vector3dc = ship.poseVel.vel
        val motionNormal: Vector3dc = motionVector.normalize(Vector3d()).mul(-1.0)

        val avgCenterOfPressure: Vector3d = Vector3d()
        for (dir in Direction.values()) {
            if (exposedFaces[dir]!!.isEmpty()) continue
            val centerOfPressure = Vector3d()
            exposedFaces[dir]!!.forEach {
                centerOfPressure.add(it.x().toDouble(), it.y().toDouble(), it.z().toDouble())
            }
            centerOfPressure.div(exposedFaces[dir]!!.size.toDouble())
            val dot = motionNormal.dot(dir.normal.toJOMLD())
            centerOfPressure.mul(dot)
            avgCenterOfPressure.add(centerOfPressure)
        }

        dragLogger.info("Center of Pressure: $avgCenterOfPressure")


        return ship.transform.shipToWorld.transformPosition(avgCenterOfPressure, Vector3d())
    }

    /**
     * Returns the density of air at a Y value adapted to a real life altitude, where Y = 60 is sea level, and Y = 320 is the top of the Troposphere.
     *
     * Where:
     *
     * p = mass density (kg/m^3)
     *
     * pb = base density via b (kg/m^3)
     *
     * Tb = standard temperature via b (K)
     *
     * g0 = standard gravitational acceleration (m/s^2)
     *
     * h = altitude over sea level (m)
     *
     * hb = base altitude via b (m)
     *
     * R = universal gas constant (N-m/(mol-K))
     *
     * M = molar mass of Earth's air (kg/mol)
     *
     * L = temperature lapse rate (K/m)
     * @param y The Y value to get the air density for.
     * @return The air density at the given Y value, in kg/m^3.
     * @see <a href="https://en.wikipedia.org/wiki/Barometric_formula">Wikipedia source for krabber patter formuler</a>
     */
    private fun getAirDensityForY(y: Double): Double {
        val worldScale = 11000.0 / (max_height - 63.0)

        val realAltitude = (y - 63.0) * worldScale

        val layer = when {
            realAltitude < 11000 -> 0
            realAltitude < 20000 -> 1
            realAltitude < 32000 -> 2
            realAltitude < 47000 -> 3
            realAltitude < 51000 -> 4
            realAltitude < 71000 -> 5
            else -> 6
        }

        val hb = when (layer) {
            0 -> 0.0
            1 -> 11000.0
            2 -> 20000.0
            3 -> 32000.0
            4 -> 47000.0
            5 -> 51000.0
            6 -> 71000.0
            else -> 0.0
        }

        val pb = when (layer) {
            0 -> 1.225
            1 -> 0.36391
            2 -> 0.08803
            3 -> 0.01322
            4 -> 0.00143
            5 -> 0.00086
            6 -> 0.000064
            else -> 0.0
        }

        val Tb = when (layer) {
            0 -> 288.15
            1 -> 216.65
            2 -> 216.65
            3 -> 228.65
            4 -> 270.65
            5 -> 270.65
            6 -> 214.65
            else -> 0.0
        }

        val g0 = GRAVITATIONAL_ACCELERATION

        val R = UNIVERSAL_GAS_CONSTANT

        val M = AIR_MOLAR_MASS

        val L = when (layer) {
            0 -> 0.0065
            1 -> 0.0
            2 -> -0.001
            3 -> -0.0028
            4 -> 0.0
            5 -> 0.0028
            6 -> 0.002
            else -> 0.0
        }

        return when (L != 0.0) {
            true -> pb * Math.pow((Tb - (realAltitude - hb) * L) / Tb, ((g0 * M) / (R * L) -1.0))
            else -> pb * Math.exp((-g0 * M * (realAltitude - hb)) / (R * Tb))
        }
    }

    companion object {
        fun getOrCreate(ship: ServerShip): DragController? {
            if (ship.getAttachment(DragController::class.java) == null) {
                ship.saveAttachment(DragController::class.java, DragController())
            }
            return ship.getAttachment(DragController::class.java)
        }

        private const val DRAG_COEFFICIENT = 1.05
        private const val GRAVITATIONAL_ACCELERATION = 9.80665
        private const val UNIVERSAL_GAS_CONSTANT = 8.3144598
        private const val AIR_MOLAR_MASS = 0.0289644

        private val dragLogger by logger("Drag Controller")
    }
}