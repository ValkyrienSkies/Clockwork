package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.bodies.PhysVsBody
import org.valkyrienskies.core.api.physics.CollisionDiagnostic
import org.valkyrienskies.core.api.physics.ContactDiagnosticsOptions
import org.valkyrienskies.core.api.physics.ContactDiagnosticsSubscription
import org.valkyrienskies.core.api.world.PhysLevel
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToInt

internal object AeronautContactTelemetry {
    private const val WATCH_NANOS = 1_000_000_000L
    private const val MAX_SLEEP_PROBES = 8
    private const val MAX_SLEEP_PAIRS = 32
    private const val PROBE_INTERVAL = 5L
    private const val PROBE_EXPIRY = 30L
    private const val POINTS_PER_CELL = 16

    data class Face(val block: BlockPos, val direction: Direction, val plane: Int)
    data class Cell(val face: Face, val center: Vector3dc, val normalLoad: Double, val upwardLoad: Double,
                    val points: List<Vector3dc>)
    data class Contact(val target: Long, val cells: List<Cell>)

    private data class PairKey(val source: Long, val target: Long)
    private data class LoadSample(val cells: List<Cell>, var verified: Long)
    private class Dimension {
        val watched = ConcurrentHashMap<Long, Long>()
        var attempted = false
        var subscription: ContactDiagnosticsSubscription? = null
        var sequence = 0L
        var contacts = emptyMap<PairKey, LoadSample>()
        @Volatile var published = emptyMap<Long, List<Contact>>()
    }

    private class Accumulator {
        val moment = Vector3d()
        var normalLoad = 0.0
        var upwardLoad = 0.0
        var latestTime = Double.NEGATIVE_INFINITY
        val points = ArrayList<Vector3dc>()

        fun add(position: Vector3dc, force: Double, upward: Double, time: Double) {
            moment.fma(force, position)
            normalLoad += force
            upwardLoad += force * upward
            if (time < latestTime) return
            if (time > latestTime) {
                points.clear()
                latestTime = time
            }
            if (points.size < POINTS_PER_CELL) points.add(position)
        }

        fun finish(face: Face) = Cell(face, moment.div(normalLoad), normalLoad, upwardLoad, points)
    }

    private val dimensions = ConcurrentHashMap<String, Dimension>()

    fun watch(dimension: String, bodies: Set<Long>) {
        val watched = dimensions.computeIfAbsent(dimension) { Dimension() }.watched
        val expires = System.nanoTime() + WATCH_NANOS
        for (body in bodies) watched[body] = expires
    }

    fun read(dimension: String): Map<Long, List<Contact>> = dimensions[dimension]?.published.orEmpty()

    fun clear() = dimensions.clear()

