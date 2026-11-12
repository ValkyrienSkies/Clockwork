package org.valkyrienskies.clockwork.content.schematics

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronFrozenShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.dimensionId
import java.util.WeakHashMap

class VdexConstruction {
    var cannon = 0L
    var wasStatic = false

    companion object {
        private val pending = WeakHashMap<ServerLevel, MutableMap<Long, VdexConstruction>>()
        fun reserve(level: ServerLevel, id: Long, cannon: BlockPos, wasStatic: Boolean) {
            pending.getOrPut(level) { HashMap() }[id] = VdexConstruction().also { it.cannon = cannon.asLong(); it.wasStatic = wasStatic }
        }

        @JvmStatic
        fun locked(ship: ServerShip?): Boolean = (ship as? LoadedServerShip)?.getAttachment(VdexConstruction::class.java) != null || ship != null && pending.values.any { it.containsKey(ship.id) }

        @JvmStatic
        fun locked(level: Level?, pos: BlockPos): Boolean {
            if (level !is ServerLevel) return false
            val ship = level.getLoadedShipManagingPos(pos)
            if (ship != null) return ship.getAttachment(VdexConstruction::class.java) != null || pending[level]?.containsKey(ship.id) == true
            val reservations = pending[level] ?: return false
            if (reservations.isEmpty()) return false
            return level.getShipManagingPos(pos)?.id in reservations
        }

        fun release(level: ServerLevel, ship: LoadedServerShip) {
            pending[level]?.remove(ship.id)
            val construction = ship.removeAttachment(VdexConstruction::class.java) ?: return
            ship.isStatic = construction.wasStatic
            GravitronFrozenShip.thaw(level, ship)
        }

        fun tick(level: ServerLevel, ship: LoadedServerShip) {
            if (ship.chunkClaimDimension != level.dimensionId) return
            if (ship.getAttachment(VdexConstruction::class.java) == null) pending[level]?.get(ship.id)?.let {
                ship.setAttachment(it)
                GravitronFrozenShip.freeze(level, ship, ship.transform.positionInModel)
            }
            val construction = ship.getAttachment(VdexConstruction::class.java) ?: return
            ship.isStatic = true
            val pos = BlockPos.of(construction.cannon)
            if (level.gameTime % 20L == 0L && level.hasChunkAt(pos)) {
                val build = (level.getBlockEntity(pos) as? VdexCannonAccess)?.vdexBuild
                val ids = build?.saved?.getCompound("Ships")
                if (ids == null || ids.allKeys.none { ids.getLong(it) == ship.id }) release(level, ship)
            }
        }
    }
}
