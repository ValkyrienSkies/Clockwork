package org.valkyrienskies.clockwork.content.logistics.gas.smart

import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.edges.SmartEdge.FilterType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry


data class SmartDuctSettings(
    var filter: FilterType,
    var comparisonValue: Double,
    var moreThan: Boolean,
    val gasFilter: HashSet<GasType> = hashSetOf(),
    var filterBlacklist: Boolean = true,
) {
    fun write(buffer: FriendlyByteBuf) {
        buffer.writeEnum(filter)
        buffer.writeDouble(comparisonValue)
        buffer.writeBoolean(moreThan)
        buffer.writeBoolean(filterBlacklist)
        buffer.writeVarInt(gasFilter.size)
        gasFilter.forEach { buffer.writeResourceLocation(it.resourceLocation) }
    }

    companion object {
        fun read(buffer: FriendlyByteBuf): SmartDuctSettings {
            val settings = SmartDuctSettings(buffer.readEnum(FilterType::class.java), buffer.readDouble(), buffer.readBoolean())
            settings.filterBlacklist = buffer.readBoolean()
            val count = buffer.readVarInt()
            require(count in 0..4096) { "Invalid smart duct gas filter size" }
            repeat(count) {
                GasTypeRegistry.getGasType(buffer.readResourceLocation())?.let { settings.gasFilter.add(it) }
            }
            return settings
        }
    }
}