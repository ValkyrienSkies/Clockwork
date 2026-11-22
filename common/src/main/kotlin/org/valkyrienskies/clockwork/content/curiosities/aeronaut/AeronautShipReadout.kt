package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.Contraption
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.ChunkStatus
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.forces.BalloonController
import org.valkyrienskies.clockwork.content.forces.WanderShipControl
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.ContraptionWingProvider
import org.valkyrienskies.core.api.ships.Wing
import org.valkyrienskies.mod.common.block.WingBlock
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.WeakHashMap
import kotlin.math.PI
import kotlin.math.abs

/** Game-thread cache. Never loads chunks or walks a ship's empty bounding-box volume. */
object AeronautShipReadout {
    data class Snapshot(val mass: Double, val massCenter: Vector3dc,
        val potentialThrust: AeronautForceCenter?, val liveThrust: AeronautForceCenter?,
        val potentialLift: AeronautForceCenter?, val liveLift: AeronautForceCenter?,
        val assembly: AeronautAssemblyReadout.Detail = AeronautAssemblyReadout.Detail(),
        val massVisible: Boolean = true, val visibleGuides: Int = ALL_GUIDES)

    private data class PositionedWing(val position: BlockPos, val wing: Wing)
    private class Cache {
        var dirty = true
        var sampled = Long.MIN_VALUE
        var used = 0L
        var chunkCount = -1
        val chunks = HashMap<ChunkPos, LevelChunk>()
        val sources = ArrayList<BlockEntity>()
        val wings = ArrayList<PositionedWing>()
        var snapshot: Snapshot? = null
    }
    private val levels = WeakHashMap<ServerLevel, MutableMap<Long, Cache>>()
    private val contraptionWings = WeakHashMap<Contraption, List<PositionedWing>>()

    fun invalidate(level: ServerLevel, shipId: Long) { levels[level]?.get(shipId)?.dirty = true }

    @JvmStatic
    fun invalidateContraption(contraption: Contraption?) { if (contraption != null) contraptionWings.remove(contraption) }

    fun clear() {
        levels.clear()
        contraptionWings.clear()
        AeronautForceTelemetry.clear()
    }

