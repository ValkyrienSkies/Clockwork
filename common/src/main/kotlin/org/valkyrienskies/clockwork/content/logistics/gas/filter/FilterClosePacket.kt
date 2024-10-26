package org.valkyrienskies.clockwork.content.logistics.gas.filter

import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.clockwork.kelvin.api.edges.FilteredEdge
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.core.util.readVec3d
import org.valkyrienskies.core.util.writeVec3d
import kotlin.collections.HashSet

class FilterClosePacket(private val nodeA: DuctNodePos, private val nodeB: DuctNodePos, private val filter: HashSet<GasType>, private val blacklist: Boolean): C2SCWPacket {


    constructor(buffer: FriendlyByteBuf) : this(
        buffer.readVec3d(),
        buffer.readVec3d(),
        buffer.readVarIntArray().let {
            val set = HashSet<GasType>()
            for (i in it) {
                set.add(GasType.entries[i])
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
            arr[i] = gas.ordinal
        }

        buffer.writeVec3d(nodeA)
        buffer.writeVec3d(nodeB)
        buffer.writeVarIntArray(arr)
        buffer.writeBoolean(blacklist)

    }
}