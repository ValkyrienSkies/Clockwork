package org.valkyrienskies.clockwork.content.curiosities

import net.minecraft.core.BlockPos
import org.valkyrienskies.clockwork.content.forces.WanderShipControl
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.ServerShip

interface IWanderliteBlock {
    fun addToShip(ship: LoadedServerShip, pos: BlockPos, force: Double) {
        WanderShipControl.getOrCreate(ship)?.addBlock(pos, force)
    }
    fun removeFromShip(ship: LoadedServerShip, pos: BlockPos) {
        WanderShipControl.getOrCreate(ship)?.removeBlock(pos)
    }
}