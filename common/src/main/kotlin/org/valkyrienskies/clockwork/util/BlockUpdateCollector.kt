package org.valkyrienskies.clockwork.util

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOML

object BlockUpdateCollector {
    fun onSetBlock(sLevel: ServerLevel, pos: BlockPos, state: BlockState) {
        val ship = sLevel.getShipObjectManagingPos(pos)
        if (ship != null) {
            //val controller = ship.getAttachment(DragController::class.java)?.pushUpdate(pos.toJOML(), state.isAir || state.getCollisionShape(sLevel, pos).isEmpty)
        }
    }
}
