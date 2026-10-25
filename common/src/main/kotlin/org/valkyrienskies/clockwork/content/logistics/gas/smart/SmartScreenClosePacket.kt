package org.valkyrienskies.clockwork.content.logistics.gas.smart

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlockEntity
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.core.util.readVec3d
import org.valkyrienskies.core.util.writeVec3d
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.SmartEdge.FilterType
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3d

class SmartScreenClosePacket(
    private val nodeA: DuctNodePos, private val nodeB: DuctNodePos, private val settings: SmartDuctSettings,
) : C2SCWPacket {
    constructor(buffer: FriendlyByteBuf) : this(
        buffer.readVec3d().toDuctNodePos(buffer.readResourceLocation()),
        buffer.readVec3d().toDuctNodePos(buffer.readResourceLocation()),
        SmartDuctSettings.read(buffer),
    )

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val level = context.sender.level()
            if (nodeA.dimensionId != level.dimension().location() || nodeB.dimensionId != nodeA.dimensionId) return@enqueueWork
            if (!settings.comparisonValue.isFinite() || settings.comparisonValue < 0.0 || settings.filter == FilterType.NONE) return@enqueueWork
            val edges = ClockworkMod.getKelvin(level).edges
            val edge = (edges[Pair(nodeA, nodeB)] ?: edges[Pair(nodeB, nodeA)]) as? ClockworkSmartEdge ?: return@enqueueWork
            edge.filter = settings.filter
            edge.moreThan = settings.moreThan
            edge.comparisonValue = settings.comparisonValue
            edge.gasFilter.clear()
            edge.gasFilter.addAll(settings.gasFilter)
            edge.filterBlacklist = settings.filterBlacklist

            forceUpdate(level, edge.nodeA, edge.nodeB, edge)
            forceUpdate(level, edge.nodeB, edge.nodeA, edge)
        }
        context.setPacketHandled(true)
    }

    private fun forceUpdate(level: Level, nodeA: DuctNodePos, nodeB: DuctNodePos, edge: DuctEdge) {
        val be = level.getBlockEntity(nodeA.toMinecraft()) as? DuctBlockEntity ?: return
        be.edgeData[DuctBlockEntity.EdgePos(nodeA, nodeB)] = edge.serialize(CompoundTag())
        be.notifyUpdate()
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeVec3d(nodeA.toMinecraft().toVector3d())
        buffer.writeResourceLocation(nodeA.dimensionId)
        buffer.writeVec3d(nodeB.toMinecraft().toVector3d())
        buffer.writeResourceLocation(nodeB.dimensionId)
        settings.write(buffer)
    }
}