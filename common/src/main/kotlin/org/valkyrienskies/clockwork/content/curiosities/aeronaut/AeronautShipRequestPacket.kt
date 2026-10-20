package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.dimensionId
import java.util.WeakHashMap

class AeronautShipRequestPacket(private val shipId: Long) : C2SCWPacket {
    constructor(buffer: FriendlyByteBuf) : this(buffer.readLong())
    override fun write(buffer: FriendlyByteBuf) { buffer.writeLong(shipId) }
    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val player = context.sender
            val level = player.serverLevel()
            if (!canInspect(player)) return@enqueueWork
            val now = level.gameTime
            val last = requests[player]
            if (last != null && now - last in 0 until AeronautForceReadout.UPDATE_TICKS) return@enqueueWork
            requests[player] = now
            val ship = level.shipObjectWorld.loadedShips.getById(shipId) ?: return@enqueueWork
            if (ship.chunkClaimDimension != level.dimensionId) return@enqueueWork
            val box = ship.worldAABB
            val p = player.position()
            val dx = p.x - p.x.coerceIn(box.minX(), box.maxX())
            val dy = p.y - p.y.coerceIn(box.minY(), box.maxY())
            val dz = p.z - p.z.coerceIn(box.minZ(), box.maxZ())
            if (dx * dx + dy * dy + dz * dz > AeronautForceReadout.MAX_RANGE * AeronautForceReadout.MAX_RANGE) return@enqueueWork
            ClockworkPackets.sendTo(AeronautShipReadoutPacket(ship.id, level.dimension().location(),
                AeronautShipReadout.sample(level, ship)), player)
        }
        context.setPacketHandled(true)
    }

    companion object {
        private val requests = WeakHashMap<ServerPlayer, Long>()
        fun canInspect(player: ServerPlayer): Boolean = player.isAlive && !player.isSpectator &&
            player.getItemBySlot(EquipmentSlot.HEAD).item is AeronautGogglesItem && AeronautGogglesState.getGogglesAreDown(player)
    }
}
