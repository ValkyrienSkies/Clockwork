package org.valkyrienskies.clockwork.content.curiosities.magnet

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkTags
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautShipRequestPacket
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.isChunkInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor
import kotlin.math.ceil
import kotlin.math.sqrt

object Magnetism {
    private class Chunk(val instance: LevelChunk, val blocks: MutableMap<BlockPos, BlockState>) {
        var dirty = true
        var ship: Long? = null
        var materials = emptyList<Material>()
    }
    private class Material(val ship: Long?, val position: BlockPos, val state: BlockState) {
        val center = center(position)
        val iron = state.block !is MagnetBlock
        val source = (state.block as? MagnetBlock)?.let { block ->
            val strength = block.fieldStrength(state)
            if (strength == 0.0) null else MagneticFieldPacket.Source(ship ?: -1, position,
                state.getValue(DirectionalBlock.FACING), strength.toFloat())
        }
        val poles = source?.let {
            val n = it.direction
            MagneticField.poles(center, Vector3d(n.stepX.toDouble(), n.stepY.toDouble(), n.stepZ.toDouble()), it.strength.toDouble())
        } ?: emptyList()
        val coarsePoles = if (poles.isEmpty()) emptyList() else MagneticField.coarsePoles(poles)
    }
    private data class Snapshot(val materials: List<Material>, val strength: Double, val cores: Boolean)
    private data class CoreField(val material: Material, val source: MagneticFieldPacket.Source)
    private data class Cell(val x: Int, val y: Int, val z: Int)
    private data class Source(val index: Int, val body: PhysShip?, val center: Vector3d,
                              val poles: List<MagneticField.Pole>, val coarsePoles: List<MagneticField.Pole>, val load: Load?) {
        val radius = sqrt(poles.maxOf { it.position.distanceSquared(center) })
    }
    private class Load(val body: PhysShip) {
        val force = Vector3d()
        val torque = Vector3d()
        private val center = body.transform.positionInWorld
        fun add(point: Vector3dc, value: Vector3dc, sign: Double = 1.0) {
            if (!value.isFinite) return
            force.fma(sign, value)
            val x = point.x() - center.x(); val y = point.y() - center.y(); val z = point.z() - center.z()
            torque.add(sign * (y * value.z() - z * value.y()), sign * (z * value.x() - x * value.z()), sign * (x * value.y() - y * value.x()))
        }
    }
    private val chunks = WeakHashMap<ServerLevel, MutableMap<Long, Chunk>>()
    private val snapshots = ConcurrentHashMap<String, Snapshot>()
    private val fieldSamples = ConcurrentHashMap<String, MagneticField.Sample>()

    private val coreSolvers = ConcurrentHashMap<String, MagneticField.CoreSolver>()
    private val coreFields = ConcurrentHashMap<String, List<CoreField>>()

    fun clear() { chunks.clear(); snapshots.clear(); fieldSamples.clear(); coreSolvers.clear(); coreFields.clear() }

    fun chunkLoaded(level: ServerLevel, chunk: LevelChunk, loaded: Boolean) {
        val index = chunks.getOrPut(level) { HashMap() }
        val key = chunk.pos.toLong()
        if (!loaded) {
            if (index[key]?.instance === chunk) index.remove(key)
            return
        }
        val blocks = HashMap<BlockPos, BlockState>()
        for ((sectionIndex, section) in chunk.sections.withIndex()) {
            if (!section.maybeHas(::magnetic)) continue
            val baseY = chunk.getSectionYFromSectionIndex(sectionIndex) shl 4
            for (y in 0..15) for (z in 0..15) for (x in 0..15) {
                val state = section.getBlockState(x, y, z)
                if (magnetic(state)) blocks[BlockPos(chunk.pos.minBlockX + x, baseY + y, chunk.pos.minBlockZ + z)] = state
            }
        }
        index[key] = Chunk(chunk, blocks)
    }

    fun blockChanged(level: ServerLevel, pos: BlockPos, state: BlockState) {
        val index = chunks[level] ?: return
        val chunk = index[ChunkPos.asLong(pos.x shr 4, pos.z shr 4)] ?: return
        val previous = if (magnetic(state)) chunk.blocks.put(pos.immutable(), state) else chunk.blocks.remove(pos)
        if (previous != state && (previous != null || magnetic(state))) chunk.dirty = true
    }