    fun sample(level: ServerLevel, ship: LoadedServerShip): Snapshot {
        val caches = levels.getOrPut(level) { HashMap() }
        caches.entries.removeIf { level.gameTime - it.value.used > 200 }
        val cache = caches.getOrPut(ship.id) { Cache() }
        cache.used = level.gameTime
        AeronautForceTelemetry.watch(ship.id)
        if (cache.snapshot != null && level.gameTime - cache.sampled in 0 until AeronautForceReadout.UPDATE_TICKS)
            return cache.snapshot!!

        if (cache.dirty || cache.chunkCount != ship.activeChunksSet.size ||
            cache.chunks.any { (pos, chunk) -> level.chunkSource.getChunkNow(pos.x, pos.z) !== chunk }) rebuild(level, ship, cache)
        val thrust = AeronautForceAccumulator()
        for (source in cache.sources) {
            if (source.isRemoved || !level.hasChunkAt(source.blockPos)) { cache.dirty = true; continue }
            if (source is AeronautForceSource) source.addPotentialForces(level, ship, thrust)
            else if (source is EncasedFanBlockEntity) {
                // Static-air form of EncasedFanController's blade calculation, at the design RPM.
                val lift = 2.0 * PI * Math.toRadians(15.0)
                val force = 0.5 * 1.225 * (AeronautForceReadout.REFERENCE_RPM * 0.5).let { it * it } *
                    0.15 * 0.5 * lift * ClockworkConfig.SERVER.encasedFan.encasedFanForceMul
                thrust.add(source.blockPos.toJOMLD().add(0.5, 0.5, 0.5),
                    source.blockState.getValue(BlockStateProperties.FACING).normal.toJOMLD().mul(force))
            }
        }

        val potentialLift = AeronautForceAccumulator()
        val liveLift = AeronautForceAccumulator()
        for (wing in cache.wings) addWing(level, ship, wing.position.toJOMLD().add(0.5, 0.5, 0.5),
            wing.wing.wingNormal, wing.wing, potentialLift, liveLift)

        // Flaps and other assembled Create contraptions have left the chunk's block palette.
        ship.shipAABB?.let { box ->
            val bounds = AABB(box.minX().toDouble(), box.minY().toDouble(), box.minZ().toDouble(),
                box.maxX() + 1.0, box.maxY() + 1.0, box.maxZ() + 1.0).inflate(16.0)
            for (entity in level.getEntitiesOfClass(AbstractContraptionEntity::class.java, bounds)) {
                if (level.getLoadedShipManagingPos(entity.blockPosition())?.id != ship.id) continue
                val provider = entity as? ContraptionWingProvider ?: continue
                if (provider.wingGroupId < 0) continue
                val contraption = entity.contraption ?: continue
                val wings = contraptionWings.getOrPut(contraption) {
                    contraption.blocks.values.mapNotNull { info ->
                        val block = info.state.block as? WingBlock ?: return@mapNotNull null
                        val wing = block.getWing(level, info.pos, info.state) ?: return@mapNotNull null
                        PositionedWing(info.pos, wing)
                    }
                }
                val transform = provider.computeContraptionWingTransform()
                for (wing in wings) {
                    val local = Vec3.atCenterOf(wing.position)
                    val movement = entity.toGlobalVector(local, 1f).subtract(entity.toGlobalVector(local, 0f, true)).scale(20.0)
                    addWing(level, ship, transform.transformPosition(wing.position.toJOMLD().add(0.5, 0.5, 0.5)),
                        transform.transformDirection(wing.wing.wingNormal, Vector3d()).normalize(),
                        wing.wing, potentialLift, liveLift, Vector3d(movement.x, movement.y, movement.z))
                }
            }
        }

        val up = ship.transform.shipToWorldRotation.transformInverse(Vector3d(0.0, 1.0, 0.0))
        val gravity = level.shipObjectWorld.aerodynamicUtils.getAtmosphereForDimension(level.dimensionId).third
        ship.getAttachment(BalloonController::class.java)?.let { controller ->
            for (balloon in controller.balloons.values) {
                if (balloon.shouldRemove || balloon.shouldValidate) continue
                val data = balloon.makeForceData(level, ship)
                val force = Vector3d(up).mul(data.hotAir * gravity * ClockworkConfig.SERVER.balloons.balloonForceMult)
                potentialLift.add(data.center, force)
            }
            for (data in controller.forcefulBalloons) liveLift.add(data.center,
                Vector3d(up).mul(data.hotAir * gravity * ClockworkConfig.SERVER.balloons.balloonForceMult))
        }
        ship.getAttachment(WanderShipControl::class.java)?.let { controller ->
            val blocks = controller.wanderBlocks
            val mass = blocks.values.sum()
            if (mass > 0.0) {
                val center = Vector3d()
                blocks.forEach { (pos, weight) -> center.add((pos.x() + 0.5) * weight, (pos.y() + 0.5) * weight, (pos.z() + 0.5) * weight) }
                center.div(mass)
                val force = Vector3d(up).mul(mass * gravity * ClockworkConfig.SERVER.wanderlite.wanderOreForceMultiplier)
                potentialLift.add(center, force)
                liveLift.add(center, force)
            }
        }
        cache.sampled = level.gameTime
        val assembly = AeronautAssemblyReadout.sample(level, ship)
        return Snapshot(assembly.mass, assembly.center, thrust.center(),
            AeronautForceTelemetry.thrust(ship.id), potentialLift.center(), liveLift.center(), assembly.detail).also { cache.snapshot = it }
    }

