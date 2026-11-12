package org.valkyrienskies.clockwork.util

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlockEntity
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.assembly.ICopyableBlock
import org.valkyrienskies.mod.common.getShipManagingPos

interface ClockworkCopyableBlock : ICopyableBlock {
    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, be: BlockEntity?,
                        shipsBeingCopied: List<ServerShip>, centerPositions: Map<Long, Vector3dc>): CompoundTag? {
        val tag = be?.saveWithFullMetadata() ?: return null
        if (be is DuctBlockEntity) ClockworkDuctSchematics.capture(level, be, tag)
        val references = CompoundTag()
        for ((key, point) in ClockworkBlockReferences.positions(tag))
            references.putLong(key, level.getShipManagingPos(point)?.id ?: -1L)
        for (item in tag.getList("Items", 10)) {
            val data = (item as CompoundTag).getCompound("tag")
            for (entry in data.getCompound("selectedBlocks").getList("AABBSet", 10)) {
                val region = entry as CompoundTag
                region.putLong("ClockworkShip", level.getShipManagingPos(BlockPos.of(region.getLong("lowerCorner")))?.id ?: -1L)
            }
        }
        tag.put("ClockworkReferences", references)
        return tag
    }

    override fun onPaste(level: ServerLevel, pos: BlockPos, state: BlockState, oldShipIdToNewId: Map<Long, Long>,
                         centerPositions: Map<Long, Pair<Vector3dc, Vector3dc>>, oldJointIdToNewId: Map<Int, Int>,
                         tag: CompoundTag?): CompoundTag? = tag?.also {
        ClockworkBlockReferences.paste(it, oldShipIdToNewId, centerPositions, oldJointIdToNewId)
        ClockworkDuctSchematics.paste(level, it, oldShipIdToNewId, centerPositions)
        for (item in it.getList("Items", 10)) {
            val data = (item as CompoundTag).getCompound("tag")
            if (!data.contains("selectedBlocks")) continue
            val selected = ListTag()
            for (entry in data.getCompound("selectedBlocks").getList("AABBSet", 10)) {
                val region = entry as CompoundTag
                if (!region.contains("ClockworkShip")) continue
                val center = centerPositions[region.getLong("ClockworkShip")] ?: continue
                if (oldShipIdToNewId[region.getLong("ClockworkShip")] == null) continue
                for (key in listOf("lowerCorner", "upperCorner")) {
                    val point = BlockPos.of(region.getLong(key))
                    val moved = Vector3d(point.x.toDouble(), point.y.toDouble(), point.z.toDouble()).sub(center.first).add(center.second)
                    region.putLong(key, BlockPos.containing(moved.x, moved.y, moved.z).asLong())
                }
                region.remove("ClockworkShip")
                selected.add(region)
            }
            data.getCompound("selectedBlocks").put("AABBSet", selected)
            data.putString("selectionDimension", level.dimension().location().toString())
        }
    }
}

object ClockworkBlockReferences {
    fun positions(tag: CompoundTag): Map<String, BlockPos> = buildMap {
        for (key in listOf("ShaftOrigin", "RailOrigin"))
            if (tag.contains(key)) put(key, BlockPos.of(tag.getLong(key)))
        for (key in listOf("Controller", "shootingAtChute"))
            if (tag.contains(key)) put(key, NbtUtils.readBlockPos(tag.getCompound(key)))
        if (tag.contains("ConnectedPosX")) put("ConnectedPos", BlockPos(
            tag.getInt("ConnectedPosX"), tag.getInt("ConnectedPosY"), tag.getInt("ConnectedPosZ")))
    }

    fun paste(tag: CompoundTag, ids: Map<Long, Long>, centers: Map<Long, Pair<Vector3dc, Vector3dc>>,
              joints: Map<Int, Int>) {
        val references = tag.getCompound("ClockworkReferences")
        for ((key, oldPos) in positions(tag)) {
            val shipKey = when (key) { "ShaftOrigin" -> "ShaftShip"; "RailOrigin" -> "RailShip"; else -> null }
            val oldId = when {
                references.contains(key) -> references.getLong(key)
                shipKey != null && tag.contains(shipKey) -> tag.getLong(shipKey)
                else -> null
            }
            val newId = oldId?.let(ids::get)
            val center = oldId?.let(centers::get)
            if (newId == null || center == null) {
                if (key == "ConnectedPos") {
                    for (axis in listOf("X", "Y", "Z")) tag.remove(key + axis)
                    tag.remove("IsMain")
                } else tag.remove(key)
                shipKey?.let(tag::remove)
                continue
            }
            val point = Vector3d(oldPos.x + .5, oldPos.y + .5, oldPos.z + .5).sub(center.first).add(center.second)
            val moved = BlockPos.containing(point.x, point.y, point.z)
            when (key) {
                "ConnectedPos" -> {
                    tag.putInt(key + "X", moved.x); tag.putInt(key + "Y", moved.y); tag.putInt(key + "Z", moved.z)
                }
                "Controller", "shootingAtChute" -> tag.put(key, NbtUtils.writeBlockPos(moved))
                else -> tag.putLong(key, moved.asLong())
            }
            shipKey?.let { tag.putLong(it, newId) }
        }
        for (key in listOf("DistanceJointId", "SphericalJointId")) {
            val id = if (tag.contains(key)) joints[tag.getInt(key)] else null
            if (id == null) tag.remove(key) else tag.putInt(key, id)
        }
        tag.remove("edgeData")
        tag.remove("CondensedData")
        if (tag.contains("PhysID")) tag.putInt("PhysID", -1)
        if (tag.contains("Planned")) {
            for (key in listOf("Sequence", "Initiator", "Selection", "Regions", "Planned")) tag.remove(key)
            tag.putBoolean("Completed", true)
            tag.putBoolean("Impacted", false)
        }
        tag.remove("ClockworkReferences")
    }
}
