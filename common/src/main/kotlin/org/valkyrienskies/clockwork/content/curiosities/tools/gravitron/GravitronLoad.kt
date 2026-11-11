package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.util.ConnectedShipMass
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld

object GravitronLoad {
    data class Load(val mass: Double, val rigidMass: Double)
    const val CONNECTED_MASS = "ConnectedMass"
    fun enabled(stack: ItemStack) = stack.tag?.getBoolean(CONNECTED_MASS) == true

    fun mass(level: ServerLevel, ship: ServerShip, stack: ItemStack): Double = load(level, ship, stack).mass

    fun load(level: ServerLevel, ship: ServerShip, stack: ItemStack): Load {
        if (!enabled(stack)) return Load(ship.inertiaData.mass, ship.inertiaData.mass)
        val ground = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]
        val graph = ConnectedShipMass(level.gtpa.getAllJoints().values, groundBody = ground, elastic = { true }) { id ->
            level.shipObjectWorld.loadedShips.getById(id)?.let { ConnectedShipMass.Load(it.inertiaData.mass, it.isStatic && it.id != ship.id) }
        }
        return Load(graph.total(ship.id).mass.coerceAtLeast(ship.inertiaData.mass), graph.rigidMass(ship.id).coerceAtLeast(ship.inertiaData.mass))
    }
}
