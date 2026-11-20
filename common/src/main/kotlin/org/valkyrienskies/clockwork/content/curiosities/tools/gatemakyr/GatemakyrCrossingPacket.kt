package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import java.util.UUID

class GatemakyrCrossingPacket(val dimension: ResourceLocation, val owner: UUID, val side: Int, val placed: Long,
                              val partnerPlaced: Long, val predictedFeet: Vec3) : C2SCWPacket {
    constructor(buf: FriendlyByteBuf) : this(buf.readResourceLocation(), buf.readUUID(), buf.readVarInt(), buf.readVarLong(),
        buf.readVarLong(), Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()))

    override fun write(buf: FriendlyByteBuf) {
        buf.writeResourceLocation(dimension)
        buf.writeUUID(owner)
        buf.writeVarInt(side)
        buf.writeVarLong(placed)
        buf.writeVarLong(partnerPlaced)
        buf.writeDouble(predictedFeet.x)
        buf.writeDouble(predictedFeet.y)
        buf.writeDouble(predictedFeet.z)
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork { GatemakyrServer.cross(context.sender, this) }
        context.setPacketHandled(true)
    }
}