    fun tick(level: ServerLevel) {
        val index = chunks.getOrPut(level) { HashMap() }
        for (ship in level.shipObjectWorld.loadedShips) {
            if (ship.chunkClaimDimension != level.dimensionId) continue
            ship.activeChunksSet.forEach { x, z ->
                val chunk = level.chunkSource.getChunkNow(x, z)
                if (chunk != null && index[ChunkPos.asLong(x, z)]?.instance !== chunk) chunkLoaded(level, chunk, true)
            }
        }
        val materials = ArrayList<Material>()
        for ((_, chunk) in index) {
            if (chunk.blocks.isEmpty()) continue
            val ship = level.getLoadedShipManagingPos(chunk.blocks.keys.first())
            if (ship == null && level.isChunkInShipyard(chunk.instance.pos.x, chunk.instance.pos.z)) continue
            if (chunk.dirty || chunk.ship != ship?.id) {
                chunk.materials = chunk.blocks.map { (pos, state) -> Material(ship?.id, pos, state) }
                chunk.ship = ship?.id
                chunk.dirty = false
            }
            materials.addAll(chunk.materials)
        }
        val configured = ClockworkConfig.SERVER.magnetForceMultiplier
        val snapshot = Snapshot(materials, if (configured.isFinite()) configured.coerceIn(0.0, 100.0) else 1.0, ClockworkConfig.SERVER.magneticIronCores)
        snapshots[level.dimensionId] = snapshot
        if (level.gameTime % 5L == 0L) syncFields(level, snapshot)
    }

    private fun syncFields(level: ServerLevel, snapshot: Snapshot) {
        val players = level.players().filter(AeronautShipRequestPacket::canInspect)
        if (players.isEmpty()) return
        val available = snapshot.materials.mapNotNull { material -> material.source?.let { CoreField(material, it) } }.toMutableList()
        if (snapshot.cores) {
            val present = snapshot.materials.toHashSet()
            available.addAll(coreFields[level.dimensionId].orEmpty().filter { it.material in present })
        }
        val sources = available.mapNotNull { (material, source) ->
            if (snapshot.strength == 0.0) return@mapNotNull null
            val position = Vector3d(material.center)
            if (material.ship != null) {
                val ship = level.shipObjectWorld.loadedShips.getById(material.ship) ?: return@mapNotNull null
                ship.transform.shipToWorld.transformPosition(position)
            }
            source to position
        }
        for (player in players) {
            val eye = Vector3d(player.x, player.eyeY, player.z)
            val nearby = sources.filter { (_, position) -> position.distanceSquared(eye) <= 48.0 * 48.0 }.map { it.first }
            val crowded = nearby.size > MagneticFieldPacket.MAX_SOURCES ||
                nearby.size * 2 + nearby.count { it.moment == null } * 6 > MagneticFieldPacket.MAX_POLES
            ClockworkPackets.sendTo(MagneticFieldPacket(level.dimension().location(), if (crowded) emptyList() else nearby, crowded, snapshot.cores), player)
        }
    }

