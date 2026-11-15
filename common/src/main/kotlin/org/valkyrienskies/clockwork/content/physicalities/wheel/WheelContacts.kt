package org.valkyrienskies.clockwork.content.physicalities.wheel

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.block.Block
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.core.api.physics.ContactDiagnosticsFrame
import org.valkyrienskies.core.api.physics.ContactDiagnosticsOptions
import org.valkyrienskies.core.api.physics.TirePatchSnapshot
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

data class TireContact(val surface: Long, val normal: Vector3d, val compression: Float)
data class TireSurfaceContact(val surface: Long, val point: Vector3d, val normal: Vector3d,
                              val velocity: Vector3d, val scrub: Vector3d, val surfaceVelocity: Vector3d, val load: Float,
                              val peak: Float, val sliding: Float, val width: Float,
                              val blockState: Int = 0, val material: WheelSurface = WheelSurface.HARD)
data class TireState(val body: Long, val contacts: List<TireContact>, val slip: Float,
                     val effect: TireSurfaceContact? = null, val type: TireType = TireType.RUBBER)
data class TireMark(val surface: Long, val center: Vector3d, val normal: Vector3d, val forward: Vector3d,
                    val width: Float, val length: Float, val strength: Float, val rolling: Float = 0f)

object WheelContacts {
    internal data class Support(val position: Vector3d, val force: Vector3d)
    private data class Wheel(val width: Double, val tick: Long, val assist: Boolean, val type: TireType)
    private data class TrailKey(val stream: Long, val epoch: Long, val patch: Long, val wheelRevision: Long,
                                val surfaceRevision: Long)
    private data class Trail(val point: Vector3d, val time: Double)
    private class State {
        val wheels = ConcurrentHashMap<Long, Wheel>()
        val marks = ArrayBlockingQueue<TireMark>(512)
        val trails = HashMap<TrailKey, Trail>()
        val peaks = ConcurrentHashMap<Long, Float>()
        var enabled = false
        var unsupported = false
        @Volatile var ground = -1L
        @Volatile var contacts = emptyList<TireState>()
        @Volatile var frame: ContactDiagnosticsFrame? = null
        @Volatile var supports = emptyMap<Long, Support>()
    }
    private val dimensions = ConcurrentHashMap<String, State>()

