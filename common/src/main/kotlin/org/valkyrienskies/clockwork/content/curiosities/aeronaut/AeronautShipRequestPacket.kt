package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
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
            val snapshot = inspect(player, shipId) ?: return@enqueueWork
            ClockworkPackets.sendTo(AeronautShipReadoutPacket(shipId, level.dimension().location(), snapshot), player)
        }
        context.setPacketHandled(true)
    }

    companion object {
        private val requests = WeakHashMap<ServerPlayer, Long>()
        fun canInspect(player: ServerPlayer): Boolean = player.isAlive && !player.isSpectator &&
            player.getItemBySlot(EquipmentSlot.HEAD).item is AeronautGogglesItem && AeronautGogglesState.getGogglesAreDown(player)

        fun inspect(player: ServerPlayer, shipId: Long): AeronautShipReadout.Snapshot? {
            if (!canInspect(player)) return null
            val level = player.serverLevel()
            val world = level.shipObjectWorld
            val ship = world.loadedShips.getById(shipId) ?: return null
            if (ship.chunkClaimDimension != level.dimensionId) return null
            val bounds = ship.shipAABB ?: return null
            val modelBox = AABB(bounds.minX().toDouble(), bounds.minY().toDouble(), bounds.minZ().toDouble(),
                bounds.maxX().toDouble(), bounds.maxY().toDouble(), bounds.maxZ().toDouble())
            val eye = player.eyePosition
            val look = player.lookAngle
            val sight = AeronautShipReadout.TerrainSight(level, eye, AeronautShipReadout.SERVER_RAYS)
            val toModel = ship.transform.worldToShip
            val toWorld = ship.transform.shipToWorld
            fun model(point: Vec3): Vec3 = toModel.transformPosition(Vector3d(point.x, point.y, point.z)).let { Vec3(it.x, it.y, it.z) }
            fun exposed(point: Vec3) = sight.sees(toWorld.transformPosition(Vector3d(point.x, point.y, point.z)))
            val localEye = model(eye)
            val nearest = Vec3(localEye.x.coerceIn(modelBox.minX, modelBox.maxX),
                localEye.y.coerceIn(modelBox.minY, modelBox.maxY), localEye.z.coerceIn(modelBox.minZ, modelBox.maxZ))
            if (!exposed(nearest)) {
                val aimed = modelBox.clip(localEye, model(eye.add(look.scale(AeronautForceReadout.MAX_RANGE)))).orElse(null)
                if (aimed == null || !exposed(aimed)) return null
            }
            return AeronautShipReadout.filterVisible(AeronautShipReadout.sample(level, ship), sight, look, shipId) { body, pos ->
                if (body == null) pos
                else world.loadedShips.getById(body)?.takeIf { it.chunkClaimDimension == level.dimensionId }
                    ?.transform?.shipToWorld?.transformPosition(pos, Vector3d())
            }
        }
    }
}
