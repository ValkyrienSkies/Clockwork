package org.valkyrienskies.clockwork.content.logistics.gas.smart

import net.createmod.catnip.gui.ScreenOpener
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.filter.FilterScreen
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.edges.FilteredEdge
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.core.util.readVec3d
import org.valkyrienskies.core.util.writeVec3d
import org.valkyrienskies.kelvin.api.edges.SmartEdge.FilterType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3d

class SmartScreenOpenPacket(private val nodeA: DuctNodePos, private val nodeB: DuctNodePos, private val filter: FilterType, private val comparisonValue: Double, private val moreThan: Boolean): S2CCWPacket {



    constructor(buffer: FriendlyByteBuf) : this(
        buffer.readVec3d().toDuctNodePos(buffer.readResourceLocation()),
        buffer.readVec3d().toDuctNodePos(buffer.readResourceLocation()),
        buffer.readEnum(FilterType::class.java),
        buffer.readDouble(),
        buffer.readBoolean()
    )


    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            ScreenOpener.open(SmartDuctScreen(nodeA, nodeB, filter, comparisonValue, moreThan))
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {


        buffer.writeVec3d(nodeA.toMinecraft().toVector3d())
        buffer.writeResourceLocation(nodeA.dimensionId)
        buffer.writeVec3d(nodeB.toMinecraft().toVector3d())
        buffer.writeResourceLocation(nodeB.dimensionId)
        buffer.writeEnum(filter)
        buffer.writeDouble(comparisonValue / 1000)
        buffer.writeBoolean(moreThan)

    }
}

