package org.valkyrienskies.clockwork.content.logistics.gas.filter.edges

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.gas.filter.FilterScreenOpenPacket
import org.valkyrienskies.core.util.writeVec3d
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.edges.FilteredEdge
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3d
import org.valkyrienskies.mod.util.putVector3d

class ClockworkFilteredDuctEdge(
    override val type: ConnectionType,
    override val nodeA: DuctNodePos,
    override val nodeB: DuctNodePos,
    override var radius: Double = 0.125, override var length: Double = 0.5, override var currentFlowRate: Double = 0.0,
    override var filter: HashSet<GasType> = HashSet(),
    override var blacklist: Boolean = false,
    override var unloaded: Boolean = false
) : FilteredEdge {

    override fun interact(player: ServerPlayer): Boolean {
        ClockworkPackets.sendTo(FilterScreenOpenPacket(nodeA, nodeB, filter, blacklist), player)
        return true
    }

    override fun serialize(tag: CompoundTag): CompoundTag {

        var filtered = ""
        for (gas in filter) filtered += gas.resourceLocation.toString() + " "

        tag.putString("filtered", filtered)
        tag.putBoolean("blacklist", blacklist)

        return super.serialize(tag)
    }

    override fun deserialize(tag: CompoundTag) {

        val filtered = tag.getString("filtered")

        val set = HashSet<GasType>()
        for (str in filtered.split(" ")) {
            val type = GasTypeRegistry.getGasType(ResourceLocation(str.trim())) ?: continue
            set.add(type)
        }
        filter = set

        blacklist = tag.getBoolean("blacklist")

        return super.deserialize(tag)
    }

}