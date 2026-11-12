package org.valkyrienskies.clockwork.util

import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import org.valkyrienskies.mod.common.schematic.VdexMetadata
import org.valkyrienskies.mod.common.shipObjectWorld

object ClockworkContraptionSchematics {
    fun capture(level: ServerLevel, metadata: VdexMetadata, origins: Map<Long, BlockPos>,
                blocks: Map<String, CompoundTag>): Map<String, CompoundTag> {
        val result = blocks.toMutableMap()
        for (entry in metadata.ships) {
            val ship = level.shipObjectWorld.allShips.getById(entry.previousId) ?: continue
            val origin = origins[entry.previousId] ?: continue
            val contraptions = LinkedHashSet<ControlledContraptionEntity>()
            ship.activeChunksSet.forEach { x, z ->
                if (!level.hasChunk(x, z)) return@forEach
                for (be in level.getChunk(x, z).blockEntities.values) {
                    val entity = when (be) {
                        is PropellerBearingBlockEntity -> be.propellerContraption
                        is FlapBearingBlockEntity -> be.movedContraption
                        else -> null
                    }
                    if (entity != null && entity.isAlive) contraptions.add(entity)
                }
            }
            if (contraptions.isEmpty()) continue
            val template = blocks[entry.nbtFile]?.copy() ?: continue
            val entities = template.getList("entities", 10)
            val ids = contraptions.mapTo(HashSet()) { it.uuid }
            entities.removeIf { (it as CompoundTag).getCompound("nbt").let { nbt -> nbt.hasUUID("UUID") && nbt.getUUID("UUID") in ids } }
            for (entity in contraptions) {
                val nbt = entity.saveWithoutId(CompoundTag())
                nbt.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entity.type).toString())
                nbt.remove("VSContraptionSegment"); nbt.remove("VSContraptionBody")
                nbt.put("ClockworkAnchorOffset", NbtUtils.writeBlockPos(entity.contraption.anchor.subtract(entity.blockPosition())))
                val point = entity.position().subtract(origin.x.toDouble(), origin.y.toDouble(), origin.z.toDouble())
                val block = BlockPos.containing(point)
                entities.add(CompoundTag().apply {
                    put("pos", ListTag().apply { add(DoubleTag.valueOf(point.x)); add(DoubleTag.valueOf(point.y)); add(DoubleTag.valueOf(point.z)) })
                    put("blockPos", ListTag().apply { add(IntTag.valueOf(block.x)); add(IntTag.valueOf(block.y)); add(IntTag.valueOf(block.z)) })
                    put("nbt", nbt)
                })
            }
            template.put("entities", entities)
            result[entry.nbtFile] = template
        }
        return result
    }
}
