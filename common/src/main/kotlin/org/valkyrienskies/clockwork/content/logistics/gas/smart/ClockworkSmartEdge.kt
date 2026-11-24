package org.valkyrienskies.clockwork.content.logistics.gas.smart

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.edges.IEdgeFlowGate
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.kelvin.api.simulation.EdgeFlowContext
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry

class ClockworkSmartEdge(
    type: ConnectionType, nodeA: DuctNodePos, nodeB: DuctNodePos,
    radius: Double, length: Double, currentFlowRate: Double,
) : PipeDuctEdge(type, nodeA, nodeB, radius, length, currentFlowRate), IEdgeFlowGate {
    enum class FilterType {
        NONE,
        PRESSURE,
        TEMPERATURE,
        HEAT_ENERGY,
        MASS;
    }

    var filter = FilterType.PRESSURE
    var comparisonValue = 101_325.0
    var moreThan = false
    val gasFilter = hashSetOf<GasType>()
    var filterBlacklist = true

    override fun allowsFlow(context: EdgeFlowContext, flowRate: Double): Boolean {
        if (filter == FilterType.NONE) return true
        val receivingNode = if (flowRate > 0.0) context.nodeB else context.nodeA
        val value = measuredValue(receivingNode.pressure, receivingNode.temperature, receivingNode.gasMasses)
        return if (moreThan) value > comparisonValue else value < comparisonValue
    }

    fun measuredValue(pressure: Double, temperature: Double, masses: Map<GasType, Double>): Double {
        var totalMoles = 0.0
        var selectedMoles = 0.0
        var selectedMass = 0.0
        var selectedCapacity = 0.0
        for ((gas, mass) in masses) {
            if (mass <= 0.0) continue
            val moles = gas.massToMoles(mass)
            totalMoles += moles
            if ((gas in gasFilter) == filterBlacklist) continue
            selectedMoles += moles
            selectedMass += mass
            selectedCapacity += mass * gas.specificHeatCapacity * 1000.0 / gas.adiabaticIndex
        }
        return when (filter) {
            FilterType.NONE -> 0.0
            FilterType.PRESSURE -> if (totalMoles > 0.0) pressure * selectedMoles / totalMoles else 0.0
            FilterType.TEMPERATURE -> if (selectedMass > 0.0) temperature else 0.0
            FilterType.HEAT_ENERGY -> selectedCapacity * temperature
            FilterType.MASS -> selectedMass
        }
    }

    override fun interact(player: ServerPlayer): Boolean {
        val settings = if (filter == FilterType.NONE) {
            SmartDuctSettings(FilterType.PRESSURE, 101_325.0, false, HashSet(gasFilter), filterBlacklist)
        } else SmartDuctSettings(filter, comparisonValue, moreThan, HashSet(gasFilter), filterBlacklist)
        ClockworkPackets.sendTo(SmartScreenOpenPacket(nodeA, nodeB, settings), player)
        return true
    }

    override fun serialize(tag: CompoundTag): CompoundTag {
        tag.putInt("filter", filter.ordinal)
        tag.putDouble("comparisonValue", comparisonValue)
        tag.putBoolean("moreThan", moreThan)
        tag.put("gasFilter", ListTag().apply {
            gasFilter.forEach { add(StringTag.valueOf(it.resourceLocation.toString())) }
        })
        tag.putBoolean("filterBlacklist", filterBlacklist)
        return super.serialize(tag)
    }

    override fun deserialize(tag: CompoundTag) {
        filter = FilterType.entries.getOrElse(tag.getInt("filter")) { FilterType.NONE }
        if (!tag.contains("filter") && tag.contains("filterOrdinal")) {
            filter = FilterType.entries.getOrElse(tag.getInt("filterOrdinal")) { FilterType.PRESSURE }
        }
        comparisonValue = tag.getDouble("comparisonValue")
        moreThan = tag.getBoolean("moreThan")
        gasFilter.clear()
        for (entry in tag.getList("gasFilter", Tag.TAG_STRING.toInt())) {
            val id = ResourceLocation.tryParse(entry.asString) ?: continue
            GasTypeRegistry.getGasType(id)?.let { gasFilter.add(it) }
        }
        filterBlacklist = !tag.contains("filterBlacklist") || tag.getBoolean("filterBlacklist")
        super.deserialize(tag)
    }
}