    fun watch(level: ServerLevel, body: Long, width: Double, assist: Boolean, type: TireType) {
        val state = dimensions.computeIfAbsent(level.dimensionId) { State() }
        state.ground = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId] ?: -1L
        state.wheels[body] = Wheel(width, level.gameTime, assist, type)
    }

    fun clear() = dimensions.clear()
    fun lastFrame(dimension: String): ContactDiagnosticsFrame? = dimensions[dimension]?.frame
    internal fun support(dimension: String, wheel: Long): Support? = dimensions[dimension]?.supports?.get(wheel)

    fun tick(level: ServerLevel) {
        val state = dimensions[level.dimensionId] ?: return
        state.wheels.entries.removeIf { level.gameTime - it.value.tick > 10 }
        state.peaks.keys.removeIf { !state.wheels.containsKey(it) }
        if (level.gameTime % 2L != 0L) return
        val marks = ArrayList<TireMark>()
        state.marks.drainTo(marks, 256)
        val listeners = level.players().map { Vector3d(it.x, it.y, it.z) }
        val contacts = state.contacts.map { wheel ->
            val effect = wheel.effect
            val peak = state.peaks.remove(wheel.body) ?: 0f
            val position = level.shipObjectWorld.allBodies.getById(wheel.body)?.kinematics?.position
            val close = position != null && listeners.any { it.distanceSquared(position) < 96 * 96 }
            wheel.copy(effect = effect?.takeIf { close }?.let { resolve(level, it.copy(peak = maxOf(it.peak, peak))) })
        }
        val rollingSurfaces = HashMap<Pair<Long, BlockPos>, Boolean>()
        for (player in level.players()) {
            val position = Vector3d(player.x, player.y, player.z)
            val nearby = contacts.filter {
                level.shipObjectWorld.allBodies.getById(it.body)?.kinematics?.position?.distanceSquared(position)
                    ?.let { distance -> distance < 96.0 * 96.0 } == true
            }.take(128)
            val nearbyMarks = marks.filter {
                val center = Vector3d(it.center)
                if (it.surface != -1L) {
                    val body = level.shipObjectWorld.allBodies.getById(it.surface) ?: return@filter false
                    body.kinematics.toWorld.transformPosition(center)
                }
                if (center.distanceSquared(position) >= 96.0 * 96.0) return@filter false
                if (it.strength > .01f) return@filter true
                val probe = Vector3d(it.center).fma(-.015, it.normal)
                val pos = BlockPos.containing(probe.x, probe.y, probe.z)
                rollingSurfaces.getOrPut(it.surface to pos) {
                    WheelSurface.contact(level, it.center, it.normal)?.second?.let { block -> WheelSurface.of(block).rollingTracks(block) } == true
                }
            }
            if (nearby.isNotEmpty() || nearbyMarks.isNotEmpty())
                ClockworkPackets.sendTo(WheelContactPacket(level.dimension().location(), nearby, nearbyMarks), player)
        }
    }

    private fun resolve(level: ServerLevel, effect: TireSurfaceContact): TireSurfaceContact? {
        val (_, block) = WheelSurface.contact(level, effect.point, effect.normal) ?: return null
        val above = Vector3d(effect.point).fma(.06, effect.normal)
        val abovePos = BlockPos.containing(above.x, above.y, above.z)
        val wet = level.hasChunkAt(abovePos) && level.getFluidState(abovePos).`is`(FluidTags.WATER)
        return effect.copy(blockState = Block.getId(block), material = if (wet) WheelSurface.WET else WheelSurface.of(block))
    }

    fun physicsTick(level: PhysLevel) {
        val state = dimensions[level.dimension] ?: return
        if (state.unsupported) return
        val wheels = state.wheels.toMap()
        val enabled = wheels.isNotEmpty()
        if (enabled != state.enabled) {
            try { level.configureContactDiagnostics(ContactDiagnosticsOptions(tires = enabled)) }
            catch (_: UnsupportedOperationException) { state.unsupported = true; return }
            state.enabled = enabled
            state.trails.clear()
            state.contacts = emptyList()
        }
        if (!enabled) return
        val frame = level.drainContactDiagnostics() ?: return
        state.frame = frame
        if (frame.reset || frame.overflow) state.trails.clear()
        state.trails.entries.removeIf { frame.endTime - it.value.time > .15 || it.key.stream in frame.retiredStreams }
        val contacts = HashMap<Long, MutableList<TireContact>>()
        val slipping = HashMap<Long, Float>()
        val effects = HashMap<Long, TireSurfaceContact>()
        val supports = HashMap<Long, Support>()
        for (batch in frame.tires) {
            if (batch.reset || batch.overflow) state.trails.keys.removeIf { it.stream == batch.stream }
            for (patch in batch.patches) {
                val wheel = wheels[patch.wheelBody] ?: continue
                val key = TrailKey(batch.stream, batch.epoch, patch.patch, patch.wheelRevision, patch.surfaceRevision)
                if (patch.events and TirePatchSnapshot.END != 0) { state.trails.remove(key); continue }
                if (wheel.assist && !frame.reset && !frame.overflow && !batch.reset && !batch.overflow && patch.normal.isFinite &&
                    patch.normalForce.isFinite() && patch.normalForce > 0 &&
                    (patch.sleeping || frame.endTime - patch.lastSolvedTime <= .05)) {
                    supports.getOrPut(patch.wheelBody) { Support(Vector3d(patch.wheelFrame.position), Vector3d()) }
                        .force.fma(patch.normalForce, patch.normal)
                }
                val ship = if (patch.surfaceBody == state.ground) null else level.getShipById(patch.surfaceBody)
                val surface = ship?.id ?: -1L
                val normal = Vector3d(patch.normal)
                ship?.transform?.toModel?.transformDirection(normal)
                if (!normal.isFinite || normal.lengthSquared() < 1e-12) continue
                normal.normalize()
                if (patch.compression.isFinite() && patch.compression > 0.0)
                    contacts.getOrPut(patch.wheelBody) { ArrayList() }.add(TireContact(surface, normal, patch.compression.toFloat()))
                val sliding = if (patch.sleeping || patch.normalForce < 100.0 || patch.normalImpulse <= 0.0 || !patch.slidingWork.isFinite()) 0.0
                    else patch.slidingWork / patch.normalImpulse
                val slip = ((sliding - .08) / 2.0).coerceIn(0.0, 1.0).toFloat()
                slipping[patch.wheelBody] = maxOf(slipping[patch.wheelBody] ?: 0f, slip)
                var rolling = 0f
                val contactLoad = if (wheel.type.physics.adhesion > 0) abs(patch.normalForce) else patch.normalForce
                if (contactLoad.isFinite() && (contactLoad > 100 || wheel.type.appearance.webs)) {
                    val point = patch.surfaceFrame.rotation.transform(Vector3d(patch.surfaceLocalPoint)).add(patch.surfaceFrame.position)
                    val surfaceVelocity = Vector3d(patch.surfaceFrame.angularVelocity)
                        .cross(Vector3d(point).sub(patch.surfaceFrame.position)).add(patch.surfaceFrame.velocity)
                    val velocity = Vector3d(patch.wheelFrame.velocity).sub(surfaceVelocity)
                    val scrub = Vector3d(patch.slipVelocity)
                    if (patch.sleeping) { velocity.zero(); scrub.zero() }
                    rolling = ((Vector3d(velocity).fma(-velocity.dot(patch.normal), patch.normal).length() - .15) / .6)
                        .coerceIn(0.0, 1.0).toFloat()
                    ship?.transform?.toModel?.transformPosition(point)
                    ship?.transform?.toModel?.transformDirection(velocity)
                    ship?.transform?.toModel?.transformDirection(scrub)
                    if (point.isFinite && velocity.isFinite && scrub.isFinite && surfaceVelocity.isFinite) {
                        val effect = TireSurfaceContact(surface, point, Vector3d(normal), velocity, scrub, surfaceVelocity,
                            contactLoad.toFloat(), (if (patch.sleeping) contactLoad else maxOf(0.0, patch.peakNormalForce)).toFloat(),
                            sliding.toFloat(), wheel.width.toFloat())
                        if (effect.load > (effects[patch.wheelBody]?.load ?: -1f)) effects[patch.wheelBody] = effect
                        if (effect.peak.isFinite()) state.peaks.merge(patch.wheelBody, effect.peak) { a, b -> maxOf(a, b) }
                    }
                }
                val markStrength = ((sliding - .8) / 2.8).coerceIn(0.0, 1.0).toFloat()
                if (markStrength <= .01f && rolling <= .01f) {
                    state.trails.remove(key)
                    continue
                }
                if (patch.pathTruncated) state.trails.remove(key)
                fun position(point: Vector3dc): Vector3d {
                    val world = patch.surfaceFrame.rotation.transform(Vector3d(point)).add(patch.surfaceFrame.position)
                    ship?.transform?.toModel?.transformPosition(world)
                    return world
                }
                val forward = Vector3d(patch.rolling)
                ship?.transform?.toModel?.transformDirection(forward)
                forward.normalize()
                val scale = ship?.transform?.scaling?.x() ?: 1.0
                val wheelScale = level.getBodyById(patch.wheelBody)?.kinematics?.scaling?.x() ?: 1.0
                val width = (wheel.width * .58 * wheelScale / scale).toFloat()
                val samples = patch.path.map { it.timestamp to position(it.surfaceLocalPoint) }.ifEmpty {
                    listOf(patch.timestamp to position(patch.surfaceLocalPoint))
                }
                for ((time, point) in samples) {
                    if (!point.isFinite) continue
                    val previous = state.trails[key]
                    if (previous != null && time <= previous.time) continue
                    val delta = previous?.let { Vector3d(point).sub(it.point) }
                    val distance = delta?.length() ?: 0.0
                    if (previous != null && time - previous.time <= .15 && distance in .015..2.0) {
                        state.marks.offer(TireMark(surface, Vector3d(point).add(previous.point).mul(.5), normal,
                            delta!!.div(distance), width, (distance + .015).toFloat(), markStrength, rolling))
                    } else if (distance < .015 && markStrength > .01f) {
                        state.marks.offer(TireMark(surface, point, normal, forward, width,
                            (patch.footprintLength / scale).coerceIn(.08, 1.0).toFloat(), markStrength, rolling))
                    }
                    if (previous == null || distance >= .015 || markStrength > .01f || time - previous.time > .15)
                        state.trails[key] = Trail(point, time)
                }
            }
        }
        state.supports = supports
        state.contacts = wheels.map { (id, wheel) ->
            TireState(id, contacts[id].orEmpty().sortedByDescending { it.compression }.take(4), slipping[id] ?: 0f, effects[id], wheel.type)
        }
    }
}

