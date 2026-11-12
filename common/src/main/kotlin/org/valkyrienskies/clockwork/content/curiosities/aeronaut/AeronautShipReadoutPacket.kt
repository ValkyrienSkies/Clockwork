package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.client.render.goggles.AeronautGoggleOverlayRenderer
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class AeronautShipReadoutPacket(val shipId: Long, val dimension: ResourceLocation, val snapshot: AeronautShipReadout.Snapshot) : S2CCWPacket {
    override var player: Player? = null
    constructor(buffer: FriendlyByteBuf) : this(buffer.readLong(), buffer.readResourceLocation(),
        AeronautShipReadout.Snapshot(buffer.readDouble(), readVector(buffer), readCenter(buffer), readCenter(buffer), readCenter(buffer), readCenter(buffer)))

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeLong(shipId)
        buffer.writeResourceLocation(dimension)
        buffer.writeDouble(snapshot.mass)
        writeVector(buffer, snapshot.massCenter)
        for (center in listOf(snapshot.potentialThrust, snapshot.liveThrust, snapshot.potentialLift, snapshot.liveLift)) {
            buffer.writeBoolean(center != null)
            if (center != null) {
                writeVector(buffer, center.position)
                writeVector(buffer, center.force)
                buffer.writeDouble(center.weight)
            }
        }
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork { AeronautGoggleOverlayRenderer.accept(this) }
        context.setPacketHandled(true)
    }

    companion object {
        private fun writeVector(buffer: FriendlyByteBuf, vector: Vector3dc) {
            buffer.writeDouble(vector.x()); buffer.writeDouble(vector.y()); buffer.writeDouble(vector.z())
        }
        private fun readVector(buffer: FriendlyByteBuf) = Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
        private fun readCenter(buffer: FriendlyByteBuf): AeronautForceCenter? =
            if (buffer.readBoolean()) AeronautForceCenter(readVector(buffer), readVector(buffer), buffer.readDouble()) else null
    }
}
