package org.valkyrienskies.clockwork.util

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.forces.BalloonController
import org.valkyrienskies.clockwork.content.forces.BalloonController.Companion.isValidBalloonEnclosure
import org.valkyrienskies.clockwork.content.forces.data.BalloonData
import org.valkyrienskies.mod.common.getLoadedShipManagingPos

object BlockUpdateCollector {
    fun onSetBlock(sLevel: ServerLevel, pos: BlockPos, state: BlockState) {
        val ship = sLevel.getLoadedShipManagingPos(pos)
        if (ship != null) {
            //val controller = ship.getAttachment(DragController::class.java)?.pushUpdate(pos.toJOML(), state.isAir || state.getCollisionShape(sLevel, pos).isEmpty)
            val controller = BalloonController.getOrCreate(ship)
            val shouldUpdate: ArrayList<Int> = ArrayList()
            val shouldValidate = ArrayList<Int>()
            controller.balloons.forEach { (id, balloon) ->
                val external = balloon.getExternalPositions()
                if (state.isValidBalloonEnclosure(sLevel, pos)) {
                    if (balloon.containsPosition(pos)) {
                        shouldUpdate.add(id)
                        balloon.shouldReScan = true
                        shouldValidate.add(id)
                    }
                    if (external.contains(pos)) balloon.validate(sLevel) // immediate validate to maybe seal leaks
                }
                if (!state.isValidBalloonEnclosure(sLevel, pos) && (balloon.containsPosition(pos) || external.contains(pos))) {
                    shouldValidate.add(id)
                    balloon.shouldReScan = true
                }
            }
            if (shouldUpdate.size > 1) {
                //wait what the fuck
                ClockworkMod.LOGGER.warn("A block at ${pos} somehow induced a split inside multiple balloons simultaneously. Sus...")
            }
            for (id in shouldUpdate) {
                val result = controller.balloons[id]?.trySplit(sLevel) ?: continue
                if (result.first && result.second.isNotEmpty()) {
                    for (newBalloon in result.second) {
                        controller.addBalloon(newBalloon)
                    }
                }
            }
            if (shouldValidate.size > 1) {
                //merging time
                // im not sure how the fuck this list would ever be a value larger than 2...
                // as far as my monkey brain can think it's impossible but we're going to account for it anyways
                val baseMergedBalloon = controller.balloons[shouldValidate[0]]!! // if this is null i kill mysel
                var status = BalloonData.EnclosureStatus.VALID
                for (x in 1 until controller.balloons.size) {
                    if (controller.balloons[x] != null) {
                        status = status.weakestOf(baseMergedBalloon.mergeWith(controller.balloons[x]!!, sLevel))
                        controller.balloons[x]!!.shouldRemove = true // i hope this doesn't cause issues considering it will only update next tick... :clueless:
                    }
                }
                if (status.isAtLeast(BalloonData.EnclosureStatus.UNKNOWN)) {
                    // should be all clear, not sure what to do next
                } else {
                    baseMergedBalloon.shouldRemove = true
                }
            } else {
                for (id in shouldValidate) {
                    val balloon = controller.balloons[id]
                    if (balloon != null) {
                        balloon.shouldValidate = true
                    }
                }
            }
        }
    }
}