class WheelContactPacket(val dimension: ResourceLocation, val wheels: List<TireState>, val marks: List<TireMark>) : S2CCWPacket {
    override var player: Player? = null
    constructor(buffer: FriendlyByteBuf) : this(buffer.readResourceLocation(), List(count(buffer, 128)) {
        TireState(buffer.readLong(), List(count(buffer, 4)) {
            TireContact(buffer.readLong(), vector(buffer), scalar(buffer, 0f, 1024f))
        }, scalar(buffer, 0f, 1f), if (buffer.readBoolean()) readEffect(buffer) else null, TireType.byId(buffer.readResourceLocation()))
    }, List(count(buffer, 256)) {
        TireMark(buffer.readLong(), vector(buffer), vector(buffer), vector(buffer), scalar(buffer, 0f, 1024f),
            scalar(buffer, 0f, 1024f), scalar(buffer, 0f, 1f), scalar(buffer, 0f, 1f))
    })

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeResourceLocation(dimension)
        buffer.writeVarInt(wheels.size)
        for (wheel in wheels) {
            buffer.writeLong(wheel.body); buffer.writeVarInt(wheel.contacts.size)
            for (contact in wheel.contacts) {
                buffer.writeLong(contact.surface); vector(buffer, contact.normal); buffer.writeFloat(contact.compression)
            }
            buffer.writeFloat(wheel.slip)
            buffer.writeBoolean(wheel.effect != null)
            wheel.effect?.let { effect ->
                buffer.writeLong(effect.surface); vector(buffer, effect.point); vector(buffer, effect.normal)
                vector(buffer, effect.velocity); vector(buffer, effect.scrub); vector(buffer, effect.surfaceVelocity)
                buffer.writeFloat(effect.load); buffer.writeFloat(effect.peak); buffer.writeFloat(effect.sliding)
                buffer.writeFloat(effect.width); buffer.writeVarInt(effect.blockState); buffer.writeEnum(effect.material)
            }
            buffer.writeResourceLocation(wheel.type.id)
        }
        buffer.writeVarInt(marks.size)
        for (mark in marks) {
            buffer.writeLong(mark.surface); vector(buffer, mark.center); vector(buffer, mark.normal); vector(buffer, mark.forward)
            buffer.writeFloat(mark.width); buffer.writeFloat(mark.length); buffer.writeFloat(mark.strength); buffer.writeFloat(mark.rolling)
        }
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork { WheelEffects.accept(this) }
        context.setPacketHandled(true)
    }

    companion object {
        private fun readEffect(buffer: FriendlyByteBuf) = TireSurfaceContact(buffer.readLong(), vector(buffer), vector(buffer),
            vector(buffer), vector(buffer), vector(buffer), scalar(buffer, 0f, Float.MAX_VALUE), scalar(buffer, 0f, Float.MAX_VALUE),
            scalar(buffer, 0f, Float.MAX_VALUE), scalar(buffer, 0f, 1024f), buffer.readVarInt(), buffer.readEnum(WheelSurface::class.java))
        private fun count(buffer: FriendlyByteBuf, maximum: Int) = buffer.readVarInt().also { require(it in 0..maximum) }
        private fun scalar(buffer: FriendlyByteBuf, min: Float, max: Float) = buffer.readFloat().also { require(it.isFinite() && it in min..max) }
        private fun vector(buffer: FriendlyByteBuf) = Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()).also { require(it.isFinite) }
        private fun vector(buffer: FriendlyByteBuf, value: Vector3d) { buffer.writeDouble(value.x); buffer.writeDouble(value.y); buffer.writeDouble(value.z) }
    }
}
