package org.valkyrienskies.clockwork.content.logistics.gas.smart

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlockEntity
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.edges.FilteredEdge
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.core.util.readVec3d
import org.valkyrienskies.core.util.writeVec3d
import org.valkyrienskies.kelvin.api.edges.SmartEdge
import org.valkyrienskies.kelvin.api.edges.SmartEdge.FilterType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3d
import kotlin.collections.HashSet

class SmartScreenClosePacket(private val nodeA: DuctNodePos, private val nodeB: DuctNodePos, private val filter: FilterType, private val comparisonValue: Double, private val moreThan: Boolean): C2SCWPacket {


    constructor(buffer: FriendlyByteBuf) : this(
        buffer.readVec3d().toDuctNodePos(buffer.readResourceLocation()),
        buffer.readVec3d().toDuctNodePos(buffer.readResourceLocation()),
        buffer.readEnum(FilterType::class.java),
        buffer.readDouble(),
        buffer.readBoolean()
    )


    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {

            val edge = ClockworkMod.getKelvin().edges[Pair(nodeA, nodeB)] as SmartEdge? ?: return@enqueueWork
            edge.filter = filter
            edge.moreThan = moreThan
            edge.comparisonValue = comparisonValue

            forceUpdate(context.sender.level(), nodeA)
            forceUpdate(context.sender.level(), nodeB)
        }
        context.setPacketHandled(true)
    }

    fun forceUpdate(level: Level, pos: DuctNodePos) {
        val blockPos = pos.toMinecraft()
        val be = level.getBlockEntity(blockPos) as? DuctBlockEntity ?: return
        be.notifyUpdate()
    }

    override fun write(buffer: FriendlyByteBuf) {


        buffer.writeVec3d(nodeA.toMinecraft().toVector3d())
        buffer.writeResourceLocation(nodeA.dimensionId)
        buffer.writeVec3d(nodeB.toMinecraft().toVector3d())
        buffer.writeResourceLocation(nodeB.dimensionId)
        buffer.writeEnum(filter)
        buffer.writeDouble(comparisonValue * 1000)
        buffer.writeBoolean(moreThan)

    }
}