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
            fun exposed(point: Vector3d) = sight.sees(toWorld.transformPosition(point, Vector3d()))
            val localEye = toModel.transformPosition(Vector3d(eye.x, eye.y, eye.z))
            val nearest = AeronautShipReadout.closestPoint(modelBox, localEye)
            if (!exposed(nearest)) {
                val localLook = toModel.transformDirection(Vector3d(look.x, look.y, look.z))
                val end = Vector3d(localEye).fma(AeronautForceReadout.MAX_RANGE, localLook)
                val aimed = modelBox.clip(Vec3(localEye.x, localEye.y, localEye.z), Vec3(end.x, end.y, end.z)).orElse(null)
                if ((aimed == null || !exposed(Vector3d(aimed.x, aimed.y, aimed.z))) &&
                    !exposed(AeronautShipReadout.nearAimPoint(modelBox, localEye, localLook))) return null
            }
            return AeronautShipReadout.filterVisible(AeronautShipReadout.sample(level, ship), sight, look, shipId) { body, pos ->
                if (body == null) pos
                else world.loadedShips.getById(body)?.takeIf { it.chunkClaimDimension == level.dimensionId }
                    ?.transform?.shipToWorld?.transformPosition(pos, Vector3d())
            }
        }
    }
}
