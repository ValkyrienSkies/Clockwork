package org.valkyrienskies.clockwork.content.logistics.gas.redstone

import net.createmod.catnip.nbt.NBTHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry

class RedstoneDuctConditional (var type: ConditionalType, var moreThan: Boolean,
                               var comparisonValue: Double, var filter: List<GasType> = listOf(), var filterBlacklist: Boolean = true) {


    fun passes(network: DuctNetwork<*>, ductNodePos: DuctNodePos): Boolean {
        if (type.isNone()) return false
        val gasMasses = network.getGasMassAt(ductNodePos)
        val filteredGasMasses = gasMasses.filter { (filter.contains(it.key) && !filterBlacklist) || (!filter.contains(it.key) && filterBlacklist)  }


        val toCompare = when (type) {
            ConditionalType.TEMPERATURE -> network.getTemperatureAt(ductNodePos)
            ConditionalType.HEAT_ENERGY -> network.getHeatEnergy(ductNodePos)
            ConditionalType.PRESSURE ->  {
                val pressure = network.getPressureAt(ductNodePos)

                val totalMoles = gasMasses.entries.sumOf { it.key.massToMoles(it.value) }
                val filteredMoles = filteredGasMasses.entries.sumOf { it.key.massToMoles(it.value) }

                (pressure * filteredMoles / totalMoles)
            }
            ConditionalType.MASS -> {
                filteredGasMasses.values.sum()
            }
            ConditionalType.NONE -> 0.0
        }

        return (moreThan && toCompare > comparisonValue) || (!moreThan && toCompare < comparisonValue)

    }

    fun serialize(tag: CompoundTag): CompoundTag {
        NBTHelper.writeEnum(tag, "ConditionalType", type)
        tag.putBoolean("MoreThan", moreThan)
        tag.putDouble("ComparisonValue", comparisonValue)

        val filterTag = CompoundTag()
        filter.forEach { filterTag.putString(it.name, it.resourceLocation.toString()) }
        tag.put("FilterList", filterTag)
        tag.putBoolean("FilterBlacklist", filterBlacklist)

        return tag
    }

    companion object {
        fun deserialize(tag: CompoundTag): RedstoneDuctConditional {
            val type = NBTHelper.readEnum(tag, "ConditionalType", ConditionalType::class.java)
            val moreThan = tag.getBoolean("MoreThan")
            val comparisonValue = tag.getDouble("ComparisonValue")

            // filterTag might be null if pasting from a schematic, this is just in case
            val filterTag = tag.get("FilterList") as? CompoundTag
            val filter: MutableList<GasType> = mutableListOf()
            filterTag?.allKeys?.forEach {
                val resourceLocation  = ResourceLocation.read(filterTag.getString(it)).result()
                if (!resourceLocation.isEmpty) {
                    val gasType = GasTypeRegistry.getGasType(resourceLocation.get())
                    if (gasType != null) {
                        filter.add(gasType)
                    }
                }
            }
            val blacklist = tag.getBoolean("FilterBlacklist")
            return RedstoneDuctConditional(type, moreThan, comparisonValue, filter, blacklist)
        }
    }

    enum class ConditionalType {
        NONE,
        TEMPERATURE,
        HEAT_ENERGY,
        PRESSURE,
        MASS;

        fun isNone(): Boolean {
            return this == NONE
        }

        fun next(): ConditionalType {
            return when (this) {
                NONE -> TEMPERATURE
                TEMPERATURE -> HEAT_ENERGY
                HEAT_ENERGY -> PRESSURE
                PRESSURE -> MASS
                MASS -> NONE
            }
        }
    }
}