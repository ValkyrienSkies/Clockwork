package org.valkyrienskies.clockwork.content.logistics.gas.redstone

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.ClockworkMod
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

class RedstoneDuctScreenPacket(val pos: BlockPos, val conditional: RedstoneDuctConditional): C2SCWPacket {


    constructor(buffer: FriendlyByteBuf) : this(buffer.readBlockPos(), RedstoneDuctConditional.deserialize(buffer.readNbt()!!))

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {

            (context.sender.level().getBlockEntity(pos) as? RedstoneDuctBlockEntity)?.conditional = conditional
            (context.sender.level().getBlockEntity(pos) as? RedstoneDuctBlockEntity)?.sendData()
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        buffer.writeNbt(conditional.serialize(CompoundTag()))
    }
}