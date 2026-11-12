package org.valkyrienskies.clockwork.content.curiosities.lubricant

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.saveddata.SavedData
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.mod.common.getWorldCoordinates
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.core.api.ships.Ship
import java.util.ArrayDeque
import java.util.UUID

class LubricantData : SavedData() {
    private val sections = HashMap<Long, MutableMap<Long, Block>>()
    private val changed = HashSet<Long>()
    private val areas = HashMap<Long, MutableSet<Long>>()
    private val memberships = HashMap<Long, MutableSet<Long>>()
    private var nextArea = 1L
    private var revision = 0L
    private val viewers = HashMap<UUID, Pair<Long, Long>>()

    fun contains(pos: Long, block: Block): Boolean = sections[section(pos)]?.get(pos) === block
    fun hasSection(x: Int, y: Int, z: Int) = sections.containsKey(SectionPos.asLong(x, y, z))

    fun set(pos: BlockPos, block: Block, coated: Boolean): Boolean {
        val key = pos.asLong()
        val section = section(key)
        val existing = sections[section]
        if (coated) {
            if (existing?.get(key) === block) return false
            sections.getOrPut(section) { HashMap() }[key] = block
        } else {
            if (existing?.remove(key) == null) return false
            if (existing.isEmpty()) sections.remove(section)
            forgetAreas(key)
        }
        revision++
        setDirty()
        return true
    }

    fun addArea(positions: Collection<BlockPos>) {
        val blocks = positions.map { it.asLong() }.filter { sections[section(it)]?.containsKey(it) == true }.toSet()
        if (blocks.isEmpty() || areas.values.any { it == blocks }) return
        val id = nextArea++
        areas[id] = blocks.toMutableSet()
        for (pos in blocks) memberships.getOrPut(pos) { HashSet() }.add(id)
        revision++
        setDirty()
    }