    fun tick(level: PhysLevel) {
        val state = dimensions[level.dimension] ?: return
        val now = System.nanoTime()
        state.watched.entries.removeIf { it.value < now }
        if (state.watched.isEmpty()) {
            state.subscription?.close()
            state.subscription = null
            state.attempted = false
            state.contacts = emptyMap()
            state.published = emptyMap()
            return
        }
        if (!state.attempted) {
            state.attempted = true
            state.subscription = try {
                level.subscribeContactDiagnostics(ContactDiagnosticsOptions(collisions = true))
            } catch (_: UnsupportedOperationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        val frame = state.subscription?.read() ?: return
        state.sequence++
        val sequence = state.sequence
        val duration = frame.endTime - frame.startTime
        if (frame.overflow || !duration.isFinite() || duration <= 0.0) {
            state.contacts = emptyMap()
            state.published = emptyMap()
            return
        }

        val fresh = HashMap<PairKey, MutableMap<Face, Accumulator>>()
        val sourceBodies = HashMap<Long, PhysVsBody>()
        val watched = state.watched.keys
        for (reaction in frame.collisions) {
            if (reaction.body0 !in watched && reaction.body1 !in watched) continue
            val normalForce = reaction.force.dot(reaction.normal)
            val weight = abs(normalForce) * reaction.duration / duration
            if (weight <= 0.0 || !weight.isFinite() || !reaction.point.isFinite) continue
            val sign = if (normalForce > 0.0) 1.0 else -1.0
            val up = sign * reaction.normal.y()
            if (abs(up) <= 0.05 || !up.isFinite()) continue
            val source = if (up > 0) reaction.body0 else reaction.body1
            if (source !in watched) continue
            val target = if (up > 0) reaction.body1 else reaction.body0
            val body = sourceBodies[source] ?: level.getBodyById(source)?.also { sourceBodies[source] = it } ?: continue
            val (face, position) = faceOf(body, reaction, if (up > 0) -sign else sign) ?: continue
            fresh.getOrPut(PairKey(source, target), ::HashMap).getOrPut(face, ::Accumulator)
                .add(position, weight, abs(up), reaction.timestamp)
        }

        val contacts = fresh.mapValuesTo(HashMap()) { (_, cells) ->
            LoadSample(cells.map { (face, load) -> load.finish(face) }, sequence)
        }
        var probes = 0
        val sleeping = if (frame.reset) emptyList() else state.contacts.entries.filter { (key, _) -> key !in fresh && key.source in watched }
        var retained = 0
        for ((pair, contact) in sleeping.sortedBy { it.value.verified }) {
            if (retained >= MAX_SLEEP_PAIRS) break
            val source = level.getBodyById(pair.source) ?: continue
            val target = level.getBodyById(pair.target) ?: continue
            if (!asleep(level, source) || !asleep(level, target) || (source.isStatic && target.isStatic)) continue
            if (sequence - contact.verified >= PROBE_INTERVAL && probes < MAX_SLEEP_PROBES) {
                probes++
                if (!stillSupported(level, source, target, contact.cells)) continue
                contact.verified = sequence
            }
            if (sequence - contact.verified < PROBE_EXPIRY) {
                contacts[pair] = contact
                retained++
            }
        }
        state.contacts = contacts
        state.published = contacts.entries.groupBy({ it.key.source }, { Contact(it.key.target, it.value.cells) })
    }

    private fun faceOf(body: PhysVsBody, reaction: CollisionDiagnostic, outward: Double): Pair<Face, Vector3dc>? {
        val position = body.kinematics.toModel.transformPosition(reaction.point, Vector3d())
        val normal = body.kinematics.rotation.transformInverse(Vector3d(reaction.normal).mul(outward))
        if (!position.isFinite || !normal.isFinite) return null
        val direction = Direction.getNearest(normal.x(), normal.y(), normal.z())
        val block = BlockPos.containing(position.x - direction.stepX * 0.01,
            position.y - direction.stepY * 0.01, position.z - direction.stepZ * 0.01)
        val plane = when (direction.axis) {
            Direction.Axis.X -> position.x
            Direction.Axis.Y -> position.y
            Direction.Axis.Z -> position.z
        }
        return Face(block, direction, (plane * 8.0).roundToInt()) to position
    }

    private fun asleep(level: PhysLevel, body: PhysVsBody) = body.isStatic || level.getShipById(body.id)?.isSleeping == true

    private fun stillSupported(level: PhysLevel, source: PhysVsBody, target: PhysVsBody, cells: List<Cell>): Boolean {
        val cell = cells.maxBy { it.upwardLoad }
        val direction = cell.face.direction
        val outward = source.kinematics.rotation.transform(Vector3d(direction.stepX.toDouble(),
            direction.stepY.toDouble(), direction.stepZ.toDouble()))
        val witness = cell.points.minByOrNull { it.distanceSquared(cell.center) } ?: cell.center
        val point = source.kinematics.toWorld.transformPosition(witness, Vector3d())
        val hit = level.rayCast(point.fma(-0.15, outward), outward, 0.30, source.id) ?: return false
        return hit.hitBody.id == target.id && hit.hitNormal.dot(outward) < -0.05
    }
}