    fun physicsTick(level: PhysLevel) {
        val snapshot = snapshots[level.dimension] ?: return
        if (snapshot.strength == 0.0) {
            coreSolvers.remove(level.dimension); coreFields.remove(level.dimension)
            return
        }
        val sources = ArrayList<Source>()
        val iron = ArrayList<Pair<Material, PhysShip?>>()
        val loads = HashMap<Long, Load>()
        fun load(body: PhysShip?): Load? = if (body == null || body.isStatic) null else loads.getOrPut(body.id) { Load(body) }
        for (material in snapshot.materials) {
            val body = material.ship?.let(level::getShipById)
            if (material.ship != null && body == null) continue
            if (material.iron) { iron.add(material to body); continue }
            if (material.poles.isEmpty()) continue
            val poles = if (body == null) material.poles else material.poles.map { pole ->
                MagneticField.Pole(world(body, pole.position), pole.charge)
            }
            val coarse = if (body == null) material.coarsePoles else material.coarsePoles.map { pole ->
                MagneticField.Pole(world(body, pole.position), pole.charge)
            }
            sources.add(Source(sources.size, body, world(body, material.center), poles, coarse, load(body)))
        }
        if (sources.isEmpty()) {
            coreSolvers.remove(level.dimension); coreFields.remove(level.dimension)
            return
        }
        if (snapshot.cores && iron.isNotEmpty()) {
            val centers = iron.map { (material, body) -> world(body, material.center) }
            val origin = sources.first().center
            val rotation = sources.first().body?.transform?.shipToWorldRotation
            fun relative(point: Vector3dc): Vector3d {
                val result = Vector3d(point).sub(origin)
                rotation?.transformInverse(result)
                return result
            }
            val field = sources.flatMap { source -> source.poles.map { MagneticField.Pole(relative(it.position), it.charge) } }
            val moments = coreSolvers.getOrPut(level.dimension) { MagneticField.CoreSolver() }
                .solve(field, centers.map(::relative))
            val fields = ArrayList<CoreField>()
            for (i in iron.indices) {
                val (material, body) = iron[i]
                val moment = rotation?.transform(moments[i], Vector3d()) ?: Vector3d(moments[i])
                body?.transform?.worldToShip?.transformDirection(moment)
                if (moment.lengthSquared() < 1e-16) continue
                val source = MagneticFieldPacket.Source(material.ship ?: -1, material.position, Direction.UP, 1f, moment)
                val poles = source.poles().map { MagneticField.Pole(world(body, it.position), it.charge) }
                sources.add(Source(sources.size, body, centers[i], poles, poles, load(body)))
                fields.add(CoreField(material, source))
            }
            coreFields[level.dimension] = fields
        } else {
            coreSolvers.remove(level.dimension); coreFields.remove(level.dimension)
        }
        if (loads.isEmpty() && iron.none { it.second?.isStatic == false }) return
        val cells = sources.groupBy { cell(it.center) }
        val radius = sources.maxOf { it.radius }
        val force = Vector3d()
        val multipleBodies = sources.any { it.body?.id != sources.first().body?.id }
        if (multipleBodies) for (a in sources) {
            if (a.load == null) continue
            nearby(a.center, MagneticField.RANGE + radius + a.radius, cells, sources) { b ->
                val reach = MagneticField.RANGE + a.radius + b.radius
                if (b.load != null && b.index <= a.index || a.body?.id == b.body?.id ||
                    a.center.distanceSquared(b.center) > reach * reach) return@nearby
                if (a.poles === a.coarsePoles && b.poles === b.coarsePoles) {
                    applyPair(a, b, a.poles, b.poles, snapshot.strength, force)
                    return@nearby
                }
                val coarse = MagneticField.coarseWeight(a.center.distanceSquared(b.center), a.radius + b.radius)
                if (coarse < 1.0) applyPair(a, b, a.poles, b.poles, snapshot.strength * (1.0 - coarse), force)
                if (coarse > 0.0) applyPair(a, b, a.coarsePoles, b.coarsePoles, snapshot.strength * coarse, force)
            }
        }
        val sample = fieldSamples.getOrPut(level.dimension) { MagneticField.Sample() }
        val neighbours = ArrayList<Source>()
        val poles = ArrayList<MagneticField.Pole>()
        val point = Vector3d()
        val totalForce = Vector3d()
        if (!snapshot.cores) for ((material, body) in iron) {
            val center = world(body, material.center)
            val extent = body?.transform?.scaling?.length()?.times(.25) ?: sqrt(3.0) * .25
            neighbours.clear(); poles.clear()
            nearby(center, MagneticField.RANGE + radius + extent, cells, sources) { source ->
                val reach = MagneticField.RANGE + source.radius + extent
                if (source.body?.id != body?.id && source.center.distanceSquared(center) < reach * reach) {
                    neighbours.add(source)
                    poles.addAll(source.poles)
                }
            }
            if (neighbours.isEmpty()) continue
            val ironLoad = load(body)
            if (ironLoad == null && neighbours.none { it.load != null }) continue
            for (corner in 0 until 8) {
                point.set(material.center).add(if (corner and 1 == 0) -.25 else .25,
                    if (corner and 2 == 0) -.25 else .25, if (corner and 4 == 0) -.25 else .25)
                body?.transform?.shipToWorld?.transformPosition(point)
                MagneticField.sample(poles, point, sample)
                totalForce.zero()
                var index = 0
                for (sourceIndex in neighbours.indices) for (poleIndex in neighbours[sourceIndex].poles.indices) {
                    val source = neighbours[sourceIndex]
                    val pole = source.poles[poleIndex]
                    MagneticField.ironForce(sample, index++, .125, force).mul(snapshot.strength)
                    totalForce.add(force)
                    source.load?.add(pole.position, force, -1.0)
                }
                ironLoad?.add(point, totalForce)
            }
        }
        for (load in loads.values) {
            if (load.force.isFinite && load.force.lengthSquared() > 0.0) load.body.applyWorldForce(load.force)
            if (load.torque.isFinite && load.torque.lengthSquared() > 0.0) load.body.applyWorldTorque(load.torque)
        }
    }

    private fun applyPair(a: Source, b: Source, polesA: List<MagneticField.Pole>, polesB: List<MagneticField.Pole>,
                          strength: Double, force: Vector3d) {
        for (i in polesA.indices) for (j in polesB.indices) {
            val pa = polesA[i]
            val pb = polesB[j]
            MagneticField.pairForce(pa, pb, force).mul(strength)
            a.load?.add(pa.position, force)
            b.load?.add(pb.position, force, -1.0)
        }
    }

    private inline fun nearby(point: Vector3dc, radius: Double, cells: Map<Cell, List<Source>>,
                              sources: List<Source>, action: (Source) -> Unit) {
        val reach = ceil(radius / 18.0).toInt()
        if (reach > 2) {
            sources.forEach(action)
            return
        }
        val key = cell(point)
        for (x in -reach..reach) for (y in -reach..reach) for (z in -reach..reach) {
            cells[Cell(key.x + x, key.y + y, key.z + z)]?.forEach(action)
        }
    }

    private fun magnetic(state: BlockState) = state.block is MagnetBlock || ClockworkTags.AllBlockTags.MAGNETIC.matches(state)
    private fun center(pos: BlockPos) = Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
    private fun world(body: PhysShip?, point: Vector3dc) = body?.transform?.shipToWorld?.transformPosition(point, Vector3d()) ?: Vector3d(point)
    private fun cell(point: Vector3dc) = Cell(floor(point.x() / 18.0).toInt(), floor(point.y() / 18.0).toInt(), floor(point.z() / 18.0).toInt())
}
