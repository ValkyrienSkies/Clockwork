package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.mod.common.dimensionId

/** Saved with the ship. Only Gravitron freezes acquire this visual; other static ships are untouched. */
class GravitronFrozenShip {
    var x = 0.0
    var y = 0.0
    var z = 0.0
    var frozenAt = 0L

    private fun broadcast(level: ServerLevel, ship: ServerShip, frozen: Boolean) {
        val packet = GravitronFreezePacket(ship.id, Vec3(x, y, z), frozen,
            (level.gameTime - frozenAt).coerceIn(0L, 40L).toFloat())
        if (frozen) {
            val point = ship.shipToWorld.transformPosition(Vector3d(x, y, z))
            ClockworkPackets.sendToNear(level, BlockPos.containing(point.x, point.y, point.z), 192, packet)
        } else {
            // A client may still know this ship after moving away from its original freeze position.
            level.players().forEach { ClockworkPackets.sendTo(packet, it) }
        }
    }

    companion object {
        fun freeze(level: ServerLevel, ship: LoadedServerShip, anchor: Vector3dc) {
            val field = GravitronFrozenShip().also {
                it.x = anchor.x(); it.y = anchor.y(); it.z = anchor.z(); it.frozenAt = level.gameTime
            }
            ship.setAttachment(field)
            field.broadcast(level, ship, true)
        }

        fun thaw(level: ServerLevel, ship: ServerShip) {
            (ship as? LoadedServerShip)?.removeAttachment(GravitronFrozenShip::class.java)?.broadcast(level, ship, false)
        }

        fun tick(level: ServerLevel, ship: LoadedServerShip) {
            if (ship.chunkClaimDimension != level.dimensionId) return
            val field = ship.getAttachment(GravitronFrozenShip::class.java) ?: return
            if (!ship.isStatic) thaw(level, ship)
            // Also restores the field after login, changing dimensions, or entering tracking range.
            else if (level.gameTime % 20L == 0L) field.broadcast(level, ship, true)
        }
    }
}
