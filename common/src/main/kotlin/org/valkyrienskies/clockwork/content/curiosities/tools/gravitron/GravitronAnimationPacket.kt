package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import java.util.UUID

class GravitronAnimationPacket internal constructor(
    val owner: UUID,
    val action: GravitronAction,
    val shipId: Long,
    val anchor: Vec3,
    val load: Float,
    val supercharged: Boolean,
    internal val route: List<GravitronGatePath.Reference> = emptyList(), val depth: Int = 0
) : S2CCWPacket {
    override var player: Player? = null

    constructor(buffer: FriendlyByteBuf) : this(
        buffer.readUUID(), buffer.readEnum(GravitronAction::class.java), buffer.readLong(),
        Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readFloat(), buffer.readBoolean(),
        List(buffer.readVarInt().also { require(it in 0..8) }) { GravitronGatePath.Reference(buffer.readUUID(), buffer.readVarInt(), buffer.readLong(),
            buffer.readLong(), Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())) }, buffer.readVarInt()
    )

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeUUID(owner)
        buffer.writeEnum(action)
        buffer.writeLong(shipId)
        buffer.writeDouble(anchor.x)
        buffer.writeDouble(anchor.y)
        buffer.writeDouble(anchor.z)
        buffer.writeFloat(load)
        buffer.writeBoolean(supercharged)
        buffer.writeVarInt(route.size)
        route.forEach {
            buffer.writeUUID(it.owner)
            buffer.writeVarInt(it.side)
            buffer.writeLong(it.entranceTime)
            buffer.writeLong(it.exitTime)
            buffer.writeDouble(it.localAnchor.x)
            buffer.writeDouble(it.localAnchor.y)
            buffer.writeDouble(it.localAnchor.z)
        }
        buffer.writeVarInt(depth)
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork { GravitronEffects.accept(this) }
        context.setPacketHandled(true)
    }

    companion object {
        fun send(player: Player, action: GravitronAction, shipId: Long = -1,
                 anchor: Vector3dc? = null, load: Float = 0f) {
            if (player !is ServerPlayer) return
            val state = GravitronState.getState(player)
            val reference = state.localHoldAnchor
            val route = if (shipId == state.shipID && reference != null) state.holdRoute.map {
                GravitronGatePath.Reference(it, reference)
            } else emptyList()
            ClockworkPackets.sendToClientsTrackingAndSelf(GravitronAnimationPacket(
                player.uuid, action, shipId,
                anchor?.let { Vec3(it.x(), it.y(), it.z()) } ?: Vec3.ZERO,
                load, player.mainHandItem.item is CreativeGravitronItem, route, state.holdDepth
            ), player)
        }
    }
}
