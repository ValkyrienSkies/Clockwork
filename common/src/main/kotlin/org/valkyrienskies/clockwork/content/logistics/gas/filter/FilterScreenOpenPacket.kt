package org.valkyrienskies.clockwork.content.logistics.gas.filter

import net.createmod.catnip.gui.ScreenOpener
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.edges.FilteredEdge
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.core.util.readVec3d
import org.valkyrienskies.core.util.writeVec3d
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3d

class FilterScreenOpenPacket(private val nodeA: DuctNodePos, private val nodeB: DuctNodePos, private val filter: HashSet<GasType>, private val blacklist: Boolean): S2CCWPacket {



    constructor(buffer: FriendlyByteBuf) : this(
        buffer.readVec3d().toDuctNodePos(buffer.readResourceLocation()),
        buffer.readVec3d().toDuctNodePos(buffer.readResourceLocation()),
        buffer.readUtf().let {
            val set = HashSet<GasType>()
            for (str in it.split(" ")) {
                val type = GasTypeRegistry.getGasType(ResourceLocation(str.trim())) ?: continue
                set.add(type)
            }
            set
        },
        buffer.readBoolean()
    )


    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            ScreenOpener.open(FilterScreen(nodeA, nodeB, filter, blacklist))
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {

        var tag = ""
        for (gas in filter) tag += gas.resourceLocation.toString() + " "

        buffer.writeVec3d(nodeA.toMinecraft().toVector3d())
        buffer.writeResourceLocation(nodeA.dimensionId)
        buffer.writeVec3d(nodeB.toMinecraft().toVector3d())
        buffer.writeResourceLocation(nodeB.dimensionId)
        buffer.writeUtf(tag)
        buffer.writeBoolean(blacklist)

    }
}

