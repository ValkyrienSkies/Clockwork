package org.valkyrienskies.clockwork.content.logistics.gas.smart

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.SmartDuctEdge
import org.valkyrienskies.kelvin.api.edges.SmartEdge.FilterType

class ClockworkSmartEdge(
    type: ConnectionType, nodeA: DuctNodePos, nodeB: DuctNodePos,
    radius: Double, length: Double, currentFlowRate: Double,
) : SmartDuctEdge(type, nodeA, nodeB, radius, length, currentFlowRate, unloaded = false) {
    init {
        filter = FilterType.PRESSURE
        comparisonValue = 101_325.0
    }

    override fun interact(player: ServerPlayer): Boolean {
        val settings = if (filter == FilterType.NONE) {
            SmartDuctSettings(FilterType.PRESSURE, 101_325.0, false, HashSet(gasFilter), filterBlacklist)
        } else SmartDuctSettings(filter, comparisonValue, moreThan, HashSet(gasFilter), filterBlacklist)
        ClockworkPackets.sendTo(SmartScreenOpenPacket(nodeA, nodeB, settings), player)
        return true
    }

    override fun deserialize(tag: CompoundTag) {
        super.deserialize(tag)
        // Older Clockwork saves also wrote this key. Prefer Kelvin's key when both exist.
        if (!tag.contains("filter") && tag.contains("filterOrdinal")) {
            filter = FilterType.entries.getOrElse(tag.getInt("filterOrdinal")) { FilterType.PRESSURE }
        }
    }
}