    private fun rebuild(level: ServerLevel, ship: LoadedServerShip, cache: Cache) {
        cache.sources.clear()
        cache.wings.clear()
        cache.chunks.clear()
        var missingChunks = false
        ship.activeChunksSet.forEach { x, z ->
            val chunk = level.chunkSource.getChunkNow(x, z)
            if (chunk == null) missingChunks = true
            else {
                cache.chunks[chunk.pos] = chunk
                cache.sources.addAll(chunk.blockEntities.values.filter { it is AeronautForceSource || it is EncasedFanBlockEntity })
                for ((index, section) in chunk.sections.withIndex()) {
                    if (section.hasOnlyAir() || !section.maybeHas { it.block is WingBlock }) continue
                    val baseY = level.getSectionYFromSectionIndex(index) shl 4
                    for (dx in 0..15) for (dy in 0..15) for (dz in 0..15) {
                        val state = section.getBlockState(dx, dy, dz)
                        val block = state.block as? WingBlock ?: continue
                        val pos = BlockPos((x shl 4) + dx, baseY + dy, (z shl 4) + dz)
                        val wing = block.getWing(level, pos, state) ?: continue
                        cache.wings.add(PositionedWing(pos, wing))
                    }
                }
            }
        }
        cache.chunkCount = ship.activeChunksSet.size
        cache.dirty = missingChunks
    }

    private fun addWing(level: ServerLevel, ship: LoadedServerShip, position: Vector3dc, normal: Vector3dc,
        wing: Wing, potential: AeronautForceAccumulator, live: AeronautForceAccumulator, movement: Vector3dc = Vector3d()) {
        if (!position.isFinite || !normal.isFinite || normal.lengthSquared() < 1e-12) return
        val worldPosition = ship.transform.shipToWorld.transformPosition(position, Vector3d())
        val worldNormal = ship.transform.shipToWorldRotation.transform(normal, Vector3d()).normalize()
        val atmosphere = level.shipObjectWorld.aerodynamicUtils
        val density = atmosphere.getAirDensityForY(worldPosition.y, level.dimensionId)
        val velocity = ship.angularVelocity.cross(worldPosition.sub(ship.transform.positionInWorld, Vector3d()), Vector3d()).add(ship.velocity)
            .add(ship.transform.shipToWorld.transformDirection(movement, Vector3d()))
        val force = AeronautForceReadout.wingLift(worldNormal, velocity, density, wing.wingLiftPower, wing.wingCamberAttackAngleBias)
        live.add(position, ship.transform.shipToWorldRotation.transformInverse(force, Vector3d()))
        val reference = AeronautForceReadout.wingLift(normal, AeronautForceReadout.referenceFlow(normal),
            1.225, wing.wingLiftPower, Math.toRadians(10.0))
        potential.add(position, reference)
    }

    internal const val PROJECTION = 1
    internal const val THRUST_ARROW = 2
    internal const val LIFT_ARROW = 4
    internal const val ALL_GUIDES = PROJECTION or THRUST_ARROW or LIFT_ARROW
    internal const val SERVER_RAYS = 64
    internal const val CLIENT_RAYS = 48
    private const val MAX_OUTLINE_EDGES = 24
    private const val MAX_GUIDE_CELLS = 1024
    private const val END_MARGIN = 0.02

    internal class TerrainSight(private val level: Level, val eye: Vec3, private var remaining: Int) {
        private data class Query(val point: Vec3, val terminalBlock: BlockPos?)
        private val results = HashMap<Query, Boolean>()
        private val chunks = HashMap<Long, ChunkAccess?>()
        private var guideCellsLeft = MAX_GUIDE_CELLS

        fun sees(position: Vector3dc?, terminalBlock: BlockPos? = null): Boolean {
            if (position == null || !position.isFinite) return false
            return sees(Vec3(position.x(), position.y(), position.z()), terminalBlock)
        }

        fun sees(position: Vec3, terminalBlock: BlockPos? = null): Boolean {
            val delta = position.subtract(eye)
            val distance = delta.length()
            if (!distance.isFinite() || distance > AeronautForceReadout.MAX_RANGE) return false
            val query = Query(position, terminalBlock)
            results[query]?.let { return it }
            if (remaining <= 0) return false
            remaining--
            val end = beforeEndpoint(position)
            val blocked = BlockGetter.traverseBlocks(eye, end, level, { _, pos ->
                if (pos == terminalBlock || level.isOutsideBuildHeight(pos)) return@traverseBlocks null
                val state = chunkAt(pos)?.getBlockState(pos) ?: return@traverseBlocks true
                if (!state.isAir && state.getCollisionShape(level, pos).clip(eye, end, pos) != null) true else null
            }, { false })
            return (blocked == false).also { results[query] = it }
        }

