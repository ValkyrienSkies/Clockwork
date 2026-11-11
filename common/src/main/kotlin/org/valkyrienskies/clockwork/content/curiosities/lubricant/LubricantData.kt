package org.valkyrienskies.clockwork.content.curiosities.lubricant

import net.minecraft.core.BlockPos
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

class LubricantData : SavedData() {
    private val sections = HashMap<Long, MutableMap<Long, Block>>()
    private val changed = HashSet<Long>()
    private var revision = 0L
    private val viewers = HashMap<java.util.UUID, Pair<Long, Long>>()

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
        }
        revision++
        setDirty()
        return true
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
        set(from, block, false)
        set(to, block, true)
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
            orphaned.forEach(sections::remove)
            revision++
            setDirty()
        }
        val active = HashSet<java.util.UUID>()
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
            ClockworkPackets.sendTo(LubricantSyncPacket(level.dimension().location(), visible.toLongArray()), player)
            viewers[player.uuid] = key
        }
        viewers.keys.retainAll(active)
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
