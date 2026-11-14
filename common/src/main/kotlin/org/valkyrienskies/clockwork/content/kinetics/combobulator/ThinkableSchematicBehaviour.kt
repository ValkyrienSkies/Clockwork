package org.valkyrienskies.clockwork.content.kinetics.combobulator

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor

/** Only pending schematic cables live here; established cables belong to WandLinks. */
class ThinkableSchematicBehaviour(be: SmartBlockEntity) : BlockEntityBehaviour(be) {
    private var pending = ListTag()

    override fun getType() = TYPE
    override fun isSafeNBT() = true

    override fun read(nbt: CompoundTag, clientPacket: Boolean) {
        if (clientPacket) return
        pending = ListTag().also { list ->
            nbt.getList(KEY, 10).filterIsInstance<CompoundTag>().take(6).filter { entry ->
                entry.contains("Source", 4) && entry.contains("SourceFace", 3) && entry.contains("SinkFace", 3) &&
                    entry.getInt("SourceFace") in 0..5 &&
                    entry.getInt("SinkFace") in 0..5 &&
                    BlockPos.of(entry.getLong("Source")).distSqr(BlockPos.ZERO) <= (ThinkableItem.MAX_RANGE + 2) * (ThinkableItem.MAX_RANGE + 2)
            }.forEach { list.add(it.copy()) }
        }
    }

    override fun write(nbt: CompoundTag, clientPacket: Boolean) {
        if (!clientPacket && pending.isNotEmpty()) nbt.put(KEY, pending.copy())
    }

    override fun lazyTick() {
        val level = world as? ServerLevel ?: return
        if (blockEntity.isRemoved || level.getBlockEntity(pos) !== blockEntity) return
        val changed = pending.removeIf { raw ->
            val entry = raw as CompoundTag
            val sink = WandAnchor.at(level, pos, Direction.from3DDataValue(entry.getInt("SinkFace")))
            val sourcePos = pos.offset(BlockPos.of(entry.getLong("Source")))
            if (!level.hasChunkAt(sourcePos)) return@removeIf false
            val source = WandAnchor.at(level, sourcePos, Direction.from3DDataValue(entry.getInt("SourceFace")))
            // A cannon may build the source much later, or pause across a world reload.
            ThinkableConnections.incoming(level, sink) != null || ThinkableConnections.connect(level, source, sink)
        }
        if (changed) blockEntity.setChanged()
    }

    companion object {
        const val KEY = "ClockworkThinkableConnections"
        @JvmField val TYPE = BehaviourType<ThinkableSchematicBehaviour>()

        /** Called once by StructureTemplate, before Create sanitizes and prints each block. */
        @JvmStatic
        fun transform(original: CompoundTag, mirror: Mirror, rotation: Rotation, combobulator: Boolean): CompoundTag {
            val tag = original.copy()
            for (raw in tag.getList(KEY, 10)) {
                val entry = raw as CompoundTag
                val offset = BlockPos.of(entry.getLong("Source"))
                entry.putLong("Source", StructureTemplate.transform(offset, mirror, rotation, BlockPos.ZERO).asLong())
                for (key in listOf("SourceFace", "SinkFace")) {
                    val face = Direction.from3DDataValue(entry.getInt(key))
                    entry.putInt(key, rotation.rotate(mirror.mirror(face)).ordinal)
                }
            }
            // Front/back follow the block state; reflections exchange its relative left/right.
            if (combobulator && mirror != Mirror.NONE) {
                for (raw in tag.getCompound("Program").getList("Channels", 10)) {
                    val channel = raw as CompoundTag
                    when (channel.getString("Face")) {
                        "LEFT" -> channel.putString("Face", "RIGHT")
                        "RIGHT" -> channel.putString("Face", "LEFT")
                    }
                }
            }
            return tag
        }
    }
}