        fun seesSegment(from: Vector3dc, to: Vector3dc): Boolean {
            if (!sees(from) || !sees(to)) return false
            if (from.distanceSquared(to) < 1e-8) return true
            val start = beforeEndpoint(Vec3(from.x(), from.y(), from.z()))
            val end = beforeEndpoint(Vec3(to.x(), to.y(), to.z()))
            val triangle = ViewingTriangle(eye, start, end)
            val visited = HashSet<BlockPos>()
            val queue = ArrayDeque<BlockPos>()
            val origin = BlockPos.containing(eye)
            visited.add(origin)
            queue.add(origin)
            while (queue.isNotEmpty()) {
                if (guideCellsLeft <= 0) return false
                guideCellsLeft--
                val pos = queue.removeFirst()
                if (!level.isOutsideBuildHeight(pos)) {
                    val state = chunkAt(pos)?.getBlockState(pos) ?: return false
                    if (!state.isAir) {
                        var hit = false
                        state.getCollisionShape(level, pos).forAllBoxes { x0, y0, z0, x1, y1, z1 ->
                            if (triangle.intersects(pos.x + x0, pos.y + y0, pos.z + z0, pos.x + x1, pos.y + y1, pos.z + z1))
                                hit = true
                        }
                        if (hit) return false
                    }
                }
                for (side in Direction.entries) {
                    val next = pos.relative(side)
                    if (visited.add(next) && triangle.intersects(next.x.toDouble(), next.y.toDouble(), next.z.toDouble(),
                            next.x + 1.0, next.y + 1.0, next.z + 1.0)) queue.add(next)
                }
            }
            return true
        }

        private fun chunkAt(pos: BlockPos): ChunkAccess? {
            val x = pos.x shr 4
            val z = pos.z shr 4
            return chunks.getOrPut(ChunkPos.asLong(x, z)) { level.chunkSource.getChunk(x, z, ChunkStatus.FULL, false) }
        }

        private fun beforeEndpoint(point: Vec3): Vec3 = point.add(eye.subtract(point).normalize().scale(minOf(END_MARGIN, eye.distanceTo(point))))
    }

    internal class ViewingTriangle(private val eye: Vec3, from: Vec3, to: Vec3) {
        private data class Projection(val axis: Vec3, val minimum: Double, val maximum: Double) {
            fun overlaps(x: Double, y: Double, z: Double, halfX: Double, halfY: Double, halfZ: Double): Boolean {
                val radius = abs(axis.x) * halfX + abs(axis.y) * halfY + abs(axis.z) * halfZ
                val offset = axis.x * x + axis.y * y + axis.z * z
                return minimum <= offset + radius + 1e-7 && maximum >= offset - radius - 1e-7
            }
        }

        private val projections: List<Projection> = run {
            val a = from.subtract(eye)
            val b = to.subtract(eye)
            val boxAxes = listOf(Vec3(1.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0), Vec3(0.0, 0.0, 1.0))
            val crossAxes = listOf(a, b, b.subtract(a)).flatMap { edge -> boxAxes.map(edge::cross) }
            (boxAxes + a.cross(b) + crossAxes).filter { it.lengthSqr() > 1e-16 }.map { raw ->
                val axis = raw.scale(1.0 / raw.length())
                val first = a.dot(axis)
                val second = b.dot(axis)
                Projection(axis, minOf(0.0, first, second), maxOf(0.0, first, second))
            }
        }

        fun intersects(x0: Double, y0: Double, z0: Double, x1: Double, y1: Double, z1: Double): Boolean {
            val x = (x0 + x1) * 0.5 - eye.x
            val y = (y0 + y1) * 0.5 - eye.y
            val z = (z0 + z1) * 0.5 - eye.z
            val halfX = (x1 - x0) * 0.5
            val halfY = (y1 - y0) * 0.5
            val halfZ = (z1 - z0) * 0.5
            return projections.all { it.overlaps(x, y, z, halfX, halfY, halfZ) }
        }
    }

    private data class Reading<T>(val value: T, val position: Vector3dc?, val terminalBlock: BlockPos? = null)

