package org.valkyrienskies.clockwork.util

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.content.forces.BalloonController
import org.valkyrienskies.clockwork.content.forces.BalloonController.Companion.isValidBalloonEnclosure
import org.valkyrienskies.mod.common.getLoadedShipManagingPos

object BlockUpdateCollector {
    fun onSetBlock(level: ServerLevel, pos: BlockPos, state: BlockState, previous: BlockState?) {
        if (previous == null) return // LevelChunk did not actually replace a block.
        val ship = level.getLoadedShipManagingPos(pos) ?: return
        if (state.block !== previous.block || state.block is org.valkyrienskies.mod.common.block.WingBlock)
            org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautShipReadout.invalidate(level, ship.id)
        val controller = ship.getAttachment(BalloonController::class.java) ?: return
        if (state.isValidBalloonEnclosure(level, pos) == previous.isValidBalloonEnclosure(level, pos)) return
        controller.onBlockChanged(pos)
    }
}
