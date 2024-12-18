package org.valkyrienskies.clockwork.content.logistics.gas.filter

import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.edges.FilteredEdge
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.core.util.readVec3d
import org.valkyrienskies.core.util.writeVec3d
import org.valkyrienskies.kelvin.impl.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3d
import kotlin.collections.HashSet

class FilterClosePacket(private val nodeA: DuctNodePos, private val nodeB: DuctNodePos, private val filter: HashSet<GasType>, private val blacklist: Boolean): C2SCWPacket {


    constructor(buffer: FriendlyByteBuf) : this(
        buffer.readVec3d().toDuctNodePos(buffer.readResourceLocation()),
        buffer.readVec3d().toDuctNodePos(buffer.readResourceLocation()),
        buffer.readVarIntArray().let {
            val set = HashSet<GasType>()
            for (i in it) {
                set.add(GasTypeRegistry.GAS_TYPES.values.toList()[i])
            }
            set
        },
        buffer.readBoolean()
    )


    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val edge = ClockworkMod.getKelvin().edges.get(Pair(nodeA, nodeB)) as FilteredEdge? ?: return@enqueueWork

            edge.modFilter(filter, blacklist)
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        val arr = IntArray(filter.size)
        for ((i, gas) in filter.withIndex()) {
            arr[i] = GasTypeRegistry.GAS_TYPES.values.toList().indexOf(gas)
        }

        buffer.writeVec3d(nodeA.toMinecraft().toVector3d())
        buffer.writeResourceLocation(nodeA.dimensionId)
        buffer.writeVec3d(nodeB.toMinecraft().toVector3d())
        buffer.writeResourceLocation(nodeB.dimensionId)
        buffer.writeVarIntArray(arr)
        buffer.writeBoolean(blacklist)

    }
}