    fun areaAt(pos: BlockPos): Set<Long> {
        memberships[pos.asLong()]?.maxOrNull()?.let { return areas[it]?.toSet() ?: emptySet() }
        val result = HashSet<Long>()
        val queue = ArrayDeque<BlockPos>()
        queue.add(pos)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val key = current.asLong()
            if (memberships.containsKey(key) || sections[section(key)]?.containsKey(key) != true || !result.add(key)) continue
            for (direction in Direction.values()) queue.add(current.relative(direction))
        }
        return result
    }

    fun removeArea(level: ServerLevel, pos: BlockPos, allowed: (BlockPos) -> Boolean): Int {
        val id = memberships[pos.asLong()]?.maxOrNull()
        val selected = areaAt(pos)
        if (selected.isEmpty() || selected.any { !allowed(BlockPos.of(it)) }) return 0
        for (key in selected) {
            val block = sections[section(key)]?.get(key) ?: continue
            if (id != null) {
                memberships[key]?.remove(id)
                if (memberships[key]?.isNotEmpty() == true) continue
            }
            val cursor = BlockPos.of(key)
            set(cursor, block, false)
            if (level.hasChunkAt(cursor) && level.getBlockState(cursor).block === block)
                LubricantMaterials.refresh(level, cursor, false)
        }
        if (id != null) areas.remove(id)
        revision++
        setDirty()
        return selected.size
    }

    private fun forgetAreas(pos: Long) {
        for (id in memberships.remove(pos).orEmpty()) {
            val members = areas[id] ?: continue
            members.remove(pos)
            if (members.isEmpty()) areas.remove(id)
        }
    }

    fun blockChanged(pos: BlockPos) {
        if (sections[section(pos.asLong())]?.containsKey(pos.asLong()) == true) changed.add(pos.asLong())
    }

    fun refreshShip(level: ServerLevel, ship: Ship) {
        for (entries in sections.values) for ((key, block) in entries) {
            val pos = BlockPos.of(key)
            if (level.getShipManagingPos(pos)?.id == ship.id && level.hasChunkAt(pos) && level.getBlockState(pos).block === block)
                LubricantMaterials.refresh(level, pos, true)
        }
    }

    fun move(level: ServerLevel, from: BlockPos, to: BlockPos) {
        if (from == to) return
        val block = sections[section(from.asLong())]?.get(from.asLong()) ?: return
        if (level.getBlockState(to).block !== block) return
        val regionIds = memberships[from.asLong()]?.toSet().orEmpty()
        set(from, block, false)
        set(to, block, true)
        for (id in regionIds) {
            areas.getOrPut(id) { HashSet() }.add(to.asLong())
            memberships.getOrPut(to.asLong()) { HashSet() }.add(id)
        }
        changed.remove(to.asLong())
        LubricantMaterials.refresh(level, to, true)
    }

    fun tick(level: ServerLevel) {
        for (key in changed) {
            val pos = BlockPos.of(key)
            if (!level.hasChunkAt(pos)) continue
            val block = sections[section(key)]?.get(key) ?: continue
            set(pos, block, false)
            if (level.getBlockState(pos).block === block) LubricantMaterials.refresh(level, pos, false)
        }
        changed.clear()
        if (level.gameTime % 10L != 0L) return
        val orphaned = sections.keys.filter {
            val pos = SectionPos.of(it).origin()
            level.isBlockInShipyard(pos) && level.getShipManagingPos(pos) == null
        }
        if (orphaned.isNotEmpty()) {
            orphaned.forEach { key -> sections.remove(key)?.keys?.forEach(::forgetAreas) }
            revision++
            setDirty()
        }
        val active = HashSet<UUID>()
        for (player in level.players()) {
            if (player.mainHandItem.item !is LubricantItem && player.offhandItem.item !is LubricantItem) continue
            active.add(player.uuid)
            val key = revision to player.blockPosition().asLong()
            if (viewers[player.uuid] == key && level.gameTime % 40L != 0L) continue
            val visible = ArrayList<Long>()
            for ((section, entries) in sections) {
                val center = SectionPos.of(section).origin().offset(8, 8, 8)
                val world = level.getWorldCoordinates(center, Vector3d(center.x.toDouble(), center.y.toDouble(), center.z.toDouble()))
                if (world.distanceSquared(player.x, player.y, player.z) > 80.0 * 80.0) continue
                for (pos in entries.keys) {
                    if (visible.size >= MAX_VISIBLE) break
                    visible.add(pos)
                }
                if (visible.size >= MAX_VISIBLE) break
            }
            ClockworkPackets.sendTo(LubricantSyncPacket(level.dimension().location(), visible.toLongArray(),
                visible.map { memberships[it]?.maxOrNull() ?: 0L }.toLongArray()), player)
            viewers[player.uuid] = key
        }
        viewers.keys.retainAll(active)
    }

    fun captureSchematic(level: ServerLevel, origins: Map<Long, BlockPos>): CompoundTag {
        val included = HashSet<Long>()
        val blocks = ListTag()
        for (entries in sections.values) for ((key, block) in entries) {
            val pos = BlockPos.of(key)
            val ship = level.getShipManagingPos(pos)?.id ?: continue
            val origin = origins[ship] ?: continue
            included.add(key)
            blocks.add(CompoundTag().apply {
                putLong("Source", key); putLong("Ship", ship)
                putLong("Offset", pos.subtract(origin).asLong())
                putString("Block", BuiltInRegistries.BLOCK.getKey(block).toString())
            })
        }
        return CompoundTag().apply {
            put("Blocks", blocks)
            put("Areas", ListTag().also { list -> areas.values.forEach { area ->
                val members = area.filter { it in included }
                if (members.isNotEmpty()) list.add(CompoundTag().apply { putLongArray("Blocks", members) })
            } })
        }
    }

    fun pasteSchematic(level: ServerLevel, tag: CompoundTag, destinations: Map<Long, Pair<Long, BlockPos>>) {
        val moved = HashMap<Long, BlockPos>()
        for (entry in tag.getList("Blocks", 10)) {
            val data = entry as CompoundTag
            val corner = destinations[data.getLong("Ship")]?.second ?: continue
            val pos = corner.offset(BlockPos.of(data.getLong("Offset")))
            val id = ResourceLocation.tryParse(data.getString("Block")) ?: continue
            val block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null) ?: continue
            if (!level.hasChunkAt(pos) || level.getBlockState(pos).block !== block) continue
            moved[data.getLong("Source")] = pos
            set(pos, block, true)
            changed.remove(pos.asLong())
            LubricantMaterials.refresh(level, pos, true)
        }
        for (area in tag.getList("Areas", 10))
            addArea((area as CompoundTag).getLongArray("Blocks").asSequence().mapNotNull { moved[it] }.toList())
    }

    override fun save(tag: CompoundTag): CompoundTag {
        val entries = ListTag()
        for (section in sections.values) for ((pos, block) in section) {
            entries.add(CompoundTag().apply {
                putLong("Pos", pos)
                putString("Block", BuiltInRegistries.BLOCK.getKey(block).toString())
            })
        }
        tag.put("Blocks", entries)
        tag.putLong("NextArea", nextArea)
        tag.put("Areas", ListTag().apply {
            for ((id, blocks) in areas) add(CompoundTag().apply {
                putLong("Id", id)
                putLongArray("Blocks", blocks.toLongArray())
            })
        })
        return tag
    }

    companion object {
        const val MAX_VISIBLE = 8192
        private fun section(pos: Long) = SectionPos.asLong(BlockPos.getX(pos) shr 4, BlockPos.getY(pos) shr 4, BlockPos.getZ(pos) shr 4)

        @JvmStatic
        fun get(level: ServerLevel): LubricantData = level.dataStorage.computeIfAbsent(::load, ::LubricantData, "clockwork_lubricant")

        fun load(tag: CompoundTag): LubricantData = LubricantData().apply {
            for (entry in tag.getList("Blocks", 10)) {
                val data = entry as CompoundTag
                val id = ResourceLocation.tryParse(data.getString("Block")) ?: continue
                if (!BuiltInRegistries.BLOCK.containsKey(id)) continue
                val pos = data.getLong("Pos")
                sections.getOrPut(section(pos)) { HashMap() }[pos] = BuiltInRegistries.BLOCK.get(id)
            }
            nextArea = maxOf(1L, tag.getLong("NextArea"))
            for (entry in tag.getList("Areas", 10)) {
                val area = entry as CompoundTag
                val id = area.getLong("Id")
                val blocks = area.getLongArray("Blocks").filter { sections[section(it)]?.containsKey(it) == true }.toMutableSet()
                if (id <= 0 || blocks.isEmpty()) continue
                areas[id] = blocks
                for (pos in blocks) memberships.getOrPut(pos) { HashSet() }.add(id)
                nextArea = maxOf(nextArea, id + 1)
            }
        }

        @JvmStatic
        fun moveBlock(level: Level, from: BlockPos, to: BlockPos) {
            if (level is ServerLevel) get(level).move(level, from, to)
        }

        @JvmStatic
        fun moveRegion(level: ServerLevel, blocks: Set<BlockPos>, origin: BlockPos, destination: BlockPos) {
            val offset = destination.subtract(origin)
            val data = get(level)
            for (pos in blocks) data.move(level, pos, pos.offset(offset))
        }
    }
}
