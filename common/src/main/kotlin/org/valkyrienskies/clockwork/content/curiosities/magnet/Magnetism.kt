package org.valkyrienskies.clockwork.content.curiosities.magnet

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import org.joml.Vector3d
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

object Magnetism {
    private data class Chunk(val instance: LevelChunk, val blocks: MutableMap<BlockPos, BlockState>)
    private data class Material(val ship: Long?, val position: BlockPos, val state: BlockState)
    private data class Snapshot(val materials: List<Material>, val strength: Double)
    private data class Cell(val x: Int, val y: Int, val z: Int)
    private data class Source(val index: Int, val body: PhysShip?, val center: Vector3d, val poles: List<MagneticField.Pole>)
    private data class Load(val body: PhysShip, val force: Vector3d = Vector3d(), val torque: Vector3d = Vector3d())
    private val chunks = WeakHashMap<ServerLevel, MutableMap<Long, Chunk>>()
    private val snapshots = ConcurrentHashMap<String, Snapshot>()

    fun clear() { chunks.clear(); snapshots.clear() }

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
        if (magnetic(state)) chunk.blocks[pos.immutable()] = state else chunk.blocks.remove(pos)
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
            for ((pos, state) in chunk.blocks) materials.add(Material(ship?.id, pos, state))
        }
        val configured = ClockworkConfig.SERVER.magnetForceMultiplier
        val snapshot = Snapshot(materials, if (configured.isFinite()) configured.coerceIn(0.0, 100.0) else 1.0)
        snapshots[level.dimensionId] = snapshot
        if (level.gameTime % 5L == 0L) syncFields(level, snapshot)
    }

    private fun syncFields(level: ServerLevel, snapshot: Snapshot) {
        val players = level.players().filter(AeronautShipRequestPacket::canInspect)
        if (players.isEmpty()) return
        val sources = snapshot.materials.mapNotNull { material ->
            val block = material.state.block as? MagnetBlock ?: return@mapNotNull null
            val strength = block.fieldStrength(material.state)
            if (strength == 0.0 || snapshot.strength == 0.0) return@mapNotNull null
            val position = center(material.position)
            if (material.ship != null) {
                val ship = level.shipObjectWorld.loadedShips.getById(material.ship) ?: return@mapNotNull null
                ship.transform.shipToWorld.transformPosition(position)
            }
            MagneticFieldPacket.Source(material.ship ?: -1, material.position,
                material.state.getValue(DirectionalBlock.FACING), strength.toFloat()) to position
        }
        for (player in players) {
            val eye = Vector3d(player.x, player.eyeY, player.z)
            val nearby = sources.filter { (_, position) -> position.distanceSquared(eye) <= 48.0 * 48.0 }.map { it.first }
            val crowded = nearby.size > MagneticFieldPacket.MAX_SOURCES
            ClockworkPackets.sendTo(MagneticFieldPacket(level.dimension().location(), if (crowded) emptyList() else nearby, crowded), player)
        }
    }

    fun physicsTick(level: PhysLevel) {
        val snapshot = snapshots[level.dimension] ?: return
        if (snapshot.strength == 0.0) return
        val sources = ArrayList<Source>()
        val iron = ArrayList<Pair<Material, PhysShip?>>()
        for (material in snapshot.materials) {
            val body = material.ship?.let(level::getShipById)
            if (material.ship != null && body == null) continue
            val magnet = material.state.block as? MagnetBlock
            if (magnet == null) { iron.add(material to body); continue }
            val power = magnet.fieldStrength(material.state)
            if (power == 0.0) continue
            val local = center(material.position)
            val direction = material.state.getValue(DirectionalBlock.FACING)
            val normal = Vector3d(direction.stepX.toDouble(), direction.stepY.toDouble(), direction.stepZ.toDouble())
            val poles = MagneticField.poles(local, normal, power).map { pole ->
                MagneticField.Pole(world(body, pole.position), pole.charge)
            }
            sources.add(Source(sources.size, body, world(body, local), poles))
        }
        if (sources.isEmpty()) return
        val cells = sources.groupBy { cell(it.center) }
        val loads = HashMap<Long, Load>()
        fun apply(body: PhysShip?, point: Vector3d, force: Vector3d) {
            if (body == null || body.isStatic || !force.isFinite) return
            val load = loads.getOrPut(body.id) { Load(body) }
            load.force.add(force)
            load.torque.add(Vector3d(point).sub(body.transform.positionInWorld).cross(force))
        }
        fun neighbours(point: Vector3d): List<Source> {
            val key = cell(point)
            return buildList {
                for (x in -1..1) for (y in -1..1) for (z in -1..1)
                    cells[Cell(key.x + x, key.y + y, key.z + z)]?.let(::addAll)
            }
        }
        for (a in sources) for (b in neighbours(a.center)) {
            if (b.index <= a.index || a.body?.id == b.body?.id ||
                a.body?.isStatic != false && b.body?.isStatic != false || a.center.distanceSquared(b.center) > 18.0 * 18.0) continue
            for (pa in a.poles) for (pb in b.poles) {
                val force = MagneticField.pairForce(pa, pb).mul(snapshot.strength)
                apply(a.body, pa.position, force)
                apply(b.body, pb.position, force.negate())
            }
        }
        for ((material, body) in iron) {
            val nearby = neighbours(world(body, center(material.position))).filter {
                it.body?.id != body?.id
            }
            if (nearby.isEmpty() || body?.isStatic != false && nearby.none { it.body?.isStatic == false }) continue
            val poles = nearby.flatMap { it.poles }
            for (x in listOf(-0.25, 0.25)) for (y in listOf(-0.25, 0.25)) for (z in listOf(-0.25, 0.25)) {
                val point = world(body, center(material.position).add(x, y, z))
                val field = MagneticField.field(poles, point)
                for (source in nearby) for (pole in source.poles) {
                    val force = MagneticField.ironForce(pole, point, field, 0.125).mul(snapshot.strength)
                    apply(body, point, force)
                    apply(source.body, pole.position, force.negate())
                }
            }
        }
        for (load in loads.values) {
            if (load.force.isFinite) load.body.applyWorldForce(load.force)
            if (load.torque.isFinite) load.body.applyWorldTorque(load.torque)
        }
    }

    private fun magnetic(state: BlockState) = state.block is MagnetBlock || ClockworkTags.AllBlockTags.MAGNETIC.matches(state)
    private fun center(pos: BlockPos) = Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
    private fun world(body: PhysShip?, point: Vector3d) = body?.transform?.shipToWorld?.transformPosition(point) ?: point
    private fun cell(point: Vector3d) = Cell(floor(point.x / 18.0).toInt(), floor(point.y / 18.0).toInt(), floor(point.z / 18.0).toInt())
}
