package org.valkyrienskies.clockwork.content.kinetics.combobulator

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.structure.BoundingBox
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import java.util.WeakHashMap

object ThinkableSchematicCapture {
    const val CONNECTIONS = ThinkableSchematicBehaviour.KEY

    // Schematic & Quill exports on the client; instant exports also run on the server.
    // Keep only anchors here so this common code has no client class dependencies.
    private val clientLinks = WeakHashMap<Level, List<Pair<WandAnchor, WandAnchor>>>()

    fun acceptLinks(level: Level, packet: CompoundTag) {
        val complete = packet.contains("schematicThinkables", Tag.TAG_LIST.toInt())
        clientLinks[level] = packet.getList(if (complete) "schematicThinkables" else "links", Tag.TAG_COMPOUND.toInt())
            .filterIsInstance<CompoundTag>().filter { complete || it.getBoolean("thinkable") }
            .map {
                if (complete) WandAnchor(BlockPos.of(it.getLong("Source")), Direction.from3DDataValue(it.getInt("SourceFace"))) to
                    WandAnchor(BlockPos.of(it.getLong("Sink")), Direction.from3DDataValue(it.getInt("SinkFace")))
                else WandAnchor.load(it.getCompound("a")) to WandAnchor.load(it.getCompound("b"))
            }
    }

    @JvmStatic
    fun capture(level: Level, first: BlockPos, second: BlockPos, data: CompoundTag): CompoundTag {
        val bounds = BoundingBox.fromCorners(first, second)
        val origin = BlockPos(bounds.minX(), bounds.minY(), bounds.minZ())
        val blocks = mutableMapOf<BlockPos, CompoundTag>()
        val sources = mutableSetOf<BlockPos>()
        val pending = mutableListOf<Pair<WandAnchor, WandAnchor>>()
        for (entry in data.getList("blocks", Tag.TAG_COMPOUND.toInt())) {
            val block = entry as CompoundTag
            val position = block.getList("pos", Tag.TAG_INT.toInt())
            if (position.size != 3) continue
            val pos = origin.offset(position.getInt(0), position.getInt(1), position.getInt(2))
            if (!bounds.isInside(pos)) continue
            // Sources can be adapted Create devices or sensor blocks without block entities.
            if (ThinkableConnections.device(level, pos) != null) sources.add(pos)
            if (!block.contains("nbt", Tag.TAG_COMPOUND.toInt())) continue
            val nbt = block.getCompound("nbt")
            val savedConnections = nbt.getList(CONNECTIONS, Tag.TAG_COMPOUND.toInt())
            nbt.remove(CONNECTIONS)
            if (level.getBlockEntity(pos) !is IThinkable) continue
            blocks[pos] = nbt
            for (saved in savedConnections.take(6)) {
                val connection = saved as CompoundTag
                val sourceFace = connection.getInt("SourceFace")
                val sinkFace = connection.getInt("SinkFace")
                if (!connection.contains("Source", Tag.TAG_LONG.toInt()) || sourceFace !in 0..5 || sinkFace !in 0..5) continue
                pending += WandAnchor(pos.offset(BlockPos.of(connection.getLong("Source"))), Direction.from3DDataValue(sourceFace)) to
                    WandAnchor(pos, Direction.from3DDataValue(sinkFace))
            }
        }

        val links = if (level is ServerLevel) {
            WandLinks.get(level).links.values.asSequence().filter { it.thinkable }.map { it.a to it.b }
        } else {
            clientLinks[level].orEmpty().asSequence()
        }
        for ((source, sink) in links + pending.asSequence()) {
            if (source.pos == sink.pos || source.pos !in sources) continue
            val nbt = blocks[sink.pos] ?: continue
            val connections = nbt.getList(CONNECTIONS, Tag.TAG_COMPOUND.toInt())
            if (connections.size >= 6 || connections.any {
                    (it as CompoundTag).getInt("SinkFace") == sink.face.ordinal
                }) continue
            connections.add(CompoundTag().also {
                it.putLong("Source", source.pos.subtract(sink.pos).asLong())
                it.putInt("SourceFace", source.face.ordinal)
                it.putInt("SinkFace", sink.face.ordinal)
            })
            nbt.put(CONNECTIONS, connections)
        }
        return data
    }
}