    internal fun filterVisible(snapshot: AeronautShipReadout.Snapshot, sight: TerrainSight, look: Vec3, rootId: Long,
               worldPoint: (Long?, Vector3dc) -> Vector3dc?): AeronautShipReadout.Snapshot {
        fun anchor(reading: AeronautAssemblyReadout.Anchor) = worldPoint(reading.shipId,
            Vector3d(reading.pos.x + 0.5, reading.pos.y + 0.5, reading.pos.z + 0.5))
        val massCenter = worldPoint(rootId, snapshot.massCenter)
        val massVisible = snapshot.massVisible && sight.sees(massCenter)
        fun force(center: AeronautForceCenter?) = center?.takeIf { sight.sees(worldPoint(rootId, it.position)) }
        val potentialThrust = force(snapshot.potentialThrust)
        val liveThrust = force(snapshot.liveThrust)
        val potentialLift = force(snapshot.potentialLift)
        val liveLift = force(snapshot.liveLift)
        val assembly = snapshot.assembly
        val outline = assembly.outline.takeIf { visibleOutline(it, sight) }.orEmpty()

        val readings = buildList<Reading<Any>> {
            for (joint in assembly.joints) add(Reading(joint, anchor(joint.anchor), joint.anchor.pos.takeIf { joint.anchor.shipId == null }))
            for (foot in assembly.feet) add(Reading(foot, anchor(foot.anchor), foot.anchor.pos.takeIf { foot.anchor.shipId == null }))
            for (face in assembly.contactFaces) add(Reading(face, worldPoint(face.shipId, face.center)))
        }.filter { it.position != null }.sortedByDescending {
            val point = it.position!!
            val offset = Vec3(point.x() - sight.eye.x, point.y() - sight.eye.y, point.z() - sight.eye.z)
            offset.normalize().dot(look)
        }
        val feet = ArrayList<AeronautAssemblyReadout.Foot>()
        val joints = ArrayList<AeronautAssemblyReadout.Joint>()
        val faces = ArrayList<AeronautAssemblyReadout.ContactFace>()
        for (reading in readings) {
            if (!sight.sees(reading.position, reading.terminalBlock)) continue
            when (val value = reading.value) {
                is AeronautAssemblyReadout.Foot -> feet.add(value)
                is AeronautAssemblyReadout.Joint -> joints.add(value)
                is AeronautAssemblyReadout.ContactFace -> faces.add(value)
            }
        }

        var guides = 0
        if (massVisible && massCenter != null && outline.isNotEmpty() && snapshot.visibleGuides and PROJECTION != 0) {
            val projection = Vector3d(massCenter.x(), outline.first().y(), massCenter.z())
            if (sight.seesSegment(massCenter, projection)) guides = guides or PROJECTION
        }
        fun arrow(center: AeronautForceCenter?, guide: Int) {
            if (center == null || center.force.lengthSquared() <= 1e-8 || snapshot.visibleGuides and guide == 0) return
            val from = worldPoint(rootId, center.position) ?: return
            val to = worldPoint(rootId, Vector3d(center.position).add(Vector3d(center.force).normalize().mul(2.0))) ?: return
            if (sight.seesSegment(from, to)) guides = guides or guide
        }
        arrow(liveThrust, THRUST_ARROW)
        arrow(liveLift, LIFT_ARROW)
        return snapshot.copy(mass = if (massVisible) snapshot.mass else 0.0,
            massCenter = if (massVisible) snapshot.massCenter else Vector3d(), potentialThrust = potentialThrust,
            liveThrust = liveThrust, potentialLift = potentialLift, liveLift = liveLift,
            assembly = assembly.copy(bodyCount = if (massVisible) assembly.bodyCount else 1, feet = feet, outline = outline,
                joints = joints, contactFaces = faces), massVisible = massVisible, visibleGuides = guides)
    }

    private fun visibleOutline(outline: List<Vector3dc>, sight: TerrainSight): Boolean = when (outline.size) {
        0 -> false
        1 -> sight.sees(outline[0])
        2 -> sight.seesSegment(outline[0], outline[1])
        else -> outline.size <= MAX_OUTLINE_EDGES && outline.indices.all { index ->
            sight.seesSegment(outline[index], outline[(index + 1) % outline.size])
        }
    }
}
