package org.valkyrienskies.clockwork.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlockEntity
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.mod.common.getShipManagingPos

object ClockworkDuctSchematics {
    private val mapper = ObjectMapper().registerKotlinModule()

    fun capture(level: ServerLevel, be: DuctBlockEntity, tag: CompoundTag) {
        fun node(point: DuctNodePos) = CompoundTag().apply {
            putDouble("X", point.x); putDouble("Y", point.y); putDouble("Z", point.z)
            putLong("Ship", level.getShipManagingPos(BlockPos.containing(point.x, point.y, point.z))?.id ?: -1L)
        }
        tag.put("ClockworkEdges", ListTag().also { list -> be.edgeData.forEach { (edge, settings) ->
            list.add(CompoundTag().apply {
                put("A", node(edge.first)); put("B", node(edge.second)); put("Settings", settings.copy())
            })
        } })
    }

    fun paste(level: ServerLevel, tag: CompoundTag, ids: Map<Long, Long>,
              centers: Map<Long, Pair<Vector3dc, Vector3dc>>) {
        if (!tag.contains("ClockworkEdges")) return
        fun node(saved: CompoundTag): DuctNodePos? {
            val id = saved.getLong("Ship")
            if (ids[id] == null) return null
            val center = centers[id] ?: return null
            val point = Vector3d(saved.getDouble("X"), saved.getDouble("Y"), saved.getDouble("Z"))
                .sub(center.first).add(center.second)
            return DuctNodePos(point.x, point.y, point.z, level.dimension().location())
        }
        val edges = CompoundTag()
        for (entry in tag.getList("ClockworkEdges", 10)) {
            val saved = entry as CompoundTag
            val a = node(saved.getCompound("A")) ?: continue
            val b = node(saved.getCompound("B")) ?: continue
            edges.put(mapper.writeValueAsString(DuctBlockEntity.EdgePos(a, b)), saved.getCompound("Settings"))
        }
        tag.put("edgeData", edges)
        tag.remove("ClockworkEdges")
    }
}